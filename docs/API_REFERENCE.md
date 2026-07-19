# API Reference — Iteration 2 (Dashboard, FAQ, Knowledge Base)

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
  "path": "/api/faqs",
  "fieldErrors": [ { "field": "question", "message": "Question is required" } ]
}
```
Status codes used: `400` validation/bad input, `401` unauthenticated, `403`
forbidden, `404` not found, `409` duplicate/invalid state, `500` unexpected.

---

## FAQ — `/api/faqs`

### Public
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/faqs` | Paginated list/search of **published** FAQs |
| GET | `/api/faqs/{id}` | Published FAQ detail (increments view count) |
| POST | `/api/faqs/{id}/helpful` | Register a "helpful" vote → `204` |
| POST | `/api/faqs/{id}/not-helpful` | Register a "not helpful" vote → `204` |

Query params for `GET /api/faqs`: `keyword` (matches question/answer),
`category` (name), `tag` (name), plus pagination. Default sort `displayOrder`.

### Admin (`ADMIN` role)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/faqs/admin` | Search across **all** statuses; extra `status` filter |
| GET | `/api/faqs/admin/{id}` | Detail for any status (no view increment) |
| GET | `/api/faqs/{id}/versions` | Full version history (newest first) |
| GET | `/api/faqs/{id}/versions/{n}` | A single historical revision |
| POST | `/api/faqs` | Create an FAQ → `201` with `FaqResponse` |
| PUT | `/api/faqs/{id}` | Update (snapshots prior version, bumps version) |
| PATCH | `/api/faqs/{id}/status?status=PUBLISHED` | Change publication status |
| DELETE | `/api/faqs/{id}` | Delete → `204` |

**`FaqRequest` (create/update body):**
```json
{
  "question": "How do I reset my password?",
  "answer": "Open the account page and choose Reset Password.",
  "category": "Accounts",
  "tags": ["password", "login"],
  "displayOrder": 1,
  "status": "PUBLISHED"
}
```
`question`, `answer`, `category` are required. `category` is resolved by name and
created if it does not exist. `status` optional (defaults to `DRAFT`).

**`FaqResponse`:** `id, question, answer, category {id,name,description}, tags[],
displayOrder, status, createdBy, lastModifiedBy, createdAt, updatedAt,
publishedAt, version, viewCount, helpfulCount, notHelpfulCount`.

---

## Knowledge Base — `/api/knowledge`

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

**`KnowledgeResponse`:** adds `summary, body, relatedArticles[{id,title}],
author, contributors[], estimatedReadingTimeMinutes` to the same analytics/audit
fields as FAQ.

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
| GET | `/api/dashboard/tickets` | Ticket statistics (zeroed until Ticketing ships) |
| GET | `/api/dashboard/feedback` | Feedback statistics + submissions trend chart |
| GET | `/api/dashboard/faq` | FAQ statistics + popularity leaderboards |
| GET | `/api/dashboard/knowledge` | Knowledge statistics + leaderboards |
| GET | `/api/dashboard/activity` | Paginated recent-activity feed |

All numbers are computed by the backend; chart series are returned ready to plot
(`{ key, label, points: [{ label, value }] }`). `overview` fields include ticket
counts, feedback totals, FAQ/article counts, knowledge views, and customer/staff
counts. See `dto/dashboard/*` for exact field lists.

---

## Existing endpoints (unchanged)

`/api/feedback` (CRUD) and the Thymeleaf pages (`/`, `/login`, `/register`,
`/admin/home`, `/customer/home`, `/customer/feedback`) are untouched by this
iteration.
