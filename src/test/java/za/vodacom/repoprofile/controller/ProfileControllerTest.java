package za.vodacom.repoprofile.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.vodacom.repoprofile.application.dto.PagedResponse;
import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.exception.NotFoundException;
import za.vodacom.repoprofile.exception.ProviderApiException;
import za.vodacom.repoprofile.ports.in.ProfileUseCase;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@DisplayName("ProfileController Unit Tests")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileUseCase profileUseCase;

    @Test
    @DisplayName("Should retrieve user profile successfully")
    void testGetProfileSuccess() throws Exception {
        // Arrange
        String username = "octocat";
        String provider = "github";

        ProfileResponse profile = new ProfileResponse(
                "octocat",
                "The Octocat",
                "GitHub's mascot",
                "https://avatars.githubusercontent.com/u/1?",
                "https://github.com/octocat",
                8,
                3938,
                9938,
                "Java",
                List.of(
                        new RepoResponse("Hello-World", "Hello World!", "https://github.com/octocat/Hello-World", "Java", 80, 9, 180L),
                        new RepoResponse("C-Sharp-Sample", null, "https://github.com/octocat/C-Sharp-Sample", "C#", 0, 0, 1L)
                )
        );

        when(profileUseCase.getProfile(username, provider)).thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .param("provider", provider)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.login", is("octocat")))
                .andExpect(jsonPath("$.name", is("The Octocat")))
                .andExpect(jsonPath("$.topLanguage", is("Java")))
                .andExpect(jsonPath("$.repositories", hasSize(2)))
                .andExpect(jsonPath("$.repositories[0].name", is("Hello-World")))
                .andExpect(jsonPath("$.repositories[0].stargazersCount", is(80)));
    }

    @Test
    @DisplayName("Should use default provider when not specified")
    void testGetProfileDefaultProvider() throws Exception {
        // Arrange
        String username = "testuser";
        ProfileResponse profile = new ProfileResponse("testuser", "Test", "Bio", "avatar", "url", 5, 10, 20, "Java", List.of());

        when(profileUseCase.getProfile(username, "github")).thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is("testuser")));
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void testGetProfileNotFound() throws Exception {
        // Arrange
        String username = "nonexistent";
        when(profileUseCase.getProfile(username, "github"))
                .thenThrow(new NotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 502 on provider API error")
    void testGetProfileProviderError() throws Exception {
        // Arrange
        String username = "testuser";
        when(profileUseCase.getProfile(username, "github"))
                .thenThrow(new ProviderApiException("Provider API error", new Exception()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("Should validate username format")
    void testGetProfileInvalidUsername() throws Exception {
        // Arrange
        String invalidUsername = "invalid@username!";

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}", invalidUsername)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should retrieve repositories with pagination")
    void testGetRepositoriesSuccess() throws Exception {
        // Arrange
        String username = "octocat";
        String provider = "github";
        int page = 1;
        int perPage = 10;

        List<RepoResponse> repos = List.of(
                new RepoResponse("repo1", "Desc 1", "url1", "Java", 100, 5, 1000L),
                new RepoResponse("repo2", "Desc 2", "url2", "Python", 50, 3, 500L),
                new RepoResponse("repo3", "Desc 3", "url3", "Go", 25, 1, 250L)
        );

        PagedResponse<RepoResponse> response = PagedResponse.of(repos, page, perPage);
        when(profileUseCase.getRepositories(username, provider, page, perPage)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/{username}/repos", username)
                        .param("provider", provider)
                        .param("page", String.valueOf(page))
                        .param("perPage", String.valueOf(perPage))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(page)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].name", is("repo1")))
                .andExpect(jsonPath("$.content[0].language", is("Java")));
    }

    @Test
    @DisplayName("Should validate pagination parameters")
    void testGetRepositoriesBadPagination() throws Exception {
        // Arrange
        String username = "testuser";

        // Act & Assert - page must be at least 1
        mockMvc.perform(get("/api/v1/profiles/{username}/repos", username)
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Act & Assert - perPage must not exceed 100
        mockMvc.perform(get("/api/v1/profiles/{username}/repos", username)
                        .param("perPage", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should retrieve search history")
    void testGetSearchHistorySuccess() throws Exception {
        // Arrange
        SearchSummary summary1 = new SearchSummary(1L, "octocat", "octocat – 8 repos", ZonedDateTime.now().minusDays(1));
        SearchSummary summary2 = new SearchSummary(2L, "torvalds", "torvalds – 10 repos", ZonedDateTime.now());

        when(profileUseCase.getSearchHistory()).thenReturn(List.of(summary1, summary2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/searches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("octocat")))
                .andExpect(jsonPath("$[1].username", is("torvalds")));
    }

    @Test
    @DisplayName("Should return empty search history")
    void testGetSearchHistoryEmpty() throws Exception {
        // Arrange
        when(profileUseCase.getSearchHistory()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/searches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should accept different provider names")
    void testGetProfileMultipleProviders() throws Exception {
        // Arrange
        String username = "testuser";
        ProfileResponse profile = new ProfileResponse("testuser", "Test", "Bio", "avatar", "url", 5, 10, 20, "Java", List.of());

        when(profileUseCase.getProfile(username, "gitlab")).thenReturn(profile);
        when(profileUseCase.getProfile(username, "bitbucket")).thenReturn(profile);

        // Act & Assert for GitLab
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .param("provider", "gitlab")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Act & Assert for Bitbucket
        mockMvc.perform(get("/api/v1/profiles/{username}", username)
                        .param("provider", "bitbucket")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
