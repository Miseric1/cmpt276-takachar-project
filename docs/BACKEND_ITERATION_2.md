# Iteration 2 Backend — Architecture & Schema

Backend-only iteration for SupportSync (Takachar). It migrates the database to
Supabase and adds three production-ready modules: **Dashboard**, **FAQ**, and
**Knowledge Base**. It extends the existing Spring Boot app (`com.example.demo`)
without changing its architecture, authentication, or the Thymeleaf pages.

## Stack (unchanged)

Java 17 · Spring Boot 3.2.5 · Spring Data JPA · Spring Security · PostgreSQL
(Supabase in prod, H2 in dev/test) · Docker · Render.

## Layering

```
Controller  (@RestController, thin: validate + delegate + return)
   │
Service     (@Service, all business rules, @Transactional)
   │
Repository  (Spring Data JPA; Specifications for dynamic search)
   │
Database
```

DTOs isolate the API from entities in **both** directions (request records with
Bean Validation, response records). Entities are never serialized to clients.
Mappers (`mapper/`) translate entities → DTOs inside the service transaction.

## Package map (new in this iteration)

| Package | Contents |
|---------|----------|
| `dto` | Shared `ApiError`, `PageResponse`, `Category*`, `Tag*` |
| `dto.faq` / `dto.knowledge` / `dto.dashboard` | Module DTOs (records) |
| `exception` | `GlobalExceptionHandler` + `ResourceNotFound/Duplicate/InvalidState` |
| `mapper` | `FaqMapper`, `KnowledgeMapper` |
| `model` | `Faq`, `FaqVersion`, `KnowledgeArticle`, `KnowledgeArticleVersion`, `Category`, `Tag`, `PublicationStatus` |
| `repository` | Repositories + `*Specifications` for search |
| `service` | `FaqService`, `KnowledgeService`, `CategoryService`, `TagService`, `DashboardService` |
| `service.analytics` | `Feedback/Faq/Knowledge` analytics, `ActivityService`, `TicketAnalyticsProvider` (+ empty impl) |
| `util` | `TrendUtils` (time-bucketing for charts) |
| `controller` | `FaqController`, `KnowledgeController`, `CategoryController`, `TagController`, `DashboardController` |

## Cross-cutting decisions

- **Central error handling** — `@RestControllerAdvice(annotations =
  RestController.class)` scopes JSON error handling to REST controllers only, so
  the existing Thymeleaf pages keep their view-based error behaviour.
- **Records for DTOs** — immutable, validation-friendly, low boilerplate;
  entities remain JavaBeans matching the existing style.
- **`open-in-view=false`** (dev, test, prod) — lazy associations are only read
  inside service transactions (via mappers), preventing accidental N+1 in the
  view layer.
- **Atomic counters** — view/helpful votes use `@Modifying` update queries, so
  they don't churn `updatedAt` or race.
- **Pagination bounds** — global `max-page-size=100`, default `20`; no endpoint
  returns an unbounded list.

## Data model

Shared vocabulary tables `categories` and `tags` are referenced by both content
types, so categories/tags are never hardcoded and never duplicated.

```
categories (id, name UNIQUE, description, created_at)
tags       (id, name UNIQUE)

faqs (id, question, answer TEXT, category_id → categories,
      display_order, status, created_by, last_modified_by,
      created_at, updated_at, published_at, version,
      view_count, helpful_count, not_helpful_count)
faq_tags (faq_id → faqs, tag_id → tags)                 -- M:N
faq_versions (id, faq_id, version_number, question, answer,
      category_name, status, edited_by, edited_at)      -- history snapshots

knowledge_articles (id, title, summary, body TEXT, category_id → categories,
      author, status, created_at, updated_at, published_at, version,
      estimated_reading_time_minutes, last_modified_by,
      view_count, helpful_count, not_helpful_count)
article_tags (article_id, tag_id)                       -- M:N
article_related (article_id, related_article_id)        -- M:N self-reference
article_contributors (article_id, contributor)          -- element collection
article_versions (id, article_id, version_number, title, summary, body,
      category_name, status, edited_by, edited_at)       -- history snapshots
```

Indexes: unique on `categories.name` and `tags.name`; `faqs`/`knowledge_articles`
indexed on `status`, `category_id`, and `created_at`; version tables indexed on
their parent id. Schema is created automatically by Hibernate (`ddl-auto=update`).

## Publication workflow

`PublicationStatus` encodes the allowed state transitions; the services reject
illegal transitions with `409 InvalidStateException`. Publishing stamps
`published_at` once. Every content edit writes an immutable version snapshot and
increments `version`, so history is never overwritten.

## Ticketing integration seam

Tickets are not built this iteration. `TicketAnalyticsProvider` is the seam: the
`EmptyTicketAnalyticsProvider` returns zeroed stats today, and a future ticket
module supplies a `@Primary` implementation to light up ticket analytics with no
change to the dashboard API or the frontend. FAQ/Knowledge entities already carry
the fields (ids, view/helpful counts) a ticketing module would reference.

## Testing

`mvn test` runs 22 tests (JDK 17 or 21): the original feedback suite plus new
`@SpringBootTest` integration tests for `FaqService`, `KnowledgeService`, and
`DashboardService` (versioning, publication workflow, search, reading-time,
related articles, analytics). Tests use an isolated in-memory H2
(`src/test/resources/application.properties`) and roll back per test.

> Build/test note: the project targets Java 17. The included Docker image builds
> on Temurin 17. If building locally on a newer JDK, use JDK 17 or 21 (Spring
> Boot 3.2.5 supports up to 21).

See `SUPABASE_SETUP.md` for configuration/deployment and `API_REFERENCE.md` for
the endpoint contract.
