# ADR-003: Resilience4j for Circuit Breaking, Retry, and Rate Limiting

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Development Team

## Context

The application depends on two external/infrastructure systems:

1. **GitHub REST API** — rate-limited (60 req/hr unauthenticated), subject to outages
2. **H2 Database** — in-memory, generally reliable, but still an I/O dependency

The challenge requires: "Add anything that would improve robustness and resilience."

We need a strategy that:

- Prevents cascading failures when GitHub is down
- Avoids exhausting GitHub's rate limit (HTTP 403)
- Retries transient failures without overwhelming the upstream
- Degrades gracefully when the database is unavailable

## Decision

Use **Resilience4j** with three patterns, applied as annotations on the adapter layer:

### 1. Circuit Breaker

| Instance   | Sliding Window | Failure Threshold | Open Duration | Half-Open Calls |
| ---------- | -------------- | ----------------- | ------------- | --------------- |
| `github`   | 10 calls       | 50%               | 30s           | 3               |
| `database` | 5 calls        | 50%               | 15s           | 2               |

**Why two separate circuit breakers?**

- GitHub outages are external and take longer to resolve → 30s open state
- Database issues (H2 in-memory) are typically transient → 15s open state, smaller window
- Independent circuits prevent a DB issue from blocking GitHub calls and vice versa

**Fallback strategy:**

| Adapter                         | Fallback behaviour                                                                               |
| ------------------------------- | ------------------------------------------------------------------------------------------------ |
| GitHub `fetchUser()`            | Re-throws `NotFoundException` (user doesn't exist) or throws `GitHubApiException` (service down) |
| GitHub `fetchRepositories()`    | Same as above                                                                                    |
| Database `save()`               | Logs error, returns unsaved entity — profile response still served                               |
| Database `findRecentSearches()` | Returns empty list — history endpoint still responds                                             |
| Database `pruneOldest()`        | Silently skips — no user impact                                                                  |

The key principle: **database failures must not prevent the primary use case (fetching GitHub profiles) from working.**

### 2. Retry with Exponential Backoff + Jitter

```yaml
max-attempts: 3
wait-duration: 1s
exponential-backoff-multiplier: 2
randomized-wait-factor: 0.5
```

| Retry | Base delay | With jitter (±50%) |
| ----- | ---------- | ------------------ |
| 1st   | 1s         | 0.5s – 1.5s        |
| 2nd   | 2s         | 1.0s – 3.0s        |
| 3rd   | 4s         | 2.0s – 6.0s        |

**Why jitter?** Prevents the "thundering herd" problem — if GitHub recovers from an outage, hundreds of clients retrying at exactly the same intervals would immediately overwhelm it again. Randomised wait spreads the load.

**Why exponential backoff?** Gives the upstream service progressively more time to recover between attempts.

### 3. Rate Limiter

```yaml
limit-for-period: 10
limit-refresh-period: 60s
```

Caps our application at 10 GitHub API calls per minute, well within the unauthenticated limit of 60/hr. This protects us even under high traffic.

## Rationale: Why Resilience4j over Alternatives

| Option           | Verdict                                                                      |
| ---------------- | ---------------------------------------------------------------------------- |
| **Spring Retry** | Only covers retry — no circuit breaker or rate limiter                       |
| **Hystrix**      | Netflix EOL'd it in 2018, removed from Spring Cloud 2022                     |
| **Resilience4j** | Lightweight, annotation-driven, Spring Boot auto-config, actively maintained |
| **Istio/Envoy**  | Infrastructure-level — overkill for a single service, less testable          |

## Annotation Ordering

Resilience4j applies decorators in this order:

```
Retry → CircuitBreaker → RateLimiter → (actual call)
```

This means:

1. Rate limiter checked first — reject immediately if limit exceeded
2. Circuit breaker checked — reject immediately if circuit is open
3. Actual call made
4. If call fails, retry logic kicks in (which re-enters the circuit breaker)

## Consequences

- **Positive:** The application stays responsive even when GitHub is completely down (503 with clear message vs. hanging)
- **Positive:** Database failures degrade gracefully — users still get profile data
- **Positive:** Jittered backoff prevents cascading failures during recovery
- **Positive:** All thresholds are externalised to `application.yml` — tunable without code changes
- **Negative:** Adds Resilience4j + AOP dependencies
- **Negative:** Fallback methods add code to the adapter layer

## References

- Resilience4j Documentation: [resilience4j.readme.io](https://resilience4j.readme.io/)
- Michael Nygard, _Release It!_ (2018) — Circuit Breaker pattern
- AWS Architecture Blog: "Exponential Backoff and Jitter"
