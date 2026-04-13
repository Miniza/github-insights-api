# ADR-010: Scaling Strategy for 1k+ Requests per Minute

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

The application currently runs as a single instance with an in-memory H2 database and a local Caffeine cache. This is suitable for development and low-traffic deployments, but we need a clear path to handle **1,000+ requests per minute** in production without re-architecting the codebase.

The hexagonal architecture (ADR-001) was chosen specifically to make infrastructure changes configuration-only, without touching business logic.

## Decision

Scale the application through the following layers, each independently deployable:

### 1. Shared Cache — Replace Caffeine with Redis

| Aspect  | Current                                           | Scaled                            |
| ------- | ------------------------------------------------- | --------------------------------- |
| Cache   | Caffeine (in-process)                             | Redis (shared, distributed)       |
| Benefit | Zero latency                                      | Cache shared across all instances |
| Change  | Swap `CaffeineCacheManager` → `RedisCacheManager` | Config + dependency only          |

Redis ensures a profile fetched by instance A is immediately available to instance B, eliminating redundant GitHub API calls across the cluster.

### 2. Horizontal Scaling — Multiple Instances Behind a Load Balancer

```
                 ┌──────────────┐
    Clients ───► │ Load Balancer │
                 └──┬───┬───┬───┘
                    │   │   │
                 ┌──▼┐ ┌▼──┐ ┌▼──┐
                 │ A │ │ B │ │ C │   ← Stateless app instances
                 └─┬─┘ └─┬─┘ └─┬─┘
                   │     │     │
              ┌────▼─────▼─────▼────┐
              │   Redis (cache)      │
              │   PostgreSQL (DB)    │
              └──────────────────────┘
```

The application is already stateless — no server-side sessions, no in-process state beyond cache. Scaling horizontally requires only deploying additional instances behind an NGINX or cloud load balancer.

### 3. Database — Replace H2 with PostgreSQL

As documented in ADR-008, the persistence layer uses Spring Data JPA with Flyway migrations. Switching to PostgreSQL requires **zero code changes** — only `application.yml` configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db-host:5432/repoprofile
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

PostgreSQL handles concurrent writes from multiple instances and supports connection pooling via HikariCP (already included by Spring Boot).

### 4. Rate Limiter — Distribute Across Instances

The current Resilience4j rate limiter is instance-local (10 calls/60s per instance). At scale:

- Use **Redis-backed rate limiting** (e.g., `resilience4j-ratelimiter` with a Redis semaphore or a dedicated library like `bucket4j-redis`) to enforce a global limit across all instances.
- Alternatively, use an **API gateway** (e.g., Spring Cloud Gateway, Kong) to enforce rate limits before requests reach the application.

### 5. Reactive Stack (Optional, for Extreme Throughput)

The current WebClient calls use `.block()` — each request holds a thread. For 1k+ req/min this is fine (Spring MVC's default thread pool handles ~200 concurrent requests), but for 10k+ req/min:

- Replace `@RestController` with reactive handlers
- Remove `.block()` calls in the GitHub adapter
- Use `spring-boot-starter-webflux` as the server (not just as a client)

This is a larger change and only necessary at very high scale.

### 6. Additional Optimisations

| Optimisation                                     | Impact                                          | Effort                                |
| ------------------------------------------------ | ----------------------------------------------- | ------------------------------------- |
| **CDN** (e.g., CloudFront) for profile responses | Offloads repeated lookups entirely              | Low — add cache headers               |
| **Connection pooling** for GitHub API            | Reuse TCP connections, reduce latency           | Already handled by Netty in WebClient |
| **Async persistence**                            | Already implemented via domain events (ADR-009) | None — already done                   |
| **Pre-warm cache** for popular users             | Eliminates cold-start latency spikes            | Low — scheduled task                  |

## Consequences

### Positive

- **No code changes** required for steps 1–3 (cache, scaling, database) — configuration only
- Hexagonal architecture isolates every infrastructure swap behind a port
- Each scaling layer is independently deployable and testable
- The current Caffeine + H2 setup remains ideal for development and CI

### Negative

- Redis and PostgreSQL introduce operational complexity (backups, monitoring, failover)
- Distributed rate limiting adds a network hop per request
- Full reactive migration (step 5) would require significant refactoring
