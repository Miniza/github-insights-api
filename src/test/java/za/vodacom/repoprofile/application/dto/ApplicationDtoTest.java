package za.vodacom.repoprofile.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Application DTO Unit Tests")
class ApplicationDtoTest {

    @Test
    @DisplayName("RepoResponse record fields")
    void testRepoResponseCreation() {
        RepoResponse repo = new RepoResponse(
                "Hello-World",
                "Hello World!",
                "https://github.com/octocat/Hello-World",
                "Java",
                80,
                9,
                180L
        );

        // Assert
        assertThat(repo)
                .isNotNull()
                .extracting(RepoResponse::name, RepoResponse::description, RepoResponse::language)
                .containsExactly("Hello-World", "Hello World!", "Java");

        assertThat(repo)
                .extracting(RepoResponse::stargazersCount, RepoResponse::forksCount, RepoResponse::size)
                .containsExactly(80, 9, 180L);
    }

    @Test
    @DisplayName("ProfileResponse record fields")
    void testProfileResponseCreation() {
        List<RepoResponse> repos = List.of(
                new RepoResponse("repo1", "Desc1", "url1", "Java", 100, 5, 1000L),
                new RepoResponse("repo2", "Desc2", "url2", "Python", 50, 3, 500L)
        );

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
                repos
        );

        // Assert
        assertThat(profile)
                .isNotNull()
                .extracting(ProfileResponse::login, ProfileResponse::name, ProfileResponse::topLanguage)
                .containsExactly("octocat", "The Octocat", "Java");

        assertThat(profile.repositories()).hasSize(2);
    }

    @Test
    @DisplayName("SearchSummary record fields")
    void testSearchSummaryCreation() {
        ZonedDateTime now = ZonedDateTime.now();

        SearchSummary summary = new SearchSummary(
                1L,
                "octocat",
                "octocat – 8 repos – Java",
                now
        );

        // Assert
        assertThat(summary)
                .isNotNull()
                .extracting(SearchSummary::id, SearchSummary::username, SearchSummary::summary)
                .containsExactly(1L, "octocat", "octocat – 8 repos – Java");

        assertThat(summary.searchedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ErrorResponse record fields")
    void testErrorResponseCreation() {
        Instant now = Instant.now();

        ErrorResponse error = new ErrorResponse(
                404,
                "Not Found",
                "User not found",
                now
        );

        // Assert
        assertThat(error)
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error, ErrorResponse::message)
                .containsExactly(404, "Not Found", "User not found");

        assertThat(error.timestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("ErrorResponse.of() factory")
    void testErrorResponseFactory() {
        ErrorResponse error = ErrorResponse.of(502, "Bad Gateway", "Provider unavailable");

        // Assert
        assertThat(error)
                .isNotNull()
                .extracting(ErrorResponse::status, ErrorResponse::error, ErrorResponse::message)
                .containsExactly(502, "Bad Gateway", "Provider unavailable");

        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Null description in RepoResponse")
    void testRepoResponseNullDescription() {
        RepoResponse repo = new RepoResponse("repo", null, "url", "Java", 10, 1, 100L);

        // Assert
        assertThat(repo.description()).isNull();
    }

    @Test
    @DisplayName("Null language in RepoResponse")
    void testRepoResponseNullLanguage() {
        RepoResponse repo = new RepoResponse("repo", "Desc", "url", null, 10, 1, 100L);

        // Assert
        assertThat(repo.language()).isNull();
    }

    @Test
    @DisplayName("Null topLanguage in ProfileResponse")
    void testProfileResponseNullTopLanguage() {
        ProfileResponse profile = new ProfileResponse(
                "user", "Name", "Bio", "avatar", "url", 5, 10, 20, null, List.of()
        );

        // Assert
        assertThat(profile.topLanguage()).isNull();
    }

    @Test
    @DisplayName("Empty repositories list")
    void testProfileResponseEmptyRepos() {
        ProfileResponse profile = new ProfileResponse(
                "user", "Name", "Bio", "avatar", "url", 0, 0, 0, null, List.of()
        );

        // Assert
        assertThat(profile.repositories()).isEmpty();
    }

    @Test
    @DisplayName("RepoResponse equality")
    void testRepoResponseEquality() {
        RepoResponse repo1 = new RepoResponse("repo", "Desc", "url", "Java", 10, 1, 100L);
        RepoResponse repo2 = new RepoResponse("repo", "Desc", "url", "Java", 10, 1, 100L);
        assertThat(repo1).isEqualTo(repo2);
    }

    @Test
    @DisplayName("SearchSummary equality")
    void testSearchSummaryEquality() {
        ZonedDateTime time = ZonedDateTime.now();
        SearchSummary summary1 = new SearchSummary(1L, "user", "summary", time);
        SearchSummary summary2 = new SearchSummary(1L, "user", "summary", time);
        assertThat(summary1).isEqualTo(summary2);
    }

    @Test
    @DisplayName("Should test ErrorResponse equality")
    void testErrorResponseEquality() {
        // Arrange
        Instant time = Instant.now();
        ErrorResponse error1 = new ErrorResponse(404, "Not Found", "message", time);
        ErrorResponse error2 = new ErrorResponse(404, "Not Found", "message", time);

        // Assert
        assertThat(error1).isEqualTo(error2);
    }
}
