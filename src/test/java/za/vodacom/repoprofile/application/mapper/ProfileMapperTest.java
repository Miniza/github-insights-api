package za.vodacom.repoprofile.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.vodacom.repoprofile.application.dto.ProfileResponse;
import za.vodacom.repoprofile.application.dto.RepoResponse;
import za.vodacom.repoprofile.application.dto.SearchSummary;
import za.vodacom.repoprofile.domain.model.Repo;
import za.vodacom.repoprofile.domain.model.SearchRecord;
import za.vodacom.repoprofile.domain.model.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProfileMapper Unit Tests")
class ProfileMapperTest {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    @Test
    @DisplayName("Repo → RepoResponse mapping")
    void testToRepoResponse() {
        Repo repo = new Repo(
                "Hello-World",
                "Hello World!",
                "https://github.com/octocat/Hello-World",
                "Java",
                80,
                9,
                180L
        );

        // Act
        RepoResponse response = ProfileMapper.toRepoResponse(repo);

        // Assert
        assertThat(response)
                .isNotNull()
                .extracting(
                        RepoResponse::name,
                        RepoResponse::description,
                        RepoResponse::htmlUrl,
                        RepoResponse::language,
                        RepoResponse::stargazersCount,
                        RepoResponse::forksCount,
                        RepoResponse::size
                )
                .containsExactly(
                        "Hello-World",
                        "Hello World!",
                        "https://github.com/octocat/Hello-World",
                        "Java",
                        80,
                        9,
                        180L
                );
    }

    @Test
    @DisplayName("Null description preserved in RepoResponse")
    void testToRepoResponseNullDescription() {
        Repo repo = new Repo(
                "C-Sharp-Sample",
                null,
                "https://github.com/octocat/C-Sharp-Sample",
                "C#",
                0,
                0,
                1L
        );

        RepoResponse response = ProfileMapper.toRepoResponse(repo);

        assertThat(response)
                .isNotNull()
                .extracting(RepoResponse::name, RepoResponse::description)
                .containsExactly("C-Sharp-Sample", null);
    }

    @Test
    @DisplayName("User + repos → ProfileResponse")
    void testToProfileResponse() {
        // Arrange
        User user = new User(
                "octocat",
                "The Octocat",
                "GitHub's mascot",
                "https://avatars.githubusercontent.com/u/1?",
                "https://github.com/octocat",
                8,
                3938,
                9938
        );

        List<RepoResponse> repos = List.of(
                new RepoResponse("Hello-World", "Hello World!", "https://github.com/octocat/Hello-World", "Java", 80, 9, 180L),
                new RepoResponse("C-Sharp-Sample", null, "https://github.com/octocat/C-Sharp-Sample", "C#", 0, 0, 1L)
        );

        ProfileResponse response = ProfileMapper.toProfileResponse(user, "Java", repos);

        assertThat(response)
                .isNotNull()
                .extracting(
                        ProfileResponse::login,
                        ProfileResponse::name,
                        ProfileResponse::bio,
                        ProfileResponse::avatarUrl,
                        ProfileResponse::htmlUrl,
                        ProfileResponse::publicRepos,
                        ProfileResponse::followers,
                        ProfileResponse::following,
                        ProfileResponse::topLanguage
                )
                .containsExactly(
                        "octocat",
                        "The Octocat",
                        "GitHub's mascot",
                        "https://avatars.githubusercontent.com/u/1?",
                        "https://github.com/octocat",
                        8,
                        3938,
                        9938,
                        "Java"
                );

        assertThat(response.repositories()).isEqualTo(repos);
    }

    @Test
    @DisplayName("Null top language in ProfileResponse")
    void testToProfileResponseNullTopLanguage() {
        User user = new User("testuser", "Test User", "Bio", "avatar", "url", 5, 10, 20);
        List<RepoResponse> repos = List.of();

        ProfileResponse response = ProfileMapper.toProfileResponse(user, null, repos);

        assertThat(response)
                .isNotNull()
                .extracting(ProfileResponse::topLanguage)
                .isNull();
    }

