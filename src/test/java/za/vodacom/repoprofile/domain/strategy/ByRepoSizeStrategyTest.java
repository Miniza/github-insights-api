package za.vodacom.repoprofile.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.vodacom.repoprofile.domain.model.Repo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ByRepoSizeStrategy Unit Tests")
class ByRepoSizeStrategyTest {

    private final LanguageStrategy strategy = new ByRepoSizeStrategy();

    @Test
    @DisplayName("Should determine top language by total repository size")
    void testDetermineTopLanguageBySize() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 1000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "Python", 10, 1, 500),
                new Repo("repo4", "desc", "url", "Go", 10, 1, 800)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert - Java total: 3000, Python: 500, Go: 800
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
                new Repo("repo1", "desc", "url", null, 10, 1, 5000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", null, 10, 1, 3000)
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
                new Repo("repo1", "desc", "url", "  ", 10, 1, 5000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "", 10, 1, 3000)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should sum sizes correctly for multiple repositories of same language")
    void testSumMultipleLanguages() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("java1", "desc", "url", "Java", 10, 1, 100),
                new Repo("java2", "desc", "url", "Java", 10, 1, 200),
                new Repo("java3", "desc", "url", "Java", 10, 1, 300),
                new Repo("python1", "desc", "url", "Python", 10, 1, 150),
                new Repo("python2", "desc", "url", "Python", 10, 1, 250),
                new Repo("go1", "desc", "url", "Go", 10, 1, 600),
                new Repo("rust1", "desc", "url", "Rust", 10, 1, 50)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert - Java: 600, Python: 400, Go: 600, Rust: 50
        // Go and Java are tied at 600, should return one of them
        assertThat(topLanguage).isIn("Java", "Go");
    }

    @Test
    @DisplayName("Should handle single repository")
    void testSingleRepository() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("solo-repo", "desc", "url", "Rust", 10, 1, 500)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Rust");
    }

    @Test
    @DisplayName("Should correctly handle large sizes")
    void testLargeSizes() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 1000000),
                new Repo("repo2", "desc", "url", "Python", 10, 1, 500000),
                new Repo("repo3", "desc", "url", "Go", 10, 1, 250000)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should handle zero-sized repositories")
    void testZeroSizedRepositories() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 0),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 0),
                new Repo("repo3", "desc", "url", "Python", 10, 1, 100)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert - Python has the highest total size
        assertThat(topLanguage).isEqualTo("Python");
    }

    @Test
    @DisplayName("Should return null when all languages are null or blank")
    void testAllLanguagesNullOrBlank() {
        // Arrange
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 5000),
                new Repo("repo2", "desc", "url", "  ", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "", 10, 1, 3000)
        );

        // Act
        String topLanguage = strategy.determineTopLanguage(repos);

        // Assert
        assertThat(topLanguage).isNull();
    }
}
