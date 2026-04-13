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
import reactor.core.publisher.Mono;
import za.vodacom.repoprofile.adapters.github.dto.GitHubRepoResponse;
import za.vodacom.repoprofile.adapters.github.dto.GitHubUserResponse;
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
        adapter = new GitHubWebClientAdapter(webClient, 50, 100);
    }

    @Test
    @DisplayName("Should fetch user successfully")
    void testFetchUserSuccess() {
        // Arrange
        String username = "octocat";
        GitHubUserResponse userDto = new GitHubUserResponse(
                "octocat", "The Octocat", "GitHub's mascot",
                "https://avatars.githubusercontent.com/u/1?", "https://github.com/octocat",
                8, 3938, 9938
        );

        setupUserMocking(userDto);

        // Act
        User user = adapter.fetchUser(username);

        // Assert
        assertThat(user)
                .isNotNull()
                .extracting(User::login, User::name, User::bio)
                .containsExactly("octocat", "The Octocat", "GitHub's mascot");

        assertThat(user)
                .extracting(User::publicRepos, User::followers, User::following)
                .containsExactly(8, 3938, 9938);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void testFetchUserNotFound() {
        // Arrange
        String username = "nonexistent";
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", HttpHeaders.EMPTY, null, null
        );

        setupUserMockingWithException(exception);

        // Act & Assert
        assertThatThrownBy(() -> adapter.fetchUser(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GitHub user not found");
    }

    @Test
    @DisplayName("Should throw ProviderApiException on API error")
    void testFetchUserProviderError() {
        // Arrange
        String username = "testuser";
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY, null, null
        );

        setupUserMockingWithException(exception);

        // Act & Assert
        assertThatThrownBy(() -> adapter.fetchUser(username))
                .isInstanceOf(ProviderApiException.class)
                .hasMessageContaining("GitHub API error");
    }

    @Test
    @DisplayName("Should fetch repositories successfully")
    void testFetchRepositoriesSuccess() {
        // Arrange
        String username = "octocat";
        List<GitHubRepoResponse> reposDtos = List.of(
                new GitHubRepoResponse("Hello-World", "Hello World!", "https://github.com/octocat/Hello-World",
                        "Java", 80, 9, 180L),
                new GitHubRepoResponse("C-Sharp-Sample", null, "https://github.com/octocat/C-Sharp-Sample",
                        "C#", 0, 0, 1L)
        );

        setupReposMocking(reposDtos, null);

        // Act
        List<Repo> repos = adapter.fetchRepositories(username);

        // Assert
        assertThat(repos)
                .hasSize(2)
                .extracting(Repo::name, Repo::language)
                .containsExactly(
                        tuple("Hello-World", "Java"),
                        tuple("C-Sharp-Sample", "C#")
                );
    }

    @Test
    @DisplayName("Should throw NotFoundException when repositories user not found")
    void testFetchRepositoriesNotFound() {
        // Arrange
        String username = "nonexistent";
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", HttpHeaders.EMPTY, null, null
        );

        setupReposMockingWithException(exception);

        // Act & Assert
        assertThatThrownBy(() -> adapter.fetchRepositories(username))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("GitHub user not found");
    }

    @Test
    @DisplayName("Should handle empty repository list")
    void testFetchRepositoriesEmpty() {
        // Arrange
        setupReposMocking(List.of(), null);

        // Act
        List<Repo> repos = adapter.fetchRepositories("emptyuser");

        // Assert
        assertThat(repos).isEmpty();
    }

    @Test
    @DisplayName("Should map repository fields correctly")
    void testRepositoryFieldMapping() {
        // Arrange
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

        // Act
        List<Repo> repos = adapter.fetchRepositories("testuser");

        // Assert
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
    @DisplayName("Should handle null description in repository")
    void testRepositoryNullDescription() {
        // Arrange
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

        // Act
        List<Repo> repos = adapter.fetchRepositories("testuser");

        // Assert
        assertThat(repos)
                .hasSize(1)
                .extracting(Repo::description)
                .containsExactly((String) null);
    }

    // ---- Helper methods for WebClient mocking (using doReturn to avoid generic issues) ----

    private void setupUserMocking(GitHubUserResponse userDto) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.just(userDto)).when(responseSpec).bodyToMono(GitHubUserResponse.class);
    }

    private void setupUserMockingWithException(WebClientResponseException exception) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.error(exception)).when(responseSpec).bodyToMono(GitHubUserResponse.class);
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
