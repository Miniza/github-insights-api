# Profile Insights API

A production-ready Spring Boot 3 application that consumes public source-code hosting APIs (GitHub, GitLab, Bitbucket) and exposes clean REST endpoints for user profile insights, repository listings, and language analytics — with JPA-backed search history.

## Architecture

Built using **Hexagonal Architecture (Ports and Adapters)** to enforce clean boundaries between business logic and infrastructure. **Multi-provider** support via a resolver that maps provider names to adapters at startup.

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Controller                          │
│                  (driving adapter) ?provider=                   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │  ProfileUseCase │  ← driving port (interface)
                    └──────┬──────┘
                           │
               ┌───────────▼───────────┐
               │   ProfileService      │  ← application service
               └───┬───────────────┬───┘
                   │               │
     ┌─────────────▼────────┐ ┌───▼──────────────────┐
     │ SourceCodeClientResolver │ │ SearchHistoryRepoPort│ ← driven ports
     └─────┬──────┬──────┬──┘ └───┬──────────────────┘
           │      │      │        │
    ┌──────▼┐ ┌───▼──┐ ┌─▼─────┐ ┌▼──────────────┐
    │GitHub │ │GitLab│ │Bitbucket│ │ JPA Adapter   │ ← driven adapters
    │Adapter│ │Adapter│ │Adapter │ │ (H2 Database) │
    └───────┘ └──────┘ └────────┘ └───────────────┘
```

## Tech Stack

| Component   | Technology                                                                            |
| ----------- | ------------------------------------------------------------------------------------- |
| Framework   | Spring Boot 3.3.4, Java 21                                                            |
| HTTP Client | WebClient (Spring WebFlux)                                                            |
| Database    | H2 (in-memory) with Spring Data JPA                                                   |
| Caching     | Caffeine (500 entries, 5 min TTL)                                                     |
| Resilience  | Resilience4j (circuit breaker, retry with exponential backoff + jitter, rate limiter) |
| API Docs    | SpringDoc OpenAPI 2.6.0 (Swagger UI)                                                  |
| Logging     | Logstash Logback Encoder (structured JSON in prod)                                    |
| Build       | Maven 3.9.9, Docker multi-stage build                                                 |

## Quick Start

### Docker (recommended)

```bash
docker build -t repo-profile .
docker run -p 8080:8080 repo-profile
```

### Maven (requires Maven 3.9+ and JDK 21)

```bash
./mvnw spring-boot:run
```

## API Endpoints

| Method | Endpoint                            | Description                                    |
| ------ | ----------------------------------- | ---------------------------------------------- |
| `GET`  | `/api/v1/profiles/{username}`       | User profile with top language                 |
| `GET`  | `/api/v1/profiles/{username}/repos` | User repositories (paginated, sorted by stars) |
| `GET`  | `/api/v1/searches`                  | Recent search history (last 50)                |

All profile and repository endpoints accept an optional `?provider=` query parameter (`github`, `gitlab`, `bitbucket`). Defaults to `github`.

### Example — Profile

```bash
curl http://localhost:8080/api/v1/profiles/octocat
```

```json
{
  "login": "octocat",
  "name": "The Octocat",
  "bio": null,
  "avatarUrl": "https://avatars.githubusercontent.com/u/583231?v=4",
  "htmlUrl": "https://github.com/octocat",
  "publicRepos": 8,
  "followers": 17000,
  "following": 9,
  "topLanguage": "Ruby",
  "repositories": [ ... ]
}
```

### Example — Paginated Repositories

```bash
# Page 2, 5 items per page
curl "http://localhost:8080/api/v1/profiles/octocat/repos?page=2&perPage=5"
```

```json
{
  "content": [
    {
      "name": "Hello-World",
      "description": "My first repository on GitHub!",
      "htmlUrl": "https://github.com/octocat/Hello-World",
      "language": "Ruby",
      "stargazersCount": 2500,
      "forksCount": 2300,
      "size": 1
    }
  ],
  "page": 2,
  "perPage": 5,
  "totalItems": 8,
  "totalPages": 2
}
```

| Parameter  | Default  | Description           |
| ---------- | -------- | --------------------- |
| `page`     | `1`      | Page number (1-based) |
| `perPage`  | `10`     | Results per page      |
| `provider` | `github` | Source-code provider  |

### Example — Different Provider

```bash
# GitLab (skeleton — not yet implemented)
curl "http://localhost:8080/api/v1/profiles/john/repos?provider=gitlab"
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## Project Structure

```
src/main/java/za/vodacom/repoprofile/
├── controller/              # REST controllers (driving adapter)
├── application/
│   ├── dto/                 # Response DTOs (incl. PagedResponse)
│   ├── mapper/              # Domain → DTO mappers
│   └── service/             # Use case implementations
├── domain/
│   ├── model/               # Pure domain models (Java records) + ProviderType enum
│   └── strategy/            # Language resolution strategies (configurable)
├── ports/
│   ├── in/                  # Driving port interfaces
│   └── out/                 # Driven port interfaces (SourceCodeClient)
├── adapters/
│   ├── github/              # GitHub WebClient adapter + DTOs (fully implemented)
│   ├── gitlab/              # GitLab adapter (skeleton)
│   ├── bitbucket/           # Bitbucket adapter (skeleton)
│   ├── persistence/         # JPA adapter + entities
│   └── cache/               # Caffeine cache configuration
├── config/                  # WebClient, resilience, OpenAPI, logging filter, SourceCodeClientResolver
├── exception/               # Global exception handler + ProviderApiException
└── util/                    # Constants
```

