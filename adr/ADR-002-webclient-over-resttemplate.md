# ADR-002: WebClient over RestTemplate

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Development Team

## Context

The application needs an HTTP client to call the GitHub REST API (`https://api.github.com`). Spring provides two main options:

1. **RestTemplate** — synchronous, blocking, in maintenance mode since Spring 5
2. **WebClient** — non-blocking, reactive, the recommended replacement

## Decision

Use **Spring WebFlux's `WebClient`** as the HTTP client for all GitHub API calls, configured with connection/read timeouts and a custom `User-Agent` header.

## Rationale

| Factor                   | RestTemplate                                     | WebClient                                                          |
| ------------------------ | ------------------------------------------------ | ------------------------------------------------------------------ |
| Status                   | Maintenance mode (no new features)               | Actively developed                                                 |
| Spring Boot 3 support    | Supported but discouraged                        | Recommended                                                        |
| Non-blocking I/O         | No — blocks the calling thread                   | Yes — Netty event loop                                             |
| Timeout configuration    | Per-request via `SimpleClientHttpRequestFactory` | Fluent builder with `HttpClient`                                   |
| Resilience4j integration | Works but requires manual wrapping               | Works natively with `@CircuitBreaker` on blocking `.block()` calls |
| Testability              | MockRestServiceServer                            | MockWebServer / WireMock                                           |
| Future-proofing          | Will eventually be removed                       | Long-term API                                                      |

### Why not go fully reactive?

We use `WebClient` in **blocking mode** (`.block()`) because:

- The rest of the application is servlet-based (Spring MVC, JPA/H2)
- Going fully reactive would require replacing JPA with R2DBC, adding complexity with no proportional benefit for this use case
- The blocking call happens inside the adapter — the port interface remains synchronous, keeping the domain clean

### Configuration

```java
WebClient.builder()
    .baseUrl("https://api.github.com")
    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

Timeouts are externalised to `application.yml`:

- `connect-timeout: 5s`
- `read-timeout: 10s`

## Consequences

- **Positive:** Future-proof — aligned with Spring's direction
- **Positive:** Superior timeout and connection pool configuration via Netty
- **Positive:** Can be upgraded to fully non-blocking if the persistence layer moves to R2DBC
- **Negative:** Adds `spring-boot-starter-webflux` dependency alongside `spring-boot-starter-web`
- **Negative:** `.block()` usage means we don't benefit from non-blocking I/O in this iteration

## References

- Spring Documentation: [WebClient](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- Spring Blog: "RestTemplate is in maintenance mode"
