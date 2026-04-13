package za.vodacom.repoprofile.adapters.github;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import za.vodacom.repoprofile.adapters.github.dto.GitHubRepoResponse;
import za.vodacom.repoprofile.adapters.github.dto.GitHubUserResponse;
import za.vodacom.repoprofile.config.GitHubApiProperties;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.exception.ProviderApiException;
import za.vodacom.repoprofile.exception.NotFoundException;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;
import za.vodacom.repoprofile.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("github")
public class GitHubWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebClientAdapter.class);
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;
    private final int maxPages;
    private final int perPage;

    private final Timer userFetchTimer;
    private final Timer repoFetchTimer;
    private final Counter apiErrorCounter;
    private final AtomicInteger rateLimitRemaining = new AtomicInteger(-1);

    public GitHubWebClientAdapter(WebClient gitHubWebClient, GitHubApiProperties props, MeterRegistry registry) {
        this.webClient = gitHubWebClient;
        this.maxPages = props.maxPages();
        this.perPage = props.perPage();

        this.userFetchTimer = Timer.builder("github.api.fetch.user")
                .description("Time to fetch a user profile from GitHub")
                .register(registry);
        this.repoFetchTimer = Timer.builder("github.api.fetch.repos")
                .description("Time to fetch repositories from GitHub")
                .register(registry);
        this.apiErrorCounter = Counter.builder("github.api.errors")
                .description("Number of GitHub API errors")
                .register(registry);

        registry.gauge("github.api.rate_limit.remaining", rateLimitRemaining);
    }

    @Override
    @Cacheable(value = Constants.CACHE_PROFILES, key = "'github:' + #username")
    @CircuitBreaker(name = "github", fallbackMethod = "fetchUserFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public User fetchUser(String username) {
        log.info("Fetching GitHub profile for user: {}", username);
        return userFetchTimer.record(() -> {
            try {
                ResponseEntity<GitHubUserResponse> response = webClient.get()
                        .uri("/users/{username}", username)
                        .retrieve()
                        .toEntity(GitHubUserResponse.class)
                        .block();

                if (response == null || response.getBody() == null) {
                    throw new NotFoundException("GitHub user not found: " + username);
                }

                updateRateLimitGauge(response);
                GitHubUserResponse dto = response.getBody();

                return new User(
                        dto.login(), dto.name(), dto.bio(), dto.avatarUrl(),
                        dto.htmlUrl(), dto.publicRepos(), dto.followers(), dto.following()
                );
            } catch (WebClientResponseException.NotFound e) {
                throw new NotFoundException("GitHub user not found: " + username);
            } catch (WebClientResponseException e) {
                apiErrorCounter.increment();
                throw new ProviderApiException("GitHub API error: " + e.getStatusCode(), e);
            }
        });
    }

    @Override
    @Cacheable(value = Constants.CACHE_REPOS, key = "'github:' + #username")
    @CircuitBreaker(name = "github", fallbackMethod = "fetchRepositoriesFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching GitHub repositories for user: {}", username);
        return repoFetchTimer.record(() -> {
            try {
                List<Repo> allRepos = new ArrayList<>();
                String uri = "/users/{username}/repos?per_page=" + perPage + "&sort=stars&direction=desc";
                int page = 0;

                while (uri != null && page < maxPages) {
                    ResponseEntity<List<GitHubRepoResponse>> response = webClient.get()
                            .uri(uri, username)
                            .retrieve()
                            .toEntity(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {})
                            .block();

                    if (response == null || response.getBody() == null || response.getBody().isEmpty()) {
                        break;
                    }

                    updateRateLimitGauge(response);

                    response.getBody().stream()
                            .map(dto -> new Repo(
                                    dto.name(), dto.description(), dto.htmlUrl(), dto.language(),
                                    dto.stargazersCount(), dto.forksCount(), dto.size()
                            ))
                            .forEach(allRepos::add);

                    uri = parseNextLink(response.getHeaders().getFirst("Link"));
                    page++;
                }

                return allRepos;
            } catch (WebClientResponseException.NotFound e) {
                throw new NotFoundException("GitHub user not found: " + username);
            } catch (WebClientResponseException e) {
                apiErrorCounter.increment();
                throw new ProviderApiException("GitHub API error: " + e.getStatusCode(), e);
            }
        });
    }

    private String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void updateRateLimitGauge(ResponseEntity<?> response) {
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
        if (remaining != null) {
            try {
                int value = Integer.parseInt(remaining);
                rateLimitRemaining.set(value);
                if (value < 10) {
                    log.warn("GitHub API rate limit low: {} calls remaining", value);
                }
            } catch (NumberFormatException ignored) {
                // non-numeric header value
            }
        }
    }

    @Override
    @CircuitBreaker(name = "github", fallbackMethod = "fetchRepositoriesPageFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public List<Repo> fetchRepositories(String username, int page, int perPage) {
        log.info("Fetching GitHub repositories for user: {} (page={}, perPage={})", username, page, perPage);
        try {
            ResponseEntity<List<GitHubRepoResponse>> response = webClient.get()
                    .uri("/users/{username}/repos?sort=stars&direction=desc&page={page}&per_page={perPage}",
                            username, page, perPage)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {})
                    .block();

            if (response == null || response.getBody() == null) {
                return List.of();
            }

            updateRateLimitGauge(response);

            return response.getBody().stream()
                    .map(dto -> new Repo(
                            dto.name(), dto.description(), dto.htmlUrl(), dto.language(),
                            dto.stargazersCount(), dto.forksCount(), dto.size()
                    ))
                    .toList();
        } catch (WebClientResponseException.NotFound e) {
            throw new NotFoundException("GitHub user not found: " + username);
        } catch (WebClientResponseException e) {
            apiErrorCounter.increment();
            throw new ProviderApiException("GitHub API error: " + e.getStatusCode(), e);
        }
    }

    @SuppressWarnings("unused")
    private User fetchUserFallback(String username, Throwable t) {
        if (t instanceof NotFoundException) {
            throw (NotFoundException) t;
        }
        log.error("Circuit breaker open for profile fetch: {}", t.getMessage());
        throw new ProviderApiException("GitHub service is temporarily unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private List<Repo> fetchRepositoriesFallback(String username, Throwable t) {
        if (t instanceof NotFoundException) {
            throw (NotFoundException) t;
        }
        log.error("Circuit breaker open for repos fetch: {}", t.getMessage());
        throw new ProviderApiException("GitHub service is temporarily unavailable. Please try again later.", t);
    }

    @SuppressWarnings("unused")
    private List<Repo> fetchRepositoriesPageFallback(String username, int page, int perPage, Throwable t) {
        if (t instanceof NotFoundException) {
            throw (NotFoundException) t;
        }
        log.error("Circuit breaker open for paginated repos fetch: {}", t.getMessage());
        throw new ProviderApiException("GitHub service is temporarily unavailable. Please try again later.", t);
    }
}
