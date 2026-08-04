# Ticketing Backend

The ticketing backend implements the complaint workflow in the Takachar PRD.
It is API-only; the existing admin feedback UI is unchanged.

## Delivered workflow

1. A signed-in customer starts a persisted diagnostic session.
2. Each selected option advances through PR #17's UUID-based diagnostic tree
   to another question or a terminal resolution.
3. A resolution can carry a published Knowledge Base article, and the customer
   confirms whether the suggested resolution fixed the issue.
4. An unresolved session can be escalated exactly once into a support ticket.
5. The complete question/answer trail and suggested article are attached to
   the ticket automatically.
6. The ticket receives a reference number, priority, target date, SPOC,
   timeline, ownership boundary, and optional image/video attachments.
7. Admins assign and progress the ticket. Customers can view only their own
   tickets and close a ticket only after it is resolved.

The customer diagnostic page now starts the persisted session automatically,
records each UUID option selection, shows the FAQ linked by the admin editor,
and creates a ticket with the complete diagnostic trail when the suggested
resolution does not work.

Manual ticket creation is also supported. For customers, `customerEmail` is
always derived from the authenticated session. An admin may provide
`customerEmail` when logging a ticket on a customer's behalf.

## Main tables

- `diagnostic_nodes`, `diagnostic_options` (authoritative PR #17 tree)
- `diagnostic_sessions`, `diagnostic_answers`
- `support_tickets`
- `ticket_timeline_events`
- `ticket_attachments`

Historical question text and selected answer labels are snapshotted into
`diagnostic_answers`, so replacing the tree later does not rewrite old tickets.
An active session stores its current node UUID without a foreign key, allowing
the admin editor's full-replace save operation to remain authoritative. Ticket
rows use optimistic locking.

The same JPA entities use Supabase PostgreSQL during normal application startup.
H2 is isolated to the explicit `local` profile and the automated test suite; no
tree-specific datasource is used.

## Status workflow

`OPEN` -> `IN_PROGRESS` / `WAITING_FOR_CUSTOMER` /
`WAITING_FOR_LOGISTICS` -> `RESOLVED` -> `CLOSED`.

Admins control assignment and operational states. Customers can perform only
the final `RESOLVED` -> `CLOSED` transition on their own ticket. If an open
ticket is overdue, an admin update that leaves it open must include a progress
note or delay reason.

Timeline health is derived by the API:

- `GREEN`: resolved on or before its target date
- `YELLOW`: open and still inside its target window
- `RED`: overdue, or resolved after its target date

## Priority triage and SLA targets

New tickets receive an automatic target based on priority. The admin queue is
ordered by the nearest target first, so the most time-sensitive work is seen
before lower-impact work:

| Priority | Default target |
|----------|----------------|
| `URGENT` | 1 business day |
| `HIGH` | 2 business days |
| `MEDIUM` | 3 business days |
| `LOW` | 5 business days |

Business days are Monday through Friday. Weekends are skipped; statutory and
company holidays are not excluded unless a holiday-calendar integration is
added later.

Changing a ticket's priority recalculates its target from the time of
reprioritisation. An explicit admin-supplied target still takes precedence.
Urgent and high-priority SPOC notification subjects are visibly flagged.

The backend also promotes tickets automatically when their subject or
description contains clear safety/emergency signals (for example fire, smoke,
injury, overheating, or a gas leak) or serious outage signals (for example an
offline system, production stoppage, or data loss). Automatic triage can raise
a submitted priority but never lower it.

Override the defaults with `TICKET_SLA_URGENT_BUSINESS_DAYS`,
`TICKET_SLA_HIGH_BUSINESS_DAYS`, `TICKET_SLA_MEDIUM_BUSINESS_DAYS`, and
`TICKET_SLA_LOW_BUSINESS_DAYS`.

## Attachments

`POST /api/tickets/{id}/attachments` accepts multipart field `file` for
`image/*` and `video/*` content. Authorization is checked for upload, download,
and deletion. Original names are metadata only; server-generated storage keys
prevent path traversal and filename collisions.

The default implementation stores media under `./data/ticket-uploads`. Set
`TICKET_UPLOAD_DIR` to a persistent mounted disk in production. The storage
contract is isolated behind `AttachmentStorage`, so an object-storage adapter
can replace local disk without changing the API or ticket service.

## Email notifications

Notifications are transaction-aware: email is attempted only after ticket data
commits, and a provider outage never rolls back a ticket. Events cover creation,
SPOC notification, assignment, status changes, and resolution.

Email is logged by default in every environment so a deployment can start
without email credentials. To use Resend in production, either set the values
in the deployment environment or copy
`application-secrets.properties.example` to the ignored
`application-secrets.properties` file and paste the generated key there:

```text
app.notifications.provider=resend
app.notifications.resend.api-key=re_...
app.notifications.from=Takachar Support <onboarding@resend.dev>
```

The equivalent environment variables are `EMAIL_PROVIDER=resend`,
`RESEND_API_KEY`, and `NOTIFICATION_FROM`. A verified Takachar sending domain
should replace `onboarding@resend.dev` before emailing real customers. SMTP is
still available by selecting `EMAIL_PROVIDER=smtp` and supplying the existing
`SMTP_*` settings.

`TICKET_DEFAULT_SPOC_EMAIL` supplies a fallback when a create request does not
include a SPOC.

## Complaints and SPOC-created accounts

Feedback and complaints share the `feedback` table and are distinguished by
the `type` field (`FEEDBACK` or `COMPLAINT`). Admins can filter and sort the
combined list with `GET /api/feedback?type=COMPLAINT&sortBy=type&direction=ASC`.

Public registration is disabled. The administrator acting as SPOC creates and
lists customer accounts through `/api/admin/customers`; passwords are BCrypt
hashed and never returned by the API.

## Hugging Face sentiment

New and re-analysed feedback records persist `POSITIVE`, `NEUTRAL`, or
`NEGATIVE`, confidence, model, and analysis timestamp. Configure:

```text
HF_TOKEN=hf_...
HF_SENTIMENT_MODEL=cardiffnlp/twitter-roberta-base-sentiment-latest
```

The client uses Hugging Face's current serverless route:
`https://router.huggingface.co/hf-inference/models/{model}`. If the token is
absent or inference is temporarily unavailable, feedback submission still
succeeds with an explicit neutral fallback and can be re-analysed later through
`POST /api/feedback/{id}/sentiment`.

## Verification

Run with a JDK (not a JRE):

```text
mvn test
```

The integration suite covers diagnostic escalation, ticket ownership and
admin permissions, lifecycle transitions, analytics, media upload/download,
MIME rejection, and sentiment fallback.
