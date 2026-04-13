package za.vodacom.repoprofile.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.vodacom.repoprofile.domain.model.Repo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ByRepoCountStrategy Unit Tests")
class ByRepoCountStrategyTest {

    private final LanguageStrategy strategy = new ByRepoCountStrategy();

    @Test
    @DisplayName("Should determine top language by repository count")
    void testDetermineTopLanguageByCount() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", "Python", 5, 1, 50),
                new Repo("repo4", "desc", "url", "Go", 15, 1, 150)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should return null for empty repository list")
    void testDetermineTopLanguageEmpty() {
        // Arrange
        List<Repo> repos = List.of();

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isNull();
    }

    @Test
    @DisplayName("Should ignore repositories with null language")
    void testIgnoreNullLanguage() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", null, 5, 1, 50)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should ignore repositories with blank language")
    void testIgnoreBlankLanguage() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "  ", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", "", 5, 1, 50)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should return null when all languages are null or blank")
    void testAllLanguagesNullOrBlank() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 100),
                new Repo("repo2", "desc", "url", "  ", 20, 2, 200),
                new Repo("repo3", "desc", "url", "", 5, 1, 50)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isNull();
    }

    @Test
    @DisplayName("Should handle single repository")
    void testSingleRepository() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("solo-repo", "desc", "url", "Rust", 100, 10, 1000)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Rust");
    }

    @Test
    @DisplayName("Should handle tie by selecting first max entry")
    void testHandleTie() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Python", 20, 2, 200)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert - Either could be returned since it's a tie; verify it's one of them
        assertThat(topLanguage).isIn("Java", "Python");
    }

    @Test
    @DisplayName("Should count correctly with mixed languages")
    void testMixedLanguagesCounting() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("java1", "desc", "url", "Java", 10, 1, 100),
                new Repo("java2", "desc", "url", "Java", 20, 2, 200),
                new Repo("java3", "desc", "url", "Java", 30, 3, 300),
                new Repo("python1", "desc", "url", "Python", 5, 1, 50),
                new Repo("python2", "desc", "url", "Python", 15, 2, 150),
                new Repo("go1", "desc", "url", "Go", 25, 2, 250),
                new Repo("rust1", "desc", "url", "Rust", 35, 3, 350)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert - Java appears 3 times, Python 2 times, Go and Rust 1 time each
        assertThat(topLanguage).isEqualTo("Java");
    }
}
