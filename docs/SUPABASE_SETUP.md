# Supabase PostgreSQL — Setup & Migration

Iteration 2 migrated the production database from Render's ephemeral PostgreSQL
to **Supabase PostgreSQL**, which is now the permanent provider. This document is
everything a developer or ops person needs to run the backend against Supabase
locally, in Docker, and on Render.

Nothing sensitive lives in the repository. The backend reads every credential
from environment variables at runtime.

---

## 1. What changed

- `application-prod.properties` now targets Supabase and adds HikariCP connection
  pooling tuned for Supabase's pgBouncer pooler, plus `open-in-view=false` and
  Hibernate hardening.
- Dev/test still use H2 (`application.properties` for local dev; an in-memory H2
  for the test suite). **No code depends on the database vendor.**
- The Spring profile is unchanged: production still runs with
  `SPRING_PROFILES_ACTIVE=prod`. Only the values behind the env vars change.

The frontend requires **no changes** because of this migration.

---

## 2. Get your Supabase connection details

1. Create a project at <https://supabase.com>.
2. In the dashboard: **Project Settings → Database**.
3. Under **Connection string → JDBC**, copy the URI. You will see two options:
   - **Session pooler / Direct** (host `db.<ref>.supabase.co`, port `5432`)
   - **Transaction pooler** (host `<ref>.pooler.supabase.com`, port `6543`)
   For a long-running Spring Boot service on Render, the **transaction pooler
   (6543)** is recommended because it scales connections better.
4. Note your database password (set when the project was created).

---

## 3. Environment variables

| Variable | Required | Example | Notes |
|----------|----------|---------|-------|
| `SPRING_PROFILES_ACTIVE` | yes (prod) | `prod` | Selects the Supabase profile |
| `SPRING_DATASOURCE_URL` | yes | `jdbc:postgresql://<ref>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0` | Full JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | yes | `postgres.<ref>` | Supabase username |
| `SPRING_DATASOURCE_PASSWORD` | yes | `••••••••` | Supabase database password |
| `DB_POOL_MAX_SIZE` | no | `5` | Max HikariCP connections (default 5) |
| `DB_POOL_MIN_IDLE` | no | `1` | Min idle connections (default 1) |

**Important URL parameters when using the transaction pooler (port 6543):**

- `sslmode=require` — Supabase requires TLS.
- `prepareThreshold=0` — disables server-side prepared statements, which the
  pgBouncer transaction pooler does not support. Omit this only if you use the
  direct/session connection on port 5432.

---

## 4. Run it locally against Supabase

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL="jdbc:postgresql://<ref>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
export SPRING_DATASOURCE_USERNAME="postgres.<ref>"
export SPRING_DATASOURCE_PASSWORD="your-password"

mvn spring-boot:run
```

On first start, Hibernate (`ddl-auto=update`) creates all tables, indexes, and
foreign keys automatically. To develop without Supabase, just run `mvn
spring-boot:run` with no env vars — the default H2 profile is used.

---

## 5. Docker

The existing multi-stage `Dockerfile` is unchanged and Supabase-ready — it reads
the same environment variables. Pass them at run time:

```bash
docker build -t supportsync .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://<ref>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0" \
  -e SPRING_DATASOURCE_USERNAME="postgres.<ref>" \
  -e SPRING_DATASOURCE_PASSWORD="your-password" \
  supportsync
```

---

## 6. Render deployment

Deployment still uses Render; only the environment variables change.

1. Render dashboard → your service → **Environment**.
2. Remove the old Render-PostgreSQL variables (or repoint them).
3. Add: `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`,
   `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (and optionally the
   pool-size vars).
4. Deploy. Startup logs should show HikariPool `TakacharHikariPool` connecting
   and Hibernate applying the schema.

No `Dockerfile`, `render.yaml`, or code change is required beyond the variables.

---

## 7. Verify the connection

- Startup logs contain `HikariPool-1 - Start completed` and no datasource errors.
- `GET /api/categories` returns `200` with `[]` on a fresh database.
- Sign in as the seeded admin (`admin@test.com` / `password123`, created by
  `DataInitializer`) and call an admin endpoint such as `GET /api/dashboard/overview`.

> Security note: change or disable the seeded admin account before any real
> deployment.
