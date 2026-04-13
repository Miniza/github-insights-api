# ADR-005: Strategy Pattern for Language Resolution

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

The challenge states: "The programming language the user uses the most (by repo count OR total size — your choice)."

This implies:

1. There are at least two valid algorithms
2. The choice should be deliberate and explainable
3. The system should be open to extension

## Decision

Use the **Strategy pattern** to encapsulate language-resolution algorithms behind a common interface.

### Interface

```java
public interface LanguageStrategy {
    List<LanguageStat> resolve(List<Repo> repos);
}
```

### Implementations

| Strategy              | Bean Name     | Algorithm                                          |
| --------------------- | ------------- | -------------------------------------------------- |
| `ByRepoCountStrategy` | `byRepoCount` | Count repos per language, rank by count            |
| `ByRepoSizeStrategy`  | `byRepoSize`  | Sum `size` field per language, rank by total bytes |

### Injection

The active strategy is injected via `@Qualifier`:

```java
public ProfileService(
    SourceCodeClientResolver clientResolver,
    SearchHistoryRepositoryPort searchHistoryRepository,
    Map<String, LanguageStrategy> strategies,
    @Value("${language.strategy:byRepoCount}") String strategyName
) { ... }
```

## Rationale

### Why not just pick one?

A senior engineer demonstrates **design thinking**, not just "it works." The Strategy pattern shows:

- **Open/Closed Principle:** Adding a new algorithm (e.g., `ByCommitFrequency`) requires zero changes to existing code
- **Testability:** Each strategy is independently unit-testable
- **Configurability:** The active strategy can be swapped via Spring configuration

### Why Strategy over Enum/Switch?

| Approach              | Problem                                                          |
| --------------------- | ---------------------------------------------------------------- |
| `if/else` or `switch` | Violates OCP — adding a new algorithm modifies the service       |
| Enum with method      | Couples algorithm to an enum — harder to inject dependencies     |
| Strategy + Spring DI  | Each strategy is a `@Component`, testable, injectable, swappable |

### Why `byRepoCount` as the default?

- More intuitive: "This user has 15 Java repos and 3 Python repos → primary language is Java"
- `byRepoSize` can be misleading: one large C++ repo (500MB) could outweigh 20 small JavaScript repos
- Both are available if the requirement changes

## Consequences

- **Positive:** Requirement ambiguity is turned into a strength — we support both interpretations
- **Positive:** Adding new strategies (e.g., by commit count via another API call) is trivial
- **Positive:** Clean separation of domain logic from the service layer
- **Negative:** Two classes instead of one — slight overhead for a simple algorithm
- **Trade-off accepted:** Demonstrates design principles appropriate for a senior/staff submission
