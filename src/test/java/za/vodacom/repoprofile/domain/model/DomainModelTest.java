package za.vodacom.repoprofile.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Domain Model Unit Tests")
class DomainModelTest {

    @Test
    @DisplayName("User record creation")
    void testUserCreation() {
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
    @DisplayName("Repo record creation")
    void testRepoCreation() {
        Repo repo = new Repo(
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
                .extracting(Repo::name, Repo::description, Repo::language)
                .containsExactly("Hello-World", "Hello World!", "Java");

        assertThat(repo)
                .extracting(Repo::stargazersCount, Repo::forksCount, Repo::size)
                .containsExactly(80, 9, 180L);
    }

    @Test
    @DisplayName("SearchRecord creation")
    void testSearchRecordCreation() {
        java.time.Instant instant = java.time.Instant.now();

        SearchRecord record = new SearchRecord(
                1L,
                "octocat",
                "octocat – 8 repos – Java",
                instant
        );

        // Assert
        assertThat(record)
                .isNotNull()
                .extracting(SearchRecord::id, SearchRecord::username, SearchRecord::summary)
                .containsExactly(1L, "octocat", "octocat – 8 repos – Java");

        assertThat(record.searchedAt()).isEqualTo(instant);
    }

    @Test
    @DisplayName("User equality")
    void testUserEquality() {
        User user1 = new User("test", "Test", "Bio", "avatar", "url", 5, 10, 20);
        User user2 = new User("test", "Test", "Bio", "avatar", "url", 5, 10, 20);
        User user3 = new User("other", "Other", "Bio", "avatar", "url", 5, 10, 20);
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
    }

    @Test
    @DisplayName("Repo equality")
    void testRepoEquality() {
        Repo repo1 = new Repo("repo", "Desc", "url", "Java", 10, 1, 100L);
        Repo repo2 = new Repo("repo", "Desc", "url", "Java", 10, 1, 100L);
        Repo repo3 = new Repo("other", "Desc", "url", "Java", 10, 1, 100L);
        assertThat(repo1).isEqualTo(repo2);
        assertThat(repo1).isNotEqualTo(repo3);
    }

    @Test
    @DisplayName("User with null name/bio")
    void testUserWithNullValues() {
        assertThatNoException().isThrownBy(() -> {
            User user = new User("test", null, null, null, null, 0, 0, 0);
            assertThat(user.name()).isNull();
            assertThat(user.bio()).isNull();
        });
    }

    @Test
    @DisplayName("Repo with null description/language")
    void testRepoWithNullValues() {
        assertThatNoException().isThrownBy(() -> {
            Repo repo = new Repo("repo", null, "url", null, 0, 0, 0L);
            assertThat(repo.description()).isNull();
            assertThat(repo.language()).isNull();
        });
    }

    @Test
    @DisplayName("Zero-valued repo fields")
    void testRepoWithZeroValues() {
        Repo repo = new Repo("repo", "desc", "url", "Java", 0, 0, 0L);
        assertThat(repo.stargazersCount()).isZero();
        assertThat(repo.forksCount()).isZero();
        assertThat(repo.size()).isZero();
    }

    @Test
    @DisplayName("Null summary in SearchRecord")
    void testSearchRecordWithNullSummary() {
        assertThatNoException().isThrownBy(() -> {
            SearchRecord record = new SearchRecord(1L, "user", null, java.time.Instant.now());
            assertThat(record.summary()).isNull();
        });
    }
}
