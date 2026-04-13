# ADR-006: Structured JSON Logging with Correlation IDs

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

Production systems need logs that are:

- **Machine-parseable** — for log aggregation tools (ELK, Splunk, Datadog)
- **Traceable** — every request should be traceable end-to-end
- **Rich** — include HTTP method, URI, status code, duration, client IP
- **Profile-aware** — human-readable in development, JSON in production

## Decision

Use **Logstash Logback Encoder** with **profile-based Logback configuration** and a custom **`RequestLoggingFilter`** that adds correlation IDs and request/response context via MDC.

### Logging Profiles

| Profile         | Format                 | Target                   |
| --------------- | ---------------------- | ------------------------ |
| `dev` (default) | Human-readable console | Local development        |
| `prod`          | Structured JSON        | Docker / log aggregation |

### MDC Fields (added per request)

| Field           | Source                          | Example                       |
| --------------- | ------------------------------- | ----------------------------- |
| `correlationId` | Generated UUID per request      | `a1b2c3d4-...`                |
| `method`        | `HttpServletRequest`            | `GET`                         |
| `uri`           | `HttpServletRequest`            | `/api/github/profile/octocat` |
| `queryString`   | `HttpServletRequest`            | `strategy=bySize`             |
| `clientIp`      | `HttpServletRequest`            | `172.17.0.1`                  |
| `httpStatus`    | `HttpServletResponse`           | `200`                         |
| `durationMs`    | Calculated                      | `342`                         |
| `requestBody`   | `ContentCachingRequestWrapper`  | `(body if present)`           |
| `responseBody`  | `ContentCachingResponseWrapper` | `(first 1000 chars)`          |

### Sample Production Log Entry

```json
{
  "@timestamp": "2026-04-13T10:15:30.123Z",
  "level": "INFO",
  "logger_name": "za.vodacom.repoprofile.config.RequestLoggingFilter",
  "message": "HTTP GET /api/github/profile/octocat - 200 (342ms)",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "method": "GET",
  "uri": "/api/github/profile/octocat",
  "httpStatus": "200",
  "durationMs": "342",
  "clientIp": "172.17.0.1",
  "service": "repo-profile",
  "port": "8080"
}
```

## Rationale

### Why Logstash Logback Encoder over alternatives?

| Option                          | Verdict                                                 |
| ------------------------------- | ------------------------------------------------------- |
| **Default Spring Boot logging** | Plain text — not machine-parseable                      |
| **Log4j2 JSON Layout**          | Requires switching from Logback (Spring Boot default)   |
| **Logstash Logback Encoder**    | Drop-in for Logback, well-maintained, ELK-native format |
| **Custom JSON formatter**       | Reinventing the wheel                                   |

### Why MDC over method parameters?

- MDC (Mapped Diagnostic Context) is **thread-scoped** — set once in the filter, available in every log statement downstream
- No need to pass `correlationId` through every service method
- Automatically included in JSON output by Logstash encoder

### Why a custom filter over Spring's `CommonsRequestLoggingFilter`?

- `CommonsRequestLoggingFilter` doesn't support correlation IDs
- No control over response body capture
- No duration measurement
- Can't populate MDC fields

### Why correlation IDs?

When debugging production issues, you need to trace a single user request across:

- The REST controller
- The service layer
- The GitHub API call
- The database persistence

A UUID in every log line ties them together without distributed tracing infrastructure.

## Consequences

- **Positive:** Logs are immediately usable with ELK/Splunk/Datadog — zero transformation needed
- **Positive:** Every request is traceable via `correlationId`
- **Positive:** Development logs remain human-readable (`dev` profile)
- **Positive:** Response body truncated to 1000 chars — prevents log bloat
- **Negative:** `ContentCachingResponseWrapper` buffers the response body — minor memory overhead
- **Negative:** Adds `logstash-logback-encoder` dependency (~200KB)