## Multi-Provider Support

The application is designed to support multiple source-code hosting providers. Each provider is a separate adapter implementing the `SourceCodeClient` port interface.

| Provider  | Status            | Adapter Class               |
| --------- | ----------------- | --------------------------- |
| GitHub    | Fully implemented | `GitHubWebClientAdapter`    |
| GitLab    | Skeleton          | `GitLabWebClientAdapter`    |
| Bitbucket | Skeleton          | `BitbucketWebClientAdapter` |

Adding a new provider requires:

1. A new adapter class implementing `SourceCodeClient`
2. A `@Component("providerName")` annotation
3. A WebClient bean for the provider's base URL
4. **Zero changes** to the service layer, controller, or domain model

## Resilience

### Circuit Breakers

| Circuit    | Window   | Failure Threshold | Open Duration | Protects                   |
| ---------- | -------- | ----------------- | ------------- | -------------------------- |
| `github`   | 10 calls | 50%               | 30s           | GitHub API calls           |
| `database` | 5 calls  | 50%               | 15s           | Search history persistence |

Database failures degrade gracefully — profile lookups still succeed.

### Retry with Exponential Backoff + Jitter

| Retry | Base Delay | With Jitter (±50%) |
| ----- | ---------- | ------------------ |
| 1st   | 1s         | 0.5s – 1.5s        |
| 2nd   | 2s         | 1.0s – 3.0s        |
| 3rd   | 4s         | 2.0s – 6.0s        |

### Rate Limiter

10 GitHub API calls per 60 seconds — protects against rate limit exhaustion.

## Caching

Caffeine local cache with 500 max entries and 5-minute TTL. Applied at the adapter layer — a cache hit bypasses the circuit breaker and never touches the GitHub API.

## Observability

### Logging

| Profile | Format                            |
| ------- | --------------------------------- |
| `dev`   | Human-readable console            |
| `prod`  | Structured JSON (Logstash format) |

Every request is tagged with a `correlationId` (UUID) for end-to-end tracing.

### Actuator Endpoints

```
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/caches
http://localhost:8080/actuator/circuitbreakers
http://localhost:8080/actuator/retries
http://localhost:8080/actuator/ratelimiters
```

## Configuration

All configuration is externalised to `application.yml`:

| Property                     | Default                  | Description                                     |
| ---------------------------- | ------------------------ | ----------------------------------------------- |
| `github.api.base-url`        | `https://api.github.com` | GitHub API base URL                             |
| `github.api.token`           | `""`                     | Optional auth token (bypasses 60 req/hr limit)  |
| `github.api.connect-timeout` | `5s`                     | Connection timeout                              |
| `github.api.read-timeout`    | `10s`                    | Read timeout                                    |
| `cache.max-size`             | `500`                    | Max cached entries                              |
| `cache.ttl`                  | `300s`                   | Cache time-to-live                              |
| `search-history.max-records` | `50`                     | Max stored search records                       |
| `language.strategy`          | `byRepoCount`            | Language ranking: `byRepoCount` or `byRepoSize` |

## Architecture Decision Records

Detailed rationale for every major design decision:

| ADR                                                        | Decision                                                |
| ---------------------------------------------------------- | ------------------------------------------------------- |
| [ADR-001](docs/adr/ADR-001-hexagonal-architecture.md)      | Hexagonal Architecture (Ports and Adapters)             |
| [ADR-002](docs/adr/ADR-002-webclient-over-resttemplate.md) | WebClient over RestTemplate                             |
| [ADR-003](docs/adr/ADR-003-resilience4j-patterns.md)       | Resilience4j for circuit breaking, retry, rate limiting |
| [ADR-004](docs/adr/ADR-004-caffeine-cache.md)              | Caffeine cache for rate limit mitigation                |
| [ADR-005](docs/adr/ADR-005-strategy-pattern-language.md)   | Strategy pattern for language resolution                |
| [ADR-006](docs/adr/ADR-006-structured-json-logging.md)     | Structured JSON logging with correlation IDs            |
| [ADR-007](docs/adr/ADR-007-docker-containerisation.md)     | Docker multi-stage build                                |
| [ADR-008](docs/adr/ADR-008-h2-database.md)                 | H2 in-memory database                                   |

## Design Patterns Used

| Pattern                | Where                          | Why                                                                |
| ---------------------- | ------------------------------ | ------------------------------------------------------------------ |
| **Ports and Adapters** | Entire architecture            | Dependency inversion, testability, swappable infrastructure        |
| **Strategy**           | Language resolution            | Open/Closed principle — multiple algorithms, configurable via YAML |
| **Adapter**            | GitHub, GitLab, Bitbucket, JPA | Isolate external dependencies behind domain interfaces             |
| **Circuit Breaker**    | GitHub + database adapters     | Graceful degradation, prevent cascading failures                   |
| **Factory/Resolver**   | SourceCodeClientResolver       | Selects adapter by provider name at runtime                        |
| **Builder**            | WebClient configuration        | Fluent, readable HTTP client setup                                 |
