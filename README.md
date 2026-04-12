# GitHub Profile Insights API

A production-ready Spring Boot 3 application that consumes the public GitHub REST API and exposes clean REST endpoints for user profile insights, repository listings, and language analytics — with JPA-backed search history.

## Architecture

Built using **Hexagonal Architecture (Ports and Adapters)** to enforce clean boundaries between business logic and infrastructure.

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Controller                          │
│                      (driving adapter)                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │ GitHubUseCase│  ← driving port (interface)
                    └──────┬──────┘
                           │
               ┌───────────▼───────────┐
               │  GitHubProfileService  │  ← application service
               └───┬───────────────┬───┘
                   │               │
          ┌────────▼──────┐  ┌─────▼──────────────────┐
          │  GitHubClient │  │ SearchHistoryRepoPort   │  ← driven ports
          └────────┬──────┘  └─────┬──────────────────┘
                   │               │
        ┌──────────▼──────┐  ┌─────▼──────────────────┐
        │ WebClient Adapter│  │   JPA Adapter           │  ← driven adapters
        │ (GitHub API)     │  │   (H2 Database)         │
        └─────────────────┘  └────────────────────────┘
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

| Method | Endpoint                         | Description                       |
| ------ | -------------------------------- | --------------------------------- |
| `GET`  | `/api/github/profile/{username}` | User profile with top language    |
| `GET`  | `/api/github/repos/{username}`   | User repositories sorted by stars |
| `GET`  | `/api/github/history`            | Recent search history (last 50)   |

### Example

```bash
curl http://localhost:8080/api/github/profile/octocat
```

```json
{
  "login": "octocat",
  "name": "The Octocat",
  "bio": null,
  "avatarUrl": "https://avatars.githubusercontent.com/u/583231?v=4",
  "profileUrl": "https://github.com/octocat",
  "publicRepos": 8,
  "followers": 17000,
  "following": 9,
  "topLanguages": [{ "language": "Ruby", "repoCount": 3, "percentage": 37.5 }],
  "repositories": [
    {
      "name": "Hello-World",
      "description": "My first repository on GitHub!",
      "url": "https://github.com/octocat/Hello-World",
      "language": "Ruby",
      "stars": 2500,
      "forks": 2300,
      "sizeKb": 1
    }
  ]
}
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
│   ├── dto/                 # Response DTOs
│   ├── mapper/              # Domain → DTO mappers
│   └── service/             # Use case implementations
├── domain/
│   ├── model/               # Pure domain models (Java records)
│   └── strategy/            # Language resolution strategies
├── ports/
│   ├── in/                  # Driving port interfaces
│   └── out/                 # Driven port interfaces
├── adapters/
│   ├── github/              # GitHub WebClient adapter + DTOs
│   ├── persistence/         # JPA adapter + entities
│   └── cache/               # Caffeine cache configuration
├── config/                  # WebClient, resilience, OpenAPI, logging filter
├── exception/               # Global exception handler
└── util/                    # Constants
```

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

| Property                     | Default                  | Description                                    |
| ---------------------------- | ------------------------ | ---------------------------------------------- |
| `github.api.base-url`        | `https://api.github.com` | GitHub API base URL                            |
| `github.api.token`           | `""`                     | Optional auth token (bypasses 60 req/hr limit) |
| `github.api.connect-timeout` | `5s`                     | Connection timeout                             |
| `github.api.read-timeout`    | `10s`                    | Read timeout                                   |
| `cache.max-size`             | `500`                    | Max cached entries                             |
| `cache.ttl`                  | `300s`                   | Cache time-to-live                             |
| `search-history.max-records` | `50`                     | Max stored search records                      |

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

| Pattern                | Where                          | Why                                                               |
| ---------------------- | ------------------------------ | ----------------------------------------------------------------- |
| **Ports and Adapters** | Entire architecture            | Dependency inversion, testability, swappable infrastructure       |
| **Strategy**           | Language resolution            | Open/Closed principle — multiple algorithms, zero service changes |
| **Adapter**            | GitHub client, JPA persistence | Isolate external dependencies behind domain interfaces            |
| **Circuit Breaker**    | GitHub + database adapters     | Graceful degradation, prevent cascading failures                  |
| **Builder**            | WebClient configuration        | Fluent, readable HTTP client setup                                |
