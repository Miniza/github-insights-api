package za.vodacom.repoprofile.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.application.mapper.ProfileMapper;
import za.vodacom.repoprofile.config.SourceCodeClientResolver;
import za.vodacom.repoprofile.domain.event.SearchPerformedEvent;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.domain.strategy.LanguageStrategy;
import za.vodacom.repoprofile.ports.in.ProfileUseCase;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;
import za.vodacom.repoprofile.ports.out.SearchHistoryRepositoryPort;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ProfileService implements ProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final SourceCodeClientResolver clientResolver;
    private final SearchHistoryRepositoryPort searchHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LanguageStrategy languageStrategy;
    private final int maxRecords;

    public ProfileService(SourceCodeClientResolver clientResolver,
                          SearchHistoryRepositoryPort searchHistoryRepository,
                          ApplicationEventPublisher eventPublisher,
                          Map<String, LanguageStrategy> strategies,
                          @Value("${language.strategy:byRepoCount}") String strategyName,
                          @Value("${search-history.max-records:50}") int maxRecords) {
        this.clientResolver = clientResolver;
        this.searchHistoryRepository = searchHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.languageStrategy = strategies.get(strategyName);
        if (this.languageStrategy == null) {
            throw new IllegalArgumentException(
                    "Unknown language strategy: '%s'. Available: %s".formatted(strategyName, strategies.keySet()));
        }
        this.maxRecords = maxRecords;
    }

    @Override
    public ProfileResponse getProfile(String username, String provider) {
        log.info("Processing profile request for user: {} via provider: {}", username, provider);

        SourceCodeClient client = clientResolver.resolve(provider);
        User user = client.fetchUser(username);
        List<Repo> repos = client.fetchRepositories(username);

        String topLanguage = languageStrategy.determineTopLanguage(repos);

        List<RepoResponse> sortedRepos = repos.stream()
                .sorted(Comparator.comparingInt(Repo::stargazersCount).reversed())
                .map(ProfileMapper::toRepoResponse)
                .toList();

        // Publish event — persisted asynchronously
        String summary = ProfileMapper.buildSummary(user.login(), user.publicRepos(), topLanguage);
        eventPublisher.publishEvent(new SearchPerformedEvent(user.login(), summary));

        return ProfileMapper.toProfileResponse(user, topLanguage, sortedRepos);
    }

    @Override
    public PagedResponse<RepoResponse> getRepositories(String username, String provider, int page, int perPage) {
        log.info("Processing repos request for user: {} via provider: {} (page={}, perPage={})", username, provider, page, perPage);

        SourceCodeClient client = clientResolver.resolve(provider);
        List<Repo> repos = client.fetchRepositories(username);
        List<RepoResponse> sorted = repos.stream()
                .sorted(Comparator.comparingInt(Repo::stargazersCount).reversed())
                .map(ProfileMapper::toRepoResponse)
                .toList();

        String summary = "%s – %d repos (page %d)".formatted(username, repos.size(), page);
        eventPublisher.publishEvent(new SearchPerformedEvent(username, summary));

        return PagedResponse.of(sorted, page, perPage);
    }

    @Override
    public List<SearchSummary> getSearchHistory() {
        return searchHistoryRepository.findRecentSearches(maxRecords).stream()
                .map(ProfileMapper::toSearchSummary)
                .toList();
    }
}
