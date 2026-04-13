# ADR-004: Caffeine Cache for Rate Limit Mitigation

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

The GitHub REST API enforces rate limits:

- **Unauthenticated:** 60 requests/hour
- **Authenticated (token):** 5,000 requests/hour

Each profile lookup makes **2 API calls** (user + repos). Without caching, we can serve only ~30 unique users per hour, and repeated lookups of the same user waste quota.

The challenge states: "Implement a solution that would avoid hitting GitHub rate limits."

## Decision

Use **Caffeine** as a local in-process cache with **Spring's `@Cacheable` abstraction**.

### Configuration

| Parameter          | Value        | Rationale                                                                                        |
| ------------------ | ------------ | ------------------------------------------------------------------------------------------------ |
| `maximumSize`      | 500 entries  | Covers high-traffic users without excessive memory                                               |
| `expireAfterWrite` | 300s (5 min) | GitHub profile changes are infrequent; 5 min is a good balance between freshness and API savings |

### Cache Keys

| Cache Name        | Key        | Cached Value                |
| ----------------- | ---------- | --------------------------- |
| `github-profiles` | `username` | `User` domain object        |
| `github-repos`    | `username` | `List<Repo>` domain objects |

Applied at the adapter layer:

```java
@Cacheable(value = Constants.CACHE_PROFILES, key = "#username")
public User fetchUser(String username) { ... }
```

## Rationale: Why Caffeine over Alternatives

| Option                                  | Verdict                                                                                    |
| --------------------------------------- | ------------------------------------------------------------------------------------------ |
| **No cache**                            | Hits rate limit after ~30 lookups — unacceptable                                           |
| **Spring default (ConcurrentHashMap)**  | No eviction, no TTL — memory leak                                                          |
| **Caffeine**                            | High-performance, near-optimal eviction (W-TinyLFU), built-in TTL, Spring Boot auto-config |
| **Redis**                               | Overkill for a single-instance app; adds infrastructure dependency                         |
| **EhCache**                             | Heavier, XML config, less performant than Caffeine for local caching                       |
| **GitHub conditional requests (ETags)** | Good complement but still counts toward rate limit                                         |

### Why TTL-based expiration?

- `expireAfterWrite` ensures stale data is evicted even if frequently accessed
- Alternative `expireAfterAccess` would keep "hot" entries indefinitely — risky for data freshness
- 5 minutes is short enough that profile changes (new repos, updated bio) appear within a reasonable window

### Cache placement in the architecture

The cache is applied at the **adapter layer** (not the service layer) because:

1. It's an infrastructure concern — the service shouldn't know whether data is cached
2. It pairs naturally with `@CircuitBreaker` and `@Retry` on the same methods
3. Spring evaluates `@Cacheable` **before** other annotations — a cache hit skips the circuit breaker entirely

## Consequences

- **Positive:** Dramatically reduces API calls — 100 lookups for "octocat" = 2 API calls (first hit only)
- **Positive:** Improves response time — cached responses return in <1ms vs. 200-500ms for API calls
- **Positive:** Acts as a resilience buffer — if GitHub goes down, cached data is still served for up to 5 minutes
- **Positive:** Configuration is externalised — TTL and max size tunable via `application.yml`
- **Negative:** Stale data for up to 5 minutes — acceptable for this use case
- **Negative:** Local-only cache — not shared across instances (would need Redis for multi-instance)
