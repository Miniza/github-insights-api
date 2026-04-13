package za.vodacom.repoprofile.adapters.github;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("github")
public class GitHubWebClientAdapter implements SourceCodeClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebClientAdapter.class);
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;
    private final int maxPages;
    private final int perPage;

    public GitHubWebClientAdapter(WebClient gitHubWebClient, GitHubApiProperties props) {
        this.webClient = gitHubWebClient;
        this.maxPages = props.maxPages();
        this.perPage = props.perPage();
    }

    @Override
    @Cacheable(value = Constants.CACHE_PROFILES, key = "'github:' + #username")
    @CircuitBreaker(name = "github", fallbackMethod = "fetchUserFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public User fetchUser(String username) {
        log.info("Fetching GitHub profile for user: {}", username);
        try {
            GitHubUserResponse dto = webClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .bodyToMono(GitHubUserResponse.class)
                    .block();

            if (dto == null) {
                throw new NotFoundException("GitHub user not found: " + username);
            }

            return new User(
                    dto.login(), dto.name(), dto.bio(), dto.avatarUrl(),
                    dto.htmlUrl(), dto.publicRepos(), dto.followers(), dto.following()
            );
        } catch (WebClientResponseException.NotFound e) {
            throw new NotFoundException("GitHub user not found: " + username);
        } catch (WebClientResponseException e) {
            throw new ProviderApiException("GitHub API error: " + e.getStatusCode(), e);
        }
    }

    @Override
    @Cacheable(value = Constants.CACHE_REPOS, key = "'github:' + #username")
    @CircuitBreaker(name = "github", fallbackMethod = "fetchRepositoriesFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public List<Repo> fetchRepositories(String username) {
        log.info("Fetching GitHub repositories for user: {}", username);
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
            throw new ProviderApiException("GitHub API error: " + e.getStatusCode(), e);
        }
    }

    private String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Override
    @CircuitBreaker(name = "github", fallbackMethod = "fetchRepositoriesPageFallback")
    @RateLimiter(name = "github")
    @Retry(name = "github")
    public List<Repo> fetchRepositories(String username, int page, int perPage) {
        log.info("Fetching GitHub repositories for user: {} (page={}, perPage={})", username, page, perPage);
        try {
            List<GitHubRepoResponse> body = webClient.get()
                    .uri("/users/{username}/repos?sort=stars&direction=desc&page={page}&per_page={perPage}",
                            username, page, perPage)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {})
                    .block();

            if (body == null) {
                return List.of();
            }

            return body.stream()
                    .map(dto -> new Repo(
                            dto.name(), dto.description(), dto.htmlUrl(), dto.language(),
                            dto.stargazersCount(), dto.forksCount(), dto.size()
                    ))
                    .toList();
        } catch (WebClientResponseException.NotFound e) {
            throw new NotFoundException("GitHub user not found: " + username);
        } catch (WebClientResponseException e) {
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