    @Test
    @DisplayName("SearchRecord → SearchSummary with SAST timezone")
    void testToSearchSummary() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        SearchRecord record = new SearchRecord(
                1L,
                "octocat",
                "octocat – 8 repos – Java",
                instant
        );

        SearchSummary summary = ProfileMapper.toSearchSummary(record);

        assertThat(summary)
                .isNotNull()
                .extracting(SearchSummary::id, SearchSummary::username, SearchSummary::summary)
                .containsExactly(1L, "octocat", "octocat – 8 repos – Java");

        // Verify timezone conversion
        assertThat(summary.searchedAt()).isNotNull();
        assertThat(summary.searchedAt().getZone()).isEqualTo(SAST);

        // Verify the instant is correctly converted
        assertThat(summary.searchedAt().toInstant()).isEqualTo(instant);
    }

    @Test
    @DisplayName("buildSummary format")
    void testBuildSummary() {
        String username = "octocat";
        int repoCount = 8;
        String topLanguage = "Java";

        // Act
        String summary = ProfileMapper.buildSummary(username, repoCount, topLanguage);

        // Assert
        assertThat(summary).isEqualTo("octocat – 8 repos – Java");
    }

    @Test
    @DisplayName("Should build summary with null top language")
    void testBuildSummaryNullLanguage() {
        // Arrange
        String username = "testuser";
        int repoCount = 5;

        // Act
        String summary = ProfileMapper.buildSummary(username, repoCount, null);

        // Assert
        assertThat(summary).isEqualTo("testuser – 5 repos – N/A");
    }

    @Test
    @DisplayName("Should build summary with zero repos")
    void testBuildSummaryZeroRepos() {
        // Arrange
        String username = "newuser";
        int repoCount = 0;
        String topLanguage = "JavaScript";

        // Act
        String summary = ProfileMapper.buildSummary(username, repoCount, topLanguage);

        // Assert
        assertThat(summary).isEqualTo("newuser – 0 repos – JavaScript");
    }

    @Test
    @DisplayName("Should build summary with large repo count")
    void testBuildSummaryLargeRepoCount() {
        // Arrange
        String username = "prolificdev";
        int repoCount = 1000;
        String topLanguage = "Python";

        // Act
        String summary = ProfileMapper.buildSummary(username, repoCount, topLanguage);

        // Assert
        assertThat(summary).isEqualTo("prolificdev – 1000 repos – Python");
    }

    @Test
    @DisplayName("Should map ProfileResponse to empty repository list")
    void testToProfileResponseEmptyRepos() {
        // Arrange
        User user = new User("emptyuser", "Empty User", "No repos", "avatar", "url", 0, 0, 0);

        // Act
        ProfileResponse response = ProfileMapper.toProfileResponse(user, null, List.of());

        // Assert
        assertThat(response)
                .isNotNull()
                .extracting(ProfileResponse::login, ProfileResponse::publicRepos)
                .containsExactly("emptyuser", 0);

        assertThat(response.repositories()).isEmpty();
    }

    @Test
    @DisplayName("Should map multiple repositories correctly")
    void testToProfileResponseMultipleRepos() {
        // Arrange
        User user = new User("multidev", "Multi Developer", "Multiple repos", "avatar", "url", 10, 50, 100);

        List<RepoResponse> repos = List.of(
                new RepoResponse("repo1", "First", "url1", "Java", 100, 5, 1000L),
                new RepoResponse("repo2", "Second", "url2", "Python", 50, 3, 500L),
                new RepoResponse("repo3", "Third", "url3", "Go", 25, 1, 250L),
                new RepoResponse("repo4", "Fourth", "url4", null, 10, 0, 100L)
        );

        // Act
        ProfileResponse response = ProfileMapper.toProfileResponse(user, "Java", repos);

        // Assert
        assertThat(response.repositories())
                .hasSize(4)
                .extracting(RepoResponse::name)
                .containsExactly("repo1", "repo2", "repo3", "repo4");
    }
}
