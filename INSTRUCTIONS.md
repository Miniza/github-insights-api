# Spring Boot Take-Home / Live Coding Challenge

## GitHub Profile Insights API

**Goal**

Build a small production-ready Spring Boot 3 application that consumes the public GitHub REST API and exposes clean REST endpoints with JPA persistence.

### Functional Requirements

Design and implement a REST API that allows one to request:

- GitHub user profile details (name, bio, avatar, public repos count, followers, etc.)
- The user's public repositories sorted by stargazers_count (descending)
- The programming language the user uses the most (by repo count OR total size (your choice)

Responses as JSON

### Non-Functional Requirements (mandatory)

1. **External API** – Use the real GitHub API (`https://api.github.com`). No authentication token is required for public data.
2. **API Rate Limits** – Implement a solution that would avoid hitting GitHub rate limits
3. **Persistence** – Save every successful username search into an H2 (in-memory) database with a timestamp and a short summary (e.g., “octocat – 127 repos – Go”). Keep only the last 50 searches (oldest are deleted automatically).
4. **Code quality** – Use proper structures for packages, classes, etc. Well defined responsibilities, core principles and best practices
5. **Configuration** – Everything must be configurable via `application.yml` /  `application.properties` duration, etc.).
6. **Robustness and resilience** - Add anything that would improve robustness and resilience

## Other

You are free to adjust pom.xml / build.gradle dependencies as you see fit; add, remove, change tools/libraries/starters/versions, etc.

The code should compile and the application should start with `./mvnw spring-boot:run` and be testable immediately with `curl` or Postman.

Be prepared to walk us through what you did and to answer questions about why you made the decisions you've made.

We may also ask how you would implement additional features that could improve the program.

Good luck — we're excited to see your solution!

# Too easy?

_in order_

- Add implementations to connect to repositories other than GitHub (just a skeleton that shows the design implications should suffice)
- Add Github authentication to get past the API limits
- API resilience (as the client)
- Pagination support for the `/repos` endpoint (use GitHub’s `page` and `per_page` parameters)
- Database migrations
- How would you scale this for 1k reqs/min?

