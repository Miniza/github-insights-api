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
    @DisplayName("Top language by total size")
    void testDetermineTopLanguageBySize() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 1000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "Python", 10, 1, 500),
                new Repo("repo4", "desc", "url", "Go", 10, 1, 800)
        );

        // Java: 3000, Python: 500, Go: 800
        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Empty list returns null")
    void testDetermineTopLanguageEmpty() {
        assertThat(strategy.determineTopLanguage(List.of())).isNull();
    }

    @Test
    @DisplayName("Null languages excluded")
    void testIgnoreNullLanguage() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 5000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", null, 10, 1, 3000)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Blank languages excluded")
    void testIgnoreBlankLanguage() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "  ", 10, 1, 5000),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "", 10, 1, 3000)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Sums sizes per language")
    void testSumMultipleLanguages() {
        List<Repo> repos = List.of(
                new Repo("java1", "desc", "url", "Java", 10, 1, 100),
                new Repo("java2", "desc", "url", "Java", 10, 1, 200),
                new Repo("java3", "desc", "url", "Java", 10, 1, 300),
                new Repo("python1", "desc", "url", "Python", 10, 1, 150),
                new Repo("python2", "desc", "url", "Python", 10, 1, 250),
                new Repo("go1", "desc", "url", "Go", 10, 1, 600),
                new Repo("rust1", "desc", "url", "Rust", 10, 1, 50)
        );

        // Java: 600, Python: 400, Go: 600, Rust: 50 — tie between Java and Go
        assertThat(strategy.determineTopLanguage(repos)).isIn("Java", "Go");
    }

    @Test
    @DisplayName("Single repo")
    void testSingleRepository() {
        var repos = List.of(new Repo("solo-repo", "desc", "url", "Rust", 10, 1, 500));
        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Rust");
    }

    @Test
    @DisplayName("Large sizes handled correctly")
    void testLargeSizes() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 1000000),
                new Repo("repo2", "desc", "url", "Python", 10, 1, 500000),
                new Repo("repo3", "desc", "url", "Go", 10, 1, 250000)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Zero-sized repos lose to non-zero")
    void testZeroSizedRepositories() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 0),
                new Repo("repo2", "desc", "url", "Java", 10, 1, 0),
                new Repo("repo3", "desc", "url", "Python", 10, 1, 100)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Python");
    }

    @Test
    @DisplayName("All null/blank → null")
    void testAllLanguagesNullOrBlank() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 5000),
                new Repo("repo2", "desc", "url", "  ", 10, 1, 2000),
                new Repo("repo3", "desc", "url", "", 10, 1, 3000)
        );

        assertThat(strategy.determineTopLanguage(repos)).isNull();
    }
}
