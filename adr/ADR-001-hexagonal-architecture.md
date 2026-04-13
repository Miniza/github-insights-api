# ADR-001: Hexagonal Architecture (Ports and Adapters)

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

We need a clean architecture for a Spring Boot 3 service that:

- Consumes the GitHub REST API (external dependency)
- Persists search history to a database (infrastructure dependency)
- Exposes REST endpoints (delivery mechanism)
- Must be production-ready, testable, and maintainable

The challenge explicitly requires "proper structures for packages, classes, etc. Well defined responsibilities, core principles and best practices."

## Decision

Adopt the **Hexagonal Architecture** (Ports and Adapters) pattern, organising the codebase into:

```
controller/       ← Driving adapter (REST API)
application/      ← Use cases, DTOs, mappers
domain/           ← Pure business logic, models, strategies
ports/
  in/             ← Driving ports (use-case interfaces)
  out/            ← Driven ports (external dependency interfaces)
adapters/
  github/         ← Driven adapter (GitHub API client)
  persistence/    ← Driven adapter (JPA / H2)
  cache/          ← Driven adapter (Caffeine cache config)
config/           ← Spring configuration
exception/        ← Global error handling
util/             ← Constants
```

## Rationale

### Why Hexagonal over Layered Architecture

| Concern              | Layered (N-Tier)                                                     | Hexagonal                                                             |
| -------------------- | -------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Dependency direction | Controller → Service → Repository — domain depends on infrastructure | Domain depends on nothing; infrastructure depends on domain via ports |
| Testability          | Need to mock Spring Data, WebClient, etc. inside services            | Service depends on port interfaces — trivial to mock                  |
| Swappability         | Replacing GitHub with GitLab means rewriting the service layer       | Implement a new adapter; service layer is untouched                   |
| Framework coupling   | Business logic often leaks into Spring-annotated classes             | Domain models are plain Java records — zero Spring dependency         |

### How Dependencies Flow

```
HTTP Request
    │
    ▼
Controller (driving adapter)
    │
    ▼
ProfileUseCase (driving port — interface)
    │
    ▼
ProfileService (application service — implements use case)
    │
    ├──► SourceCodeClient (driven port — interface) ──► GitHubWebClientAdapter
    │
    └──► SearchHistoryRepositoryPort (driven port) ──► SearchHistoryRepositoryAdapter
```

The **Dependency Inversion Principle** is enforced: the service never knows whether GitHub data comes from WebClient, RestTemplate, or a test stub — it only sees the port interface.

### Multi-Provider Extensibility

The requirement mentions: "Add implementations to connect to repositories other than GitHub." With hexagonal architecture, this is a matter of:

1. Creating a `GitLabWebClientAdapter` implementing `GitHubClient` (or a renamed `SourceCodeClient` port)
2. Qualifying the bean with `@Qualifier` or using a strategy/factory
3. Zero changes to the service layer

## Consequences

- **Positive:** High testability, clear boundaries, easy to extend with new providers
- **Positive:** Domain models (`User`, `Repo`, `LanguageStat`) are pure Java records with no JPA/Jackson annotations
- **Negative:** More packages and files than a simple layered approach — overhead for a small project
- **Trade-off accepted:** The structural overhead is justified by the requirement for production-readiness and extensibility
