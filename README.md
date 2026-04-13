# Profile Insights API

A Spring Boot 3 application that consumes public source-code hosting APIs (GitHub, GitLab, Bitbucket) and exposes clean REST endpoints for user profile insights, repository listings, and language analytics — with JPA-backed search history.

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

## Getting Started

### Prerequisites

- **Java 21** (e.g., [Eclipse Temurin](https://adoptium.net/))
- **Git**
- **Docker** (optional, for containerised run)

### 1. Clone the Repository

```bash
git clone https://github.com/Miniza/github-insights-api.git
cd vodacom-github
```

### 2. Run the Application

#### Option A — Docker (recommended)

```bash
docker build -t repo-profile .
docker run -p 8080:8080 repo-profile
```

#### Option B — Maven Wrapper

> **Note:** Corporate environments may block script execution (e.g. `Permission denied` on `./mvnw` or PowerShell execution policy restrictions). If you encounter this, use **Docker** (Option A) or **run directly from your IDE** (Option C) instead.

**Linux / macOS:**

```bash
chmod +x ./mvnw
./mvnw spring-boot:run
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

#### Option C — Run from IDE

Open the project in **VS Code** or **IntelliJ IDEA** and run `RepoProfileApplication.java` directly:

- **IntelliJ IDEA:** Open the file and click the green ▶ play button next to the `main` method, or right-click → _Run 'RepoProfileApplication'_.
- **VS Code:** With the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed, open the file and click the **Run** link above `main`, or press `F5`.

### 3. Verify It's Running

Open your browser or use `curl`:

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

curl http://localhost:8080/api/v1/profiles/octocat
```

Swagger UI is available at: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 4. Run Tests

```bash
./mvnw test
```

### 5. Build a Production JAR

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

## Running Tests

### Run all tests

```bash
./mvnw test
```

**Windows (PowerShell)** — if `mvnw.cmd` is not present, use the wrapper jar directly:

```powershell
cd "path\to\vodacom-github"
$projDir = (Get-Location).Path
java "-Dmaven.multiModuleProjectDirectory=$projDir" -cp .mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test
```

### Run a specific test class

```bash
./mvnw test -Dtest=ProfileServiceTest
```

### Run tests for a specific package

```bash
./mvnw test -Dtest="za.vodacom.repoprofile.domain.strategy.*"
```

### Run with verbose output

```bash
./mvnw test -Dsurefire.useFile=false
```

### Test Coverage

The test suite covers the following layers:

| Test Class                   | Layer               | What it tests                                                                                             |
| ---------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------- |
| `ProfileServiceTest`         | Application Service | Profile/repo fetching, search history, event publishing                                                   |
| `ProfileControllerTest`      | REST Controller     | Endpoint routing, validation, error responses (`@WebMvcTest`)                                             |
| `GitHubWebClientAdapterTest` | Adapter             | WebClient mocking, user/repo mapping, error handling                                                      |
| `ProfileMapperTest`          | Mapper              | Domain→DTO mapping, timezone conversion, summary building                                                 |
| `ByRepoCountStrategyTest`    | Domain Strategy     | Language ranking by repository count                                                                      |
| `ByRepoSizeStrategyTest`     | Domain Strategy     | Language ranking by total repo size                                                                       |
| `ApplicationDtoTest`         | DTOs                | Record creation, equality, factory methods                                                                |
| `PagedResponseTest`          | DTOs                | Pagination logic, edge cases, type safety                                                                 |
| `DomainModelTest`            | Domain Models       | Record creation, equality for `User`, `Repo`, `SearchRecord`                                              |
| `CustomExceptionTest`        | Exceptions          | Exception construction and message propagation                                                            |
| `GlobalExceptionHandlerTest` | Exception Handler   | HTTP status mapping for all handled exception types                                                       |
| `ProfileIntegrationTest`     | Integration         | Full lifecycle: context loading, end-to-end requests via MockWebServer, async persistence, error handling |

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

| ADR                                                   | Decision                                                |
| ----------------------------------------------------- | ------------------------------------------------------- |
| [ADR-001](adr/ADR-001-hexagonal-architecture.md)      | Hexagonal Architecture (Ports and Adapters)             |
| [ADR-002](adr/ADR-002-webclient-over-resttemplate.md) | WebClient over RestTemplate                             |
| [ADR-003](adr/ADR-003-resilience4j-patterns.md)       | Resilience4j for circuit breaking, retry, rate limiting |
| [ADR-004](adr/ADR-004-caffeine-cache.md)              | Caffeine cache for rate limit mitigation                |
| [ADR-005](adr/ADR-005-strategy-pattern-language.md)   | Strategy pattern for language resolution                |
| [ADR-006](adr/ADR-006-structured-json-logging.md)     | Structured JSON logging with correlation IDs            |
| [ADR-007](adr/ADR-007-docker-containerisation.md)     | Docker multi-stage build                                |
| [ADR-008](adr/ADR-008-h2-database.md)                 | H2 in-memory database                                   |
| [ADR-009](adr/ADR-009-async-search-history.md)        | Async search history via domain events                  |
| [ADR-010](adr/ADR-010-scaling-strategy.md)            | Scaling strategy for 1k+ requests per minute            |

## Design Patterns Used

| Pattern                | Where                          | Why                                                                |
| ---------------------- | ------------------------------ | ------------------------------------------------------------------ |
| **Ports and Adapters** | Entire architecture            | Dependency inversion, testability, swappable infrastructure        |
| **Strategy**           | Language resolution            | Open/Closed principle — multiple algorithms, configurable via YAML |
| **Adapter**            | GitHub, GitLab, Bitbucket, JPA | Isolate external dependencies behind domain interfaces             |
| **Circuit Breaker**    | GitHub + database adapters     | Graceful degradation, prevent cascading failures                   |
| **Factory/Resolver**   | SourceCodeClientResolver       | Selects adapter by provider name at runtime                        |
| **Builder**            | WebClient configuration        | Fluent, readable HTTP client setup                                 |
