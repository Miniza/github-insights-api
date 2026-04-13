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
    @DisplayName("Top language by repo count")
    void testDetermineTopLanguageByCount() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", "Python", 5, 1, 50),
                new Repo("repo4", "desc", "url", "Go", 15, 1, 150)
        );

        String topLanguage = strategy.determineTopLanguage(repos);

        assertThat(topLanguage).isEqualTo("Java");
    }

    @Test
    @DisplayName("Empty list returns null")
    void testDetermineTopLanguageEmpty() {
        List<Repo> repos = List.of();

        assertThat(strategy.determineTopLanguage(repos)).isNull();
    }

    @Test
    @DisplayName("Null languages ignored")
    void testIgnoreNullLanguage() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", null, 5, 1, 50)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Blank languages ignored")
    void testIgnoreBlankLanguage() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "  ", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Java", 20, 2, 200),
                new Repo("repo3", "desc", "url", "", 5, 1, 50)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }

    @Test
    @DisplayName("All null/blank → null")
    void testAllLanguagesNullOrBlank() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", null, 10, 1, 100),
                new Repo("repo2", "desc", "url", "  ", 20, 2, 200),
                new Repo("repo3", "desc", "url", "", 5, 1, 50)
        );

        assertThat(strategy.determineTopLanguage(repos)).isNull();
    }

    @Test
    @DisplayName("Single repo")
    void testSingleRepository() {
        List<Repo> repos = List.of(
                new Repo("solo-repo", "desc", "url", "Rust", 100, 10, 1000)
        );

        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Rust");
    }

    @Test
    @DisplayName("Tie picks one of the max entries")
    void testHandleTie() {
        List<Repo> repos = List.of(
                new Repo("repo1", "desc", "url", "Java", 10, 1, 100),
                new Repo("repo2", "desc", "url", "Python", 20, 2, 200)
        );

        assertThat(strategy.determineTopLanguage(repos)).isIn("Java", "Python");
    }

    @Test
    @DisplayName("Mixed languages counted correctly")
    void testMixedLanguagesCounting() {
        List<Repo> repos = List.of(
                new Repo("java1", "desc", "url", "Java", 10, 1, 100),
                new Repo("java2", "desc", "url", "Java", 20, 2, 200),
                new Repo("java3", "desc", "url", "Java", 30, 3, 300),
                new Repo("python1", "desc", "url", "Python", 5, 1, 50),
                new Repo("python2", "desc", "url", "Python", 15, 2, 150),
                new Repo("go1", "desc", "url", "Go", 25, 2, 250),
                new Repo("rust1", "desc", "url", "Rust", 35, 3, 350)
        );

        // Java=3, Python=2, Go=1, Rust=1
        assertThat(strategy.determineTopLanguage(repos)).isEqualTo("Java");
    }
}
