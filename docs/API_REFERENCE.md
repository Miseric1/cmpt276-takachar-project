# API Reference — Dashboard, Knowledge Base, Ticketing

All endpoints added in Iteration 2 are JSON REST APIs under `/api`. This
reference is written so the frontend can integrate without reading backend code.

## Conventions

- **Base path:** `/api`
- **Auth:** session-based (the existing Spring Security form login). Sign in via
  the existing `/login` flow; the session cookie authorizes API calls.
  - Public endpoints need no authentication.
  - Admin endpoints require the `ADMIN` role. An unauthenticated call to a
    protected `/api/**` endpoint returns `401`; an authenticated call without the
    required role returns `403`.
  - CSRF is disabled for `/api/**` (stateless JSON), so no CSRF token is needed.
- **Pagination:** any list endpoint accepts `page` (0-based), `size` (max 100),
  and `sort` (`field,asc|desc`, repeatable). Responses use the `PageResponse`
  envelope below.
- **Errors:** every failure returns the `ApiError` envelope below.
- **Publication status enum:** `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `HIDDEN`,
  `ARCHIVED`, `EXPIRED`. Only `PUBLISHED` content is visible to the public.

### `PageResponse<T>`
```json
{
  "content": [ /* T[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "hasNext": true,
  "hasPrevious": false
}
```

### `ApiError`
```json
{
  "timestamp": "2026-07-19T22:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields.",
  "path": "/api/knowledge",
  "fieldErrors": [ { "field": "title", "message": "Title is required" } ]
}
```
Status codes used: `400` validation/bad input, `401` unauthenticated, `403`
forbidden, `404` not found, `409` duplicate/invalid state, `500` unexpected.

---

## Knowledge Base — `/api/knowledge`

This is the platform's single self-service content type (FAQ-style short answers
and longer guides both live here as articles).

### Public
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/knowledge` | Paginated list/search of **published** articles |
| GET | `/api/knowledge/{id}` | Published article detail (increments view count) |
| POST | `/api/knowledge/{id}/helpful` | "Helpful" vote → `204` |
| POST | `/api/knowledge/{id}/not-helpful` | "Not helpful" vote → `204` |

Query params for `GET /api/knowledge`: `keyword` (matches title/summary/body),
`category`, `tag`, plus pagination. Default sort `updatedAt`.

### Admin (`ADMIN` role)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/knowledge/admin` | Search all statuses; extra `author`, `status` filters |
| GET | `/api/knowledge/admin/{id}` | Detail for any status |
| GET | `/api/knowledge/{id}/versions` | Version history |
| GET | `/api/knowledge/{id}/versions/{n}` | A single revision |
| POST | `/api/knowledge` | Create → `201` with `KnowledgeResponse` |
| PUT | `/api/knowledge/{id}` | Update (snapshots version, recomputes reading time) |
| PATCH | `/api/knowledge/{id}/status?status=PUBLISHED` | Change status |
| DELETE | `/api/knowledge/{id}` | Delete → `204` |

**`KnowledgeRequest`:**
```json
{
  "title": "Setting up your device",
  "summary": "A quick start guide.",
  "body": "Step 1 ... Step 2 ...",
  "category": "Troubleshooting",
  "tags": ["network", "setup"],
  "relatedArticleIds": [12, 15],
  "author": "author@takachar.com",
  "status": "PUBLISHED"
}
```
`title`, `body`, `category` are required. `estimatedReadingTimeMinutes` is
computed server-side from the body (≈200 words/minute) and never accepted from
the client. `relatedArticleIds` self/unknown ids are ignored.

**`KnowledgeResponse`:** `id, title, summary, body, category {id,name,description},
tags[], relatedArticles[{id,title}], author, contributors[], status, createdAt,
updatedAt, publishedAt, version, estimatedReadingTimeMinutes, viewCount,
helpfulCount, notHelpfulCount, lastModifiedBy`.

---

## Categories & Tags

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/categories` | public | List all categories |
| GET | `/api/categories/{id}` | public | One category |
| POST | `/api/categories` | admin | Create `{name, description}` → `201` |
| PUT | `/api/categories/{id}` | admin | Update |
| DELETE | `/api/categories/{id}` | admin | Delete |
| GET | `/api/tags` | public | List all tags |

Tags are created implicitly when content references them, so there is no tag
write API.

---

## Dashboard — `/api/dashboard` (all `ADMIN`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard` | Full summary: overview + all stat blocks + recent activity |
| GET | `/api/dashboard/overview` | System-wide summary cards only |
| GET | `/api/dashboard/tickets` | Live ticket counts, SLA health, rates, timings, and charts |
| GET | `/api/dashboard/feedback` | Feedback statistics + submissions trend chart |
| GET | `/api/dashboard/knowledge` | Knowledge statistics + leaderboards |
| GET | `/api/dashboard/activity` | Paginated recent-activity feed |

All numbers are computed by the backend; chart series are returned ready to plot
(`{ key, label, points: [{ label, value }] }`). `overview` fields include ticket
counts, feedback totals, article counts, knowledge views, and customer/staff
counts. See `dto/dashboard/*` for exact field lists.

---

## Diagnostic tree — `/api/tree`

PR #17's UUID node graph is the authoritative diagnostic configuration.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/tree` | signed in | Read the complete flat node map and root UUID |
| PUT | `/api/tree` | admin | Replace the complete diagnostic tree |

Question options route to another question or a terminal `resolution` node.
Tree replacement does not delete session history because active sessions store
the current node UUID without a database foreign key.

Resolution nodes accept an optional `knowledgeArticleId`. The referenced FAQ
must exist and be published; the value is persisted and returned in the tree:

```json
{
  "type": "resolution",
  "text": "Restart the unit and confirm the indicator returns to green.",
  "knowledgeArticleId": 12,
  "options": []
}
```

## Guided diagnostic sessions — `/api/diagnostics`

All diagnostic endpoints require a signed-in session. Session access is limited
to its customer and admins.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/sessions` | signed in | Start at the configured root question → `201` |
| GET | `/sessions/{uuid}` | owner/admin | Current question, suggestion, and trail |
| POST | `/sessions/{uuid}/answers` | owner/admin | Answer current question and advance |
| POST | `/sessions/{uuid}/resolution` | owner/admin | Confirm `{resolved:true|false}` |
| GET | `/suggestions?query=` | signed in | Top five published FAQ matches |

Answer request:

```json
{
  "questionId": "d7cbbf6e-6bb8-4eef-b4f0-04b73cfac531",
  "optionId": "551eed75-a28c-4fa0-ad56-69320dcb11a1",
  "answerText": "Optional detail"
}
```

Resolution responses include `suggestedResolution` and, when configured on the
resolution node, `suggestedArticle`. The status is one of `IN_PROGRESS`, `SOLUTION_SUGGESTED`,
`READY_FOR_TICKET`, `RESOLVED_WITH_FAQ`, or `ESCALATED`.

## Tickets — `/api/tickets`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | signed in | Admin: all/filter; customer: own only |
| POST | `/` | signed in | Manual or diagnostic ticket → `201` |
| GET | `/{id}` | owner/admin | Full ticket, trail, attachments, timeline |
| PATCH | `/{id}/assignment` | admin | Department, assignee, SPOC, priority, target |
| PATCH | `/{id}/status` | owner/admin | Admin workflow; customer may close resolved own ticket |
| POST | `/{id}/timeline` | owner/admin | Add an auditable note |
| POST | `/{id}/attachments` | owner/admin | Multipart image/video upload → `201` |
| GET | `/{id}/attachments/{attachmentId}` | owner/admin | Download attachment |
| DELETE | `/{id}/attachments/{attachmentId}` | owner/admin | Delete attachment and audit it |

List filters: `keyword`, `status`, `priority`, `project`, `department`, `spoc`,
`createdFrom`, `createdTo`, plus standard pagination and sorting.
The default queue order is `targetResolutionAt,asc`, which places tickets with
the nearest SLA target first. New targets are calculated from priority:
`URGENT` 1 business day, `HIGH` 2 business days, `MEDIUM` 3 business days,
and `LOW` 5 business days. Business days are Monday-Friday; weekends are
skipped, but statutory and company holidays require a separate calendar.
Safety/emergency wording is automatically promoted to `URGENT`, while clear
service-outage wording is promoted to `HIGH`. Automatic triage never lowers a
priority supplied by the caller.

Create request:

```json
{
  "subject": "Unit will not power on",
  "description": "The indicator stays dark after reconnecting power.",
  "project": "Pilot A",
  "customerEmail": "customer@example.com",
  "priority": "HIGH",
  "spocEmail": "spoc@takachar.com",
  "diagnosticSessionId": "31af18b3-fc14-43b0-b6d7-da824b90c392",
  "suggestedArticleId": 12
}
```

`customerEmail` is honored only for admin-created tickets. A diagnostic session
and direct `suggestedArticleId` are optional; a session's own suggestion wins.
Ticket status values are `OPEN`, `IN_PROGRESS`, `WAITING_FOR_CUSTOMER`,
`WAITING_FOR_LOGISTICS`, `RESOLVED`, and `CLOSED`. Health is returned as
`GREEN`, `YELLOW`, or `RED`.

## Feedback sentiment

Feedback and complaints share the same record. The `type` field is `FEEDBACK`
by default or `COMPLAINT`. Admins can filter and sort the unified list:

```text
GET /api/feedback?type=COMPLAINT&sortBy=type&direction=ASC
```

Supported sort fields are `type`, `status`, `category`, `project`, `account`,
`createdAt`, and `updatedAt`. Listing, updating, and deleting submissions are
admin-only; authenticated customers may create them.

Responses also include `sentiment`, `sentimentConfidence`,
`sentimentModel`, and `sentimentAnalyzedAt`. New feedback is analysed on
submission. Admins can retry with `POST /api/feedback/{id}/sentiment`.

## Customer accounts — `/api/admin/customers`

Public registration is disabled. An administrator acting as the SPOC manages
customer credentials:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/customers` | Create a customer from `{email,password}` → `201` |
| GET | `/api/admin/customers` | List customer accounts without password data |

Passwords must be 8–72 characters, are stored as BCrypt hashes, and are never
returned. Duplicate emails return `409`.

## Existing page endpoints

The Thymeleaf pages `/`, `/login`, `/admin/home`, `/customer/home`, and
`/customer/feedback` remain available. `/register` is intentionally disabled.
