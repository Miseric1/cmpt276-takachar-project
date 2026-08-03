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

The same JPA entities run on local H2 for development and Supabase PostgreSQL
when `SPRING_PROFILES_ACTIVE=prod`; no tree-specific datasource is used.

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
commits, and an SMTP outage never rolls back a ticket. Events cover creation,
SPOC notification, assignment, status changes, and resolution.

Email is disabled by default and logged locally. To enable SMTP:

```text
EMAIL_NOTIFICATIONS_ENABLED=true
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=...
SMTP_PASSWORD=...
SMTP_AUTH=true
SMTP_STARTTLS=true
NOTIFICATION_FROM=support@takachar.com
```

`TICKET_DEFAULT_SPOC_EMAIL` supplies a fallback when a create request does not
include a SPOC. `TICKET_TARGET_HOURS` controls the default target (72 hours).

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
