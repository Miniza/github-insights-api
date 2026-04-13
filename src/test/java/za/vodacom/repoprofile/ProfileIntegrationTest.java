package za.vodacom.repoprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import za.vodacom.repoprofile.adapters.persistence.SpringDataSearchRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration Tests")
class ProfileIntegrationTest {

    private static MockWebServer mockGitHub;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataSearchRepository searchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockGitHub = new MockWebServer();
        mockGitHub.start();
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockGitHub.shutdown();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("github.api.base-url", () -> mockGitHub.url("/").toString().replaceAll("/$", ""));
    }

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // If we get here, Spring context started, Flyway ran, all beans wired
    }

    @Test
    @DisplayName("GET /profiles/{username} returns profile with top language via GitHub API")
    void testFullProfileLifecycle() throws Exception {
        // Arrange — mock GitHub user response
        Map<String, Object> userResponse = Map.of(
                "login", "testuser",
                "name", "Test User",
                "bio", "A test bio",
                "avatar_url", "https://example.com/avatar.png",
                "html_url", "https://github.com/testuser",
                "public_repos", 3,
                "followers", 100,
                "following", 50
        );

        // Arrange — mock GitHub repos response
        List<Map<String, Object>> reposResponse = List.of(
                Map.of("name", "alpha", "description", "Alpha project", "html_url", "https://github.com/testuser/alpha",
                        "language", "Java", "stargazers_count", 50, "forks_count", 10, "size", 2000),
                Map.of("name", "beta", "description", "Beta project", "html_url", "https://github.com/testuser/beta",
                        "language", "Java", "stargazers_count", 30, "forks_count", 5, "size", 1000),
                Map.of("name", "gamma", "description", "Gamma project", "html_url", "https://github.com/testuser/gamma",
                        "language", "Python", "stargazers_count", 10, "forks_count", 2, "size", 500)
        );

        enqueueJson(userResponse);
        enqueueJson(reposResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.publicRepos").value(3))
                .andExpect(jsonPath("$.topLanguage").value("Java"))
                .andExpect(jsonPath("$.repositories").isArray())
                .andExpect(jsonPath("$.repositories.length()").value(3))
                .andExpect(jsonPath("$.repositories[0].name").value("alpha"))
                .andExpect(jsonPath("$.repositories[0].stargazersCount").value(50));

        // Verify search history was persisted asynchronously
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(searchRepository.findAllByOrderBySearchedAtDesc())
                        .anyMatch(e -> e.getUsername().equals("testuser")
                                && e.getSummary().contains("Java"))
        );
    }

    @Test
    @DisplayName("GET /profiles/{username}/repos returns paginated repos from GitHub API")
    void testPaginatedRepos() throws Exception {
        // Arrange — mock user + repos
        Map<String, Object> userResponse = Map.of(
                "login", "pageuser",
                "name", "Page User",
                "bio", "",
                "avatar_url", "https://example.com/avatar.png",
                "html_url", "https://github.com/pageuser",
                "public_repos", 2,
                "followers", 10,
                "following", 5
        );

        List<Map<String, Object>> reposResponse = List.of(
                Map.of("name", "repo1", "description", "First", "html_url", "https://github.com/pageuser/repo1",
                        "language", "Go", "stargazers_count", 20, "forks_count", 3, "size", 800)
        );

        enqueueJson(userResponse);
        enqueueJson(reposResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/pageuser/repos")
                        .param("page", "1")
                        .param("perPage", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.perPage").value(5))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("repo1"));
    }

    @Test
    @DisplayName("GET /profiles/{username} returns 404 for non-existent user")
    void testUserNotFound() throws Exception {
        // Arrange — GitHub returns 404
        mockGitHub.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\":\"Not Found\"}"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("GitHub user not found: nonexistent"));
    }

    @Test
    @DisplayName("GET /profiles/{username} returns 400 for invalid username format")
    void testInvalidUsername() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/invalid--user!"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /searches returns search history from H2")
    void testSearchHistoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/searches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Actuator health endpoint is accessible")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private void enqueueJson(Object body) throws Exception {
        mockGitHub.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(body)));
    }
}
