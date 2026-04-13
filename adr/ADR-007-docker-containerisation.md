# ADR-007: Docker Multi-Stage Build for Containerisation

**Status:** Accepted  
**Date:** 2026-04-13  
**Deciders:** Miniza

## Context

The application needs to be built and run reliably across environments. The development machine has:

- JDK 21 installed
- No Maven installed system-wide
- Corporate proxy restrictions preventing Maven wrapper downloads

The challenge states the app should "compile and start" — we need a reproducible build mechanism.

## Decision

Use a **multi-stage Docker build** with:

- **Build stage:** `maven:3.9.9-eclipse-temurin-21-alpine` — includes Maven + JDK
- **Run stage:** `eclipse-temurin:21-jre-alpine` — JRE only, minimal attack surface

### Dockerfile

```dockerfile
# Stage 1 — Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B      # Cache dependencies
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2 — Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Rationale

### Why Docker over running locally?

| Approach               | Issue                                                           |
| ---------------------- | --------------------------------------------------------------- |
| `mvn spring-boot:run`  | No Maven installed; wrapper download blocked by corporate proxy |
| Install Maven manually | Works but not reproducible across machines                      |
| Docker                 | Self-contained — JDK, Maven, dependencies all in the image      |

### Why multi-stage?

| Single-stage                              | Multi-stage                      |
| ----------------------------------------- | -------------------------------- |
| Final image includes Maven + JDK (~800MB) | Final image is JRE only (~200MB) |
| Build tools in production — security risk | Minimal attack surface           |
| Source code in image                      | Only compiled JAR                |

### Why Alpine base?

- `eclipse-temurin:21-jre-alpine` is ~200MB vs ~400MB for the Debian-based variant
- Smaller image = faster pull, less storage, smaller attack surface
- Alpine's `musl` libc is compatible with Spring Boot 3

### Why `dependency:go-offline` as a separate layer?

Docker caches layers. By copying `pom.xml` first and resolving dependencies, subsequent builds only re-download if `pom.xml` changes. Source code changes trigger only the compilation layer — reducing build times from ~3 minutes to ~30 seconds.

### Why `SPRING_PROFILES_ACTIVE=prod`?

The container runs with production profile, activating:

- Structured JSON logging (Logstash encoder)
- No H2 console exposure
- Production-appropriate logging levels

## Consequences

- **Positive:** Reproducible builds — works on any machine with Docker
- **Positive:** Small production image (~200MB)
- **Positive:** Build artefacts cached — fast rebuilds
- **Positive:** No build tools in production — reduced attack surface
- **Negative:** Requires Docker Desktop (available on the development machine)
- **Negative:** Initial build downloads all Maven dependencies (~3 min first time)
