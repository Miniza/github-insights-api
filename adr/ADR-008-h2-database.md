# ADR-008: H2 In-Memory Database for Search History

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Development Team

## Context

The challenge requires: "Save every successful username search into an H2 (in-memory) database with a timestamp and a short summary. Keep only the last 50 searches."

H2 is mandated by the requirements. This ADR documents the design decisions around how we use it and acknowledges limitations for a production deployment.

## Decision

Use **H2 in-memory mode** with **Spring Data JPA** and **Flyway** for schema management (`ddl-auto: validate`).

### Schema

```
search_history
├── id          BIGINT (auto-generated)
├── username    VARCHAR
├── summary     VARCHAR (e.g., "octocat – 127 repos – Go")
└── searched_at TIMESTAMP
```

### Pruning Strategy

A custom JPQL query deletes records beyond the configured maximum:

```sql
DELETE FROM SearchHistoryEntity e
WHERE e.id NOT IN (
    SELECT e2.id FROM SearchHistoryEntity e2
    ORDER BY e2.searchedAt DESC
    LIMIT :maxRecords
)
```

This runs after every successful search, keeping exactly the last 50 entries.

### Circuit Breaker Protection

The database adapter is protected by a `database` circuit breaker (see ADR-003). If the database becomes unavailable:

- Profile lookup still succeeds (primary use case unaffected)
- Search history endpoint returns an empty list
- Pruning is silently skipped

## Rationale

### Why H2 and not PostgreSQL?

The challenge explicitly requires H2. For a production deployment, we would migrate to PostgreSQL with Flyway migrations.

### Why Flyway with `ddl-auto: validate`?

- Flyway provides repeatable, version-controlled schema creation via `V1__create_search_history.sql`
- `validate` mode ensures the JPA entity model stays in sync with the actual schema
- Even with H2 in-memory databases, Flyway ensures consistent schema setup and serves as documentation
- The same migration workflow carries over to PostgreSQL with zero changes

### Why `DB_CLOSE_DELAY=-1`?

Prevents H2 from closing the database when the last connection is released, which can happen during connection pool recycling.

### Why prune instead of FIFO queue?

- JPA doesn't natively support fixed-size collections
- A delete-beyond-N approach is simple, uses standard JPQL, and works with any database
- Alternative (circular buffer with modulo ID) adds complexity with no benefit

## Production Migration Path

If this application were deployed to production, the following changes would be required:

| Current (H2)                             | Production (PostgreSQL)                   |
| ---------------------------------------- | ----------------------------------------- |
| `spring.datasource.url: jdbc:h2:mem:...` | `jdbc:postgresql://host:5432/repoprofile` |
| `ddl-auto: validate`                     | `ddl-auto: validate`                      |
| Flyway with versioned SQL scripts        | Flyway with versioned SQL scripts         |
| In-memory (data lost on restart)         | Persistent storage                        |
| H2 console enabled                       | H2 console disabled                       |

This is a configuration-only change — the JPA entities and repositories require zero code modifications (a benefit of the hexagonal architecture).

## Consequences

- **Positive:** Zero-config database — starts with the application, no external setup
- **Positive:** Fast — in-memory operations, no disk I/O
- **Positive:** H2 console available at `/h2-console` for debugging
- **Negative:** Data is lost on every application restart
- **Negative:** Not suitable for multi-instance deployments (each instance has its own database)
- **Positive:** `ddl-auto: validate` catches entity/schema drift at startup
