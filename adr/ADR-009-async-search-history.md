# ADR-009: Async Search History Persistence via Domain Events

## Status

Accepted

## Context

Every profile and repository lookup persists a `search_history` record so users can review recent searches via `/api/v1/searches`. The persistence was originally performed **synchronously** inside `ProfileService`, adding database I/O latency to every API response — even though the caller never sees the persisted record in the response payload.

The options considered were:

| Approach                       | Latency Impact           | Infrastructure        | Durability           | Complexity |
| ------------------------------ | ------------------------ | --------------------- | -------------------- | ---------- |
| Synchronous (status quo)       | High — blocks response   | None                  | Same as DB           | Low        |
| `@Async` + Spring Events       | None — fires and forgets | None — in-process     | Same as DB           | Low        |
| Message Queue (RabbitMQ/Kafka) | None                     | New broker dependency | Survives app crashes | High       |

## Decision

Use **Spring Application Events** with **`@Async`** processing:

1. `ProfileService` publishes a `SearchPerformedEvent` (domain event record) via `ApplicationEventPublisher`.
2. `SearchHistoryEventListener` (in the persistence adapter layer) handles the event on a separate thread, calling `SearchHistoryRepositoryPort.save()` and `pruneOldest()`.
3. `@EnableAsync` is activated via `AsyncConfig`.

### Why not a message queue?

- The database is H2 in-memory. Both the queue and the database would be lost on a crash — durable messaging adds no real value here.
- A broker introduces operational overhead (deployment, monitoring, dead-letter queues) disproportionate to the problem size.
- If the application later moves to PostgreSQL, adopting a transactional outbox or a lightweight queue can be done incrementally.

## Consequences

### Positive

- **Lower response latency** — the HTTP response returns immediately without waiting for the database write.
- **Clean separation** — the service layer has no direct dependency on the persistence adapter for writes; it publishes a domain event.
- **Hexagonal alignment** — the event listener sits in the adapter layer, keeping the application service infrastructure-free.
- **Graceful failure** — if the async save fails, the API response is unaffected. A warning is logged.

### Negative

- **Slight ordering uncertainty** — under heavy load, search history order may not perfectly reflect request arrival order.
- **Test complexity** — async execution requires `Awaitility` or similar utilities to assert persistence in integration tests.
- **No retry on failure** — if the async save throws, the record is lost. Acceptable for analytics-grade data on an in-memory database.

## File Changes

| File                                                   | Change                                                                              |
| ------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| `domain/event/SearchPerformedEvent.java`               | New domain event record                                                             |
| `adapters/persistence/SearchHistoryEventListener.java` | New `@Async @EventListener` handler                                                 |
| `config/AsyncConfig.java`                              | New `@EnableAsync` configuration                                                    |
| `application/service/ProfileService.java`              | Replaced direct `save()`/`pruneOldest()` calls with `eventPublisher.publishEvent()` |
