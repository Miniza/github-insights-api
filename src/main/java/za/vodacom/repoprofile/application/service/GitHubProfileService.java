package za.vodacom.repoprofile.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.vodacom.repoprofile.application.dto.GitHubProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.application.mapper.GitHubMapper;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.domain.strategy.LanguageStrategy;
import za.vodacom.repoprofile.ports.in.GitHubUseCase;
import za.vodacom.repoprofile.ports.out.GitHubClient;
import za.vodacom.repoprofile.ports.out.SearchHistoryRepositoryPort;

import java.util.Comparator;
import java.util.List;

@Service
public class GitHubProfileService implements GitHubUseCase {

    private static final Logger log = LoggerFactory.getLogger(GitHubProfileService.class);

    private final GitHubClient gitHubClient;
    private final SearchHistoryRepositoryPort searchHistoryRepository;
    private final LanguageStrategy languageStrategy;
    private final int maxRecords;

    public GitHubProfileService(GitHubClient gitHubClient,
                                SearchHistoryRepositoryPort searchHistoryRepository,
                                @Qualifier("byRepoCount") LanguageStrategy languageStrategy,
                                @Value("${search-history.max-records:50}") int maxRecords) {
        this.gitHubClient = gitHubClient;
        this.searchHistoryRepository = searchHistoryRepository;
        this.languageStrategy = languageStrategy;
        this.maxRecords = maxRecords;
    }

    @Override
    public GitHubProfileResponse getProfile(String username) {
        log.info("Processing profile request for user: {}", username);

        User user = gitHubClient.fetchUser(username);
        List<Repo> repos = gitHubClient.fetchRepositories(username);

        String topLanguage = languageStrategy.determineTopLanguage(repos);

        List<RepoResponse> sortedRepos = repos.stream()
                .sorted(Comparator.comparingInt(Repo::stargazersCount).reversed())
                .map(GitHubMapper::toRepoResponse)
                .toList();

        // Persist search record
        String summary = GitHubMapper.buildSummary(user.login(), user.publicRepos(), topLanguage);
        searchHistoryRepository.save(user.login(), summary);
        searchHistoryRepository.pruneOldest(maxRecords);

        return GitHubMapper.toProfileResponse(user, topLanguage, sortedRepos);
    }

    @Override
    public List<RepoResponse> getRepositories(String username) {
        log.info("Processing repos request for user: {}", username);

        List<Repo> repos = gitHubClient.fetchRepositories(username);
        return repos.stream()
                .sorted(Comparator.comparingInt(Repo::stargazersCount).reversed())
                .map(GitHubMapper::toRepoResponse)
                .toList();
    }

    @Override
    public List<SearchSummary> getSearchHistory() {
        return searchHistoryRepository.findRecentSearches(maxRecords).stream()
                .map(GitHubMapper::toSearchSummary)
                .toList();
    }
}
