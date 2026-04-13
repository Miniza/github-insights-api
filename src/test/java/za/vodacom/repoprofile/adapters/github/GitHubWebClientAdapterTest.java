package za.vodacom.repoprofile.adapters.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import za.vodacom.repoprofile.adapters.github.dto.GitHubRepoResponse;
import za.vodacom.repoprofile.adapters.github.dto.GitHubUserResponse;
import za.vodacom.repoprofile.config.GitHubApiProperties;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.User;
import za.vodacom.repoprofile.exception.NotFoundException;
import za.vodacom.repoprofile.exception.ProviderApiException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubWebClientAdapter Unit Tests")
class GitHubWebClientAdapterTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GitHubWebClientAdapter adapter;

    @BeforeEach
    void setUp() {
        var props = new GitHubApiProperties(
                "https://api.github.com", "test-agent", "",
                java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(10),
                50, 100
        );
        adapter = new GitHubWebClientAdapter(webClient, props, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Fetches and maps user correctly")
    void testFetchUserSuccess() {
        String username = "octocat";
        GitHubUserResponse userDto = new GitHubUserResponse(
                "octocat", "The Octocat", "GitHub's mascot",
                "https://avatars.githubusercontent.com/u/1?", "https://github.com/octocat",
                8, 3938, 9938
        );

        setupUserMocking(userDto);

        User user = adapter.fetchUser(username);

        assertThat(user)
                .isNotNull()
                .extracting(User::login, User::name, User::bio)
                .containsExactly("octocat", "The Octocat", "GitHub's mascot");

        assertThat(user)
                .extracting(User::publicRepos, User::followers, User::following)
                .containsExactly(8, 3938, 9938);
    }

    @Test
    @DisplayName("404 → NotFoundException")
    void testFetchUserNotFound() {
        String username = "nonexistent";
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", HttpHeaders.EMPTY, null, null
        );

        setupUserMockingWithException(exception);

        assertThatThrownBy(() -> adapter.fetchUser(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GitHub user not found");
    }

    @Test
    @DisplayName("5xx → ProviderApiException")
    void testFetchUserProviderError() {
        String username = "testuser";
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY, null, null
        );

        setupUserMockingWithException(exception);

        assertThatThrownBy(() -> adapter.fetchUser(username))
                .isInstanceOf(ProviderApiException.class)
                .hasMessageContaining("GitHub API error");
    }

    @Test
    @DisplayName("Fetches and maps repos")
    void testFetchRepositoriesSuccess() {
        String username = "octocat";
        List<GitHubRepoResponse> reposDtos = List.of(
                new GitHubRepoResponse("Hello-World", "Hello World!", "https://github.com/octocat/Hello-World",
                        "Java", 80, 9, 180L),
                new GitHubRepoResponse("C-Sharp-Sample", null, "https://github.com/octocat/C-Sharp-Sample",
                        "C#", 0, 0, 1L)
        );

        setupReposMocking(reposDtos, null);

        List<Repo> repos = adapter.fetchRepositories(username);

        assertThat(repos)
                .hasSize(2)
                .extracting(Repo::name, Repo::language)
                .containsExactly(
                        tuple("Hello-World", "Java"),
                        tuple("C-Sharp-Sample", "C#")
                );
    }

    @Test
    @DisplayName("fetchRepositories 404 → NotFoundException")
    void testFetchRepositoriesNotFound() {
        String username = "nonexistent";
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", HttpHeaders.EMPTY, null, null
        );

        setupReposMockingWithException(exception);

        assertThatThrownBy(() -> adapter.fetchRepositories(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GitHub user not found");
    }

    @Test
    @DisplayName("Empty repo list")
    void testFetchRepositoriesEmpty() {
        setupReposMocking(List.of(), null);

        List<Repo> repos = adapter.fetchRepositories("emptyuser");

        assertThat(repos).isEmpty();
    }

    @Test
    @DisplayName("All repo fields mapped correctly")
    void testRepositoryFieldMapping() {
        GitHubRepoResponse repoDto = new GitHubRepoResponse(
                "test-repo",
                "Test Description",
                "https://github.com/testuser/test-repo",
                "TypeScript",
                42,
                7,
                2500L
        );

        setupReposMocking(List.of(repoDto), null);

        List<Repo> repos = adapter.fetchRepositories("testuser");

        assertThat(repos)
                .hasSize(1)
                .extracting(Repo::name, Repo::description, Repo::htmlUrl, Repo::language,
                        Repo::stargazersCount, Repo::forksCount, Repo::size)
                .containsExactly(
                        tuple("test-repo", "Test Description", "https://github.com/testuser/test-repo",
                                "TypeScript", 42, 7, 2500L)
                );
    }

    @Test
    @DisplayName("Null description preserved")
    void testRepositoryNullDescription() {
        GitHubRepoResponse repoDto = new GitHubRepoResponse(
                "no-desc-repo",
                null,
                "https://github.com/testuser/no-desc-repo",
                "Rust",
                100,
                10,
                5000L
        );

        setupReposMocking(List.of(repoDto), null);

        List<Repo> repos = adapter.fetchRepositories("testuser");

        assertThat(repos)
                .hasSize(1)
                .extracting(Repo::description)
                .containsExactly((String) null);
    }

    // ---- WebClient mock helpers ----

    private void setupUserMocking(GitHubUserResponse userDto) {
        ResponseEntity<GitHubUserResponse> responseEntity = ResponseEntity.ok(userDto);
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.just(responseEntity)).when(responseSpec).toEntity(GitHubUserResponse.class);
    }

    private void setupUserMockingWithException(WebClientResponseException exception) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.error(exception)).when(responseSpec).toEntity(GitHubUserResponse.class);
    }

    private void setupReposMocking(List<GitHubRepoResponse> reposDtos, String nextLink) {
        HttpHeaders headers = new HttpHeaders();
        if (nextLink != null) {
            headers.put("Link", List.of(nextLink));
        }

        ResponseEntity<List<GitHubRepoResponse>> responseEntity = ResponseEntity.ok()
                .headers(headers)
                .body(reposDtos);

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.just(responseEntity)).when(responseSpec).toEntity(any(ParameterizedTypeReference.class));
    }

    private void setupReposMockingWithException(WebClientResponseException exception) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.error(exception)).when(responseSpec).toEntity(any(ParameterizedTypeReference.class));
    }
}
