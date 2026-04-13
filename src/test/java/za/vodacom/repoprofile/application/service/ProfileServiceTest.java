package za.vodacom.repoprofile.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.domain.event.SearchPerformedEvent;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.SearchRecord;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.domain.strategy.LanguageStrategy;
import za.vodacom.repoprofile.ports.out.ClientResolver;
import za.vodacom.repoprofile.ports.out.SourceCodeClient;
import za.vodacom.repoprofile.ports.out.SearchHistoryRepositoryPort;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Unit Tests")
class ProfileServiceTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private SearchHistoryRepositoryPort searchHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SourceCodeClient sourceCodeClient;

    @Mock
    private LanguageStrategy languageStrategy;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        Map<String, LanguageStrategy> strategies = new HashMap<>();
        strategies.put("byRepoCount", languageStrategy);

        profileService = new ProfileService(
                clientResolver,
                searchHistoryRepository,
                eventPublisher,
                strategies,
                "byRepoCount",
                50
        );
    }

    @Test
    @DisplayName("Fetches user profile with top language")
    void testGetProfileSuccess() {
        String username = "octocat";
        String provider = "github";
        User user = new User("octocat", "The Octocat", "GitHub's mascot", "https://avatars.githubusercontent.com/u/1?", 
                "https://github.com/octocat", 8, 3938, 9938);
        List<Repo> repos = List.of(
                new Repo("Hello-World", "Hello World!", "https://github.com/octocat/Hello-World", 
                        "Java", 80, 9, 180),
                new Repo("C-Sharp-Sample", null, "https://github.com/octocat/C-Sharp-Sample", 
                        "C#", 0, 0, 1)
        );

        when(clientResolver.resolve(provider)).thenReturn(sourceCodeClient);
        when(sourceCodeClient.fetchUser(username)).thenReturn(user);
        when(sourceCodeClient.fetchRepositories(username)).thenReturn(repos);
        when(languageStrategy.determineTopLanguage(repos)).thenReturn("Java");

        ProfileResponse response = profileService.getProfile(username, provider);

        assertThat(response)
                .isNotNull()
                .extracting(ProfileResponse::login, ProfileResponse::name, ProfileResponse::topLanguage)
                .containsExactly("octocat", "The Octocat", "Java");

        assertThat(response.repositories())
                .hasSize(2)
                .extracting(RepoResponse::name)
                .containsExactly("Hello-World", "C-Sharp-Sample");

        assertThat(response.repositories())
                .isSortedAccordingTo((r1, r2) -> Integer.compare(r2.stargazersCount(), r1.stargazersCount()));

        verify(eventPublisher).publishEvent(any(SearchPerformedEvent.class));
    }

    @Test
    @DisplayName("Repos sorted by stars descending")
    void testRepositoriesSortedByStars() {
        String username = "testuser";
        String provider = "gitlab";
        User user = new User("testuser", "Test User", "Test", "https://example.com/avatar", 
                "https://example.com/profile", 5, 10, 20);
        List<Repo> repos = List.of(
                new Repo("repo1", "Small Project", "https://example.com/repo1", "Python", 10, 1, 100),
                new Repo("repo2", "Popular Project", "https://example.com/repo2", "Python", 150, 50, 5000),
                new Repo("repo3", "Medium Project", "https://example.com/repo3", "Python", 50, 10, 1000)
        );

        when(clientResolver.resolve(provider)).thenReturn(sourceCodeClient);
        when(sourceCodeClient.fetchUser(username)).thenReturn(user);
        when(sourceCodeClient.fetchRepositories(username)).thenReturn(repos);
        when(languageStrategy.determineTopLanguage(repos)).thenReturn("Python");

        ProfileResponse response = profileService.getProfile(username, provider);

        assertThat(response.repositories())
                .extracting(RepoResponse::stargazersCount)
                .containsExactly(150, 50, 10);
    }

    @Test
    @DisplayName("Paginated repo fetch returns correct page metadata")
    void testGetRepositoriesWithPagination() {
        String username = "testuser";
        String provider = "github";
        int page = 1;
        int perPage = 10;

        User user = new User("testuser", "Test User", "Bio", "avatar", "url", 25, 10, 5);
        List<Repo> pageRepos = createTestRepositories(10);
        when(clientResolver.resolve(provider)).thenReturn(sourceCodeClient);
        when(sourceCodeClient.fetchUser(username)).thenReturn(user);
        when(sourceCodeClient.fetchRepositories(username, page, perPage)).thenReturn(pageRepos);

        PagedResponse<RepoResponse> response = profileService.getRepositories(username, provider, page, perPage);

        assertThat(response)
                .isNotNull()
                .extracting(PagedResponse::page, PagedResponse::totalItems, PagedResponse::totalPages)
                .containsExactly(1, 25, 3);

        assertThat(response.content())
                .hasSize(perPage)
                .allSatisfy(repo -> assertThat(repo).isNotNull());

        verify(sourceCodeClient).fetchRepositories(username, page, perPage);
        verify(eventPublisher).publishEvent(any(SearchPerformedEvent.class));
    }

    @Test
    @DisplayName("Empty repo list")
    void testGetRepositoriesEmpty() {
        String username = "emptyuser";
        String provider = "github";
        int page = 1;
        int perPage = 10;

        User user = new User("emptyuser", "Empty User", "Bio", "avatar", "url", 0, 0, 0);
        when(clientResolver.resolve(provider)).thenReturn(sourceCodeClient);
        when(sourceCodeClient.fetchUser(username)).thenReturn(user);
        when(sourceCodeClient.fetchRepositories(username, page, perPage)).thenReturn(List.of());

        PagedResponse<RepoResponse> response = profileService.getRepositories(username, provider, page, perPage);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalItems()).isZero();
        verify(eventPublisher).publishEvent(any(SearchPerformedEvent.class));
    }

    @Test
    @DisplayName("Search history retrieval")
    void testGetSearchHistory() {
        SearchRecord record1 = new SearchRecord(1L, "octocat", "octocat – 8 repos", Instant.now().minusSeconds(86400));
        SearchRecord record2 = new SearchRecord(2L, "torvalds", "torvalds – 10 repos", Instant.now());

        when(searchHistoryRepository.findRecentSearches(50)).thenReturn(List.of(record1, record2));

        List<SearchSummary> history = profileService.getSearchHistory();

        assertThat(history)
                .hasSize(2)
                .allSatisfy(summary -> assertThat(summary).isNotNull());

        verify(searchHistoryRepository).findRecentSearches(50);
    }

    @Test
    @DisplayName("Empty search history")
    void testGetSearchHistoryEmpty() {
        when(searchHistoryRepository.findRecentSearches(50)).thenReturn(List.of());

        List<SearchSummary> history = profileService.getSearchHistory();

        assertThat(history).isEmpty();
        verify(searchHistoryRepository).findRecentSearches(50);
    }

    @Test
    @DisplayName("Resolves correct provider client")
    void testClientResolverIntegration() {
        String username = "testuser";
        User user = new User("testuser", "Test", "Bio", "avatar", "url", 5, 10, 20);

        when(clientResolver.resolve("github")).thenReturn(sourceCodeClient);
        when(sourceCodeClient.fetchUser(username)).thenReturn(user);
        when(sourceCodeClient.fetchRepositories(username)).thenReturn(List.of());
        when(languageStrategy.determineTopLanguage(any())).thenReturn("Java");

        profileService.getProfile(username, "github");

        verify(clientResolver).resolve("github");
        verify(sourceCodeClient).fetchUser(username);
        verify(sourceCodeClient).fetchRepositories(username);
    }

    @Test
    @DisplayName("Unknown language strategy throws on construction")
    void testUnknownStrategyThrowsException() {
        Map<String, LanguageStrategy> strategies = new HashMap<>();
        strategies.put("byRepoCount", languageStrategy);
        assertThatThrownBy(() -> new ProfileService(
                clientResolver,
                searchHistoryRepository,
                eventPublisher,
                strategies,
                "unknownStrategy",
                50
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown language strategy");
    }

    private List<Repo> createTestRepositories(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new Repo(
                        "repo" + i,
                        "Description " + i,
                        "https://example.com/repo" + i,
                        "Java",
                        100 - i,
                        10 - (i % 10),
                        1000 + i
                ))
                .toList();
    }
}
