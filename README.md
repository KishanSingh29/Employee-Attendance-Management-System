# AttendTrack — Employee Attendance Management System

AttendTrack is a microservices-based HR platform for daily attendance, leave management and
payroll-day calculation. Employees check in / check out, track paid and sick leave balances,
and see how unpaid days affect their salary; HR approves requests, adjusts salaries and exports
monthly reports. The backend is three independent Spring Boot services that communicate over
Kafka, each with its own MySQL database, fronted by a dependency-free HTML/CSS/JS UI.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Microservices](#microservices)
- [Kafka Event Flow](#kafka-event-flow)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start (Docker)](#quick-start-docker)
- [Manual Setup (without Docker)](#manual-setup-without-docker)
- [Running the Frontend](#running-the-frontend)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Edge Cases & Validation](#edge-cases--validation)
- [Project Structure](#project-structure)

---

## Architecture

```
                                 ┌───────────────────────────────────────────┐
                                 │            frontend2/ (static)            │
                                 │   HTML + CSS + vanilla JS  (no build)     │
                                 │   login · dashboards · attendance · leave │
                                 └───────────────┬───────────────────────────┘
                                                 │  fetch() + JWT Bearer token
             ┌───────────────────────────────────┼───────────────────────────────────┐
             │                                   │                                   │
     ┌───────▼────────┐                 ┌────────▼─────────┐                 ┌────────▼─────────┐
     │  authservice   │                 │ attendanceservice│                 │   leaveservice   │
     │   :8081        │                 │      :8082       │                 │      :8083       │
     │ JWT issuer     │                 │ JWT validator    │                 │ JWT validator    │
     │ users / roles  │                 │ check-in/out     │                 │ apply / approve  │
     └───┬────────┬───┘                 └────────┬─────────┘                 └────────┬─────────┘
         │        │                              │                                   │
         │        │  publish "user-registered"   │  consume                          │  consume
         │        └──────────────►┌──────────────▼───────────────────────────────────▼──┐
         │                        │            Apache Kafka  (topic: user-registered)   │
         │                        └─────────────────────────────────────────────────────┘
         │
   ┌─────▼────────┐   ┌──────────────────────┐   ┌──────────────────────┐
   │ authservice_ │   │ attendanceservice_db │   │   leaveservice_db    │
   │     db       │   │                      │   │                      │
   └──────────────┘   └──────────────────────┘   └──────────────────────┘
                          MySQL 8  (one logical DB per service)

   All of the above run as containers on a single `attendance-network` bridge (docker-compose).
```

**Design notes**

- **Database per service** – no service reads another service's tables. Identity data is
  replicated into `attendanceservice` (`employee_profiles`) and `leaveservice` (`leave_balance`)
  through the Kafka event, not through shared tables or synchronous calls.
- **One token issuer** – only `authservice` signs JWTs. `attendanceservice` and `leaveservice`
  share the same HMAC secret and *validate* tokens locally; they never call `authservice` at
  request time.
- **Eventual consistency** – a freshly registered user can check in / apply for leave only after
  the `user-registered` event has been consumed (typically milliseconds). Consumers are
  idempotent, so replays are safe.

---

## Tech Stack

| Layer            | Technology                                                                 |
|------------------|---------------------------------------------------------------------------|
| **Backend**      | Java 21, Spring Boot 3.3.5, Spring Web (MVC), Spring Data JPA / Hibernate |
|                  | Spring Security + JWT (`io.jsonwebtoken` jjwt 0.12.6, HS256)             |
|                  | Spring for Apache Kafka (producer / consumer, JSON serialization)        |
|                  | Bean Validation (Jakarta), Lombok                                        |
|                  | springdoc-openapi 2.6.0 (Swagger UI)                                     |
| **Frontend**     | Static HTML5, CSS3 (IBM Plex Sans / Mono), vanilla ES2020 JavaScript     |
|                  | No framework, no bundler, no `node_modules` — served as plain files      |
| **Data**         | MySQL 8.0 (`ddl-auto=update`, schema auto-created per service)           |
| **Messaging**    | Apache Kafka 7.5.0 (Confluent images) + ZooKeeper                        |
| **Infrastructure** | Docker & Docker Compose, multi-stage Dockerfiles (Maven → JRE 21)     |
| **Build**        | Maven (wrapper `./mvnw` included in every service)                      |

---

## Microservices

### 1. authservice — `:8081`

| | |
|---|---|
| **Purpose**  | Registration, login, JWT issuance & refresh, logout (token blacklist), user directory. The single source of truth for employee identity. |
| **Database** | `authservice_db` |
| **Publishes**| Kafka topic `user-registered` on every successful registration |
| **Tables**   | `users`, `refresh_tokens`, `blacklisted_tokens` |

**Key endpoints**

| Method | Path              | Auth        | Description |
|--------|-------------------|-------------|-------------|
| POST   | `/auth/register`  | public      | Create an employee; auto-generates `EMP001…`, publishes `user-registered` (payload includes `salary`). |
| POST   | `/auth/login`     | public      | Returns `accessToken` (JWT, 1 h) + `refreshToken` (opaque UUID, 24 h). |
| POST   | `/auth/refresh`   | public      | Exchange a valid refresh token for a new access token. |
| GET    | `/auth/me`        | EMPLOYEE/HR | Current user profile (`userId, employeeId, firstName, lastName, email, role, department, salary`). |
| GET    | `/auth/employees` | HR          | List every registered employee. |
| POST   | `/auth/logout`    | EMPLOYEE/HR | Blacklists the current access-token JTI and revokes the user's refresh tokens. |

### 2. attendanceservice — `:8082`

| | |
|---|---|
| **Purpose**  | Daily check-in / check-out, working-hours calculation, personal history & monthly summary, and organisation-wide attendance views for HR. |
| **Database** | `attendanceservice_db` |
| **Consumes** | Kafka topic `user-registered` → creates a local `EmployeeProfile` |
| **Tables**   | `attendance`, `employee_profiles` |

**Business rules** (configurable in `application.properties`)

- `attendance.late-threshold = 09:30:00` — check-in after this time ⇒ `LATE`, otherwise `PRESENT`.
- `attendance.half-day-hours = 4` — worked hours `< 4` ⇒ `HALF_DAY`.
- Overnight shifts are supported: hours are computed across real dates, never negative.

**Key endpoints**

| Method | Path                                   | Auth        | Description |
|--------|----------------------------------------|-------------|-------------|
| POST   | `/attendance/checkin`                  | EMPLOYEE/HR | Mark today's check-in (once per day). |
| POST   | `/attendance/checkout`                 | EMPLOYEE/HR | Mark check-out; computes `workingHours` and final status. |
| GET    | `/attendance/today`                    | EMPLOYEE/HR | Today's record for the caller. |
| GET    | `/attendance/history?month=&year=`     | EMPLOYEE/HR | Caller's daily records for a month. |
| GET    | `/attendance/summary?month=&year=`     | EMPLOYEE/HR | Totals: present / absent / late / half-day / on-leave / working hours. |
| GET    | `/attendance/hr/dashboard`             | HR          | Today's company-wide counts. |
| GET    | `/attendance/hr/all?date=`             | HR          | Every employee's attendance for a given date. |
| GET    | `/attendance/hr/employee/{userId}?month=&year=` | HR | One employee's monthly records. |
| GET    | `/attendance/hr/report?month=&year=`   | HR          | Per-employee monthly aggregates. |
| POST   | `/attendance/profiles`                 | HR          | Manually (re)sync an employee profile from authservice data. |
| GET    | `/attendance/profiles/me`              | EMPLOYEE/HR | The caller's local profile mirror. |

### 3. leaveservice — `:8083`

| | |
|---|---|
| **Purpose**  | Leave applications, HR approval workflow, yearly PL/SL balances, and unpaid-leave payroll deduction. |
| **Database** | `leaveservice_db` |
| **Consumes** | Kafka topic `user-registered` → creates a `LeaveBalance` for the current year (PL 12, SL 6, `monthlySalary` from the event, default `30000`). |
| **Tables**   | `leave_balance`, `leave_request` |

**Policy** (configurable in `application.properties`)

- `leave.paid-total = 12`, `leave.sick-total = 6` days per calendar year.
- Weekends are excluded from the day count (`WorkingDays` helper).
- Unpaid-leave deduction: **per-day salary = monthly salary ÷ 26 working days**; total = unpaid working days in the month × per-day salary.
- `leave.monthly-salary = 30000` is only a fallback for balances created before salaries existed.

**Key endpoints**

| Method | Path                                          | Auth        | Description |
|--------|-----------------------------------------------|-------------|-------------|
| POST   | `/leave/apply`                                | EMPLOYEE/HR | Apply for `PAID` / `SICK` / `UNPAID` leave (future dates only). |
| GET    | `/leave/my-requests`                          | EMPLOYEE/HR | Caller's requests, newest first. |
| GET    | `/leave/balance?year=`                        | EMPLOYEE/HR | Remaining PL / SL for the year. |
| GET    | `/leave/deduction?month=&year=`               | EMPLOYEE/HR | Unpaid days + `monthlySalary`, `perDaySalary`, `totalDeduction` for the month. |
| PUT    | `/leave/cancel/{leaveId}`                     | EMPLOYEE/HR | Cancel a **PENDING** request (owner only). |
| GET    | `/leave/hr/pending`                           | HR          | All pending requests. |
| PUT    | `/leave/hr/approve/{leaveId}`                 | HR          | Approve; deducts the balance atomically. |
| PUT    | `/leave/hr/reject/{leaveId}`                  | HR          | Reject with a reason (stored in `remarks`, shown to the employee). |
| GET    | `/leave/hr/all?status=`                       | HR          | All requests, optional `PENDING/APPROVED/REJECTED` filter. |
| GET    | `/leave/hr/employee/{userId}/balance?year=`   | HR          | A specific employee's balance. |
| GET    | `/leave/hr/report?month=&year=`               | HR          | Per-employee monthly leave summary. |
| PUT    | `/leave/hr/employee/{userId}/salary`          | HR          | Update an employee's monthly salary (`{ "salary": 45000.0 }`, min 1000). |

---

## Kafka Event Flow

Topic: **`user-registered`** (1 partition, auto-created by `authservice` on startup)
Payload (JSON, no type headers):

```json
{
  "userId": "765a2d44-6423-47d9-a542-69e58d028a4d",
  "employeeId": "EMP001",
  "firstName": "Kishan",
  "lastName": "Singh",
  "email": "kishan@gmail.com",
  "department": "IT",
  "role": "EMPLOYEE",
  "salary": 30000.0
}
```

**Step by step**

1. Client calls `POST /auth/register` on **authservice**.
2. authservice validates the body, generates the next `EMPXXX` id, hashes the password, and
   saves a `users` row (including `salary`).
3. In the same transaction path it calls `UserEventProducer.publishUserRegistered(...)`, which
   serializes a `UserRegisteredEvent` and sends it to `user-registered`.
4. authservice responds `201 Created` with `{ success, message, employeeId, userId }`.
5. **attendanceservice** `UserRegisteredConsumer` (group `attendance-group`) receives the event
   and inserts an `employee_profiles` row — unless one already exists (idempotent; unique
   `user_id`, `DataIntegrityViolationException` swallowed on race).
6. **leaveservice** `UserRegisteredConsumer` (group `leave-group`) receives the same event and
   inserts a `leave_balance` row for `Year.now()` with `pl_total = 12`, `sl_total = 6`,
   `monthly_salary = event.salary` (or `30000` if null). Also idempotent.
7. The employee can now log in, check in, and apply for leave. If a consumer is down, the event
   is re-delivered later (`auto-offset-reset=earliest`) and the same idempotent insert runs.

> A missed/duplicated event never corrupts state: consumers check for an existing row first and
> catch unique-constraint violations.

---

## Features

### Employee

- Register with first/last name, work email, password, role, department and **monthly salary**.
- Log in / stay signed in (JWT access + refresh tokens stored in `localStorage`).
- **Check in / check out** from the dashboard; live "today's timesheet" (in / out / hours / status).
- Attendance history with a **month/year filter**, **summary cards**, and a **colour-coded calendar view** (Present / Late / Half-day / On-leave / Absent, weekends greyed out).
- Leave balances (PL / SL) with progress bars and days remaining.
- **Apply for leave** (PAID / SICK / UNPAID) with client + server validation.
- "My leave requests" table with status badges, **HR rejection remarks**, and a **Cancel** action for pending requests.
- **Leave-deduction card**: monthly salary, per-day rate (÷26), unpaid days, total deduction.

### HR

- Everything an employee has, plus the HR console:
- **Dashboard** — today's totals (employees / present / absent / on-leave / late) and a
  **sortable** "today's attendance" table (click any column header, asc/desc).
- **Pending leave requests** — approve inline, or reject with a required reason (modal).
- **Employees** — searchable / department-filtered directory with role badges.
- **Employee detail** — one employee's monthly attendance (summary cards + daily table).
- **Update Salary** — per-employee modal that calls `PUT /leave/hr/employee/{userId}/salary`.
- **Leave management** — all requests with `All / Pending / Approved / Rejected` tabs, approve/reject.
- **Attendance report** — monthly per-employee aggregates with **CSV export**.

---

## Prerequisites

| Tool               | Version | Notes |
|--------------------|---------|-------|
| **Java (JDK)**     | 21      | Required to build/run the services (`java.version` = 21). |
| **Maven**          | 3.9+    | Optional — each service ships `./mvnw` / `mvnw.cmd`. |
| **Docker Desktop** | latest  | For the one-command Docker path (bundles MySQL, Kafka, ZooKeeper). |
| **MySQL**          | 8.0     | Only for the manual (non-Docker) path. |
| **Apache Kafka**   | 3.x     | Only for the manual path. |

A modern browser is enough for the frontend (no Node.js required).

---

## Quick Start (Docker)

From the repository root:

```bash
docker compose up --build
```

This builds and starts **six** containers on the `attendance-network` bridge:

| Container              | Image                          | Host port(s)        |
|------------------------|--------------------------------|---------------------|
| `att-mysql`            | `mysql:8.0`                    | `3307` → 3306       |
| `att-zookeeper`        | `confluentinc/cp-zookeeper`    | `2181`              |
| `att-kafka`            | `confluentinc/cp-kafka`        | `9092`, `29092`     |
| `att-authservice`      | built from `./authservice`     | `8081`              |
| `att-attendanceservice`| built from `./attendanceservice`| `8082`             |
| `att-leaveservice`     | built from `./leaveservice`    | `8083`              |

**Startup order** is handled for you: MySQL and Kafka expose health checks, and each service
`depends_on` them with `condition: service_healthy`. The three MySQL schemas
(`authservice_db`, `attendanceservice_db`, `leaveservice_db`) are created automatically
(`createDatabaseIfNotExist=true` + Hibernate `ddl-auto=update`).

**Verify**

```bash
curl http://localhost:8081/swagger-ui.html   # auth
curl http://localhost:8082/swagger-ui.html   # attendance
curl http://localhost:8083/swagger-ui.html   # leave
```

Then open the frontend — see [Running the Frontend](#running-the-frontend).

**Stop / reset**

```bash
docker compose down            # stop containers
docker compose down -v         # stop and wipe the MySQL volume (fresh databases)
```

---

## Manual Setup (without Docker)

### 1. MySQL

Start a local MySQL 8 on `localhost:3306` with user `root` / password `root`
(or edit `spring.datasource.*` in each service's `application.properties`). You do **not** need
to create the databases — each service creates its own on first run.

```sql
-- optional: pre-create, otherwise createDatabaseIfNotExist handles it
CREATE DATABASE IF NOT EXISTS authservice_db;
CREATE DATABASE IF NOT EXISTS attendanceservice_db;
CREATE DATABASE IF NOT EXISTS leaveservice_db;
```

### 2. Kafka

Run Kafka + ZooKeeper locally so a broker is reachable at `localhost:9092`
(the value of `spring.kafka.bootstrap-servers` in every service).

```bash
# from a Kafka distribution
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh     config/server.properties
```

The `user-registered` topic is created automatically by authservice on startup
(`KafkaTopicConfig`), so no manual topic creation is needed.

> Using the Dockerised Kafka from `docker compose` for a locally-run service instead? Point the
> service at the host listener: `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:29092`.

### 3. Run each service

Open three terminals (order does not strictly matter, but start `authservice` first so the
topic exists):

```bash
# terminal 1
cd authservice && ./mvnw spring-boot:run        # → http://localhost:8081

# terminal 2
cd attendanceservice && ./mvnw spring-boot:run  # → http://localhost:8082

# terminal 3
cd leaveservice && ./mvnw spring-boot:run       # → http://localhost:8083
```

Build a jar instead:

```bash
cd authservice && ./mvnw clean package      # target/authservice-0.0.1-SNAPSHOT.jar
java -jar target/*.jar
```

`./mvnw` on Windows: use `mvnw.cmd`.

---

## Running the Frontend

`frontend2/` is 100 % static — no build step. Its API base URLs are hard-coded in
`frontend2/js/auth.js`:

```js
const AUTH_URL       = "http://localhost:8081";
const ATTENDANCE_URL = "http://localhost:8082";
const LEAVE_URL      = "http://localhost:8083";
```

Serve the folder over HTTP (opening the files directly with `file://` breaks relative asset
loading and CORS):

```bash
cd frontend2
python -m http.server 5500
# open http://localhost:5500/login.html
```

Any static server works (`npx serve`, VS Code "Live Server", nginx, …).
Register a user, sign in, and you land on the employee or HR dashboard based on your role.

| Page                        | Role      |
|-----------------------------|-----------|
| `login.html`, `register.html` | public  |
| `employee-dashboard.html`, `employee-attendance.html`, `employee-leaves.html` | EMPLOYEE / HR |
| `hr-dashboard.html`, `hr-employees.html`, `hr-employee-detail.html`, `hr-leaves.html`, `hr-attendance-report.html` | HR |

---

## API Documentation

Each service publishes interactive OpenAPI docs (springdoc):

| Service            | Swagger UI                              | OpenAPI JSON                          |
|--------------------|-----------------------------------------|--------------------------------------|
| authservice        | http://localhost:8081/swagger-ui.html   | http://localhost:8081/v3/api-docs    |
| attendanceservice  | http://localhost:8082/swagger-ui.html   | http://localhost:8082/v3/api-docs    |
| leaveservice       | http://localhost:8083/swagger-ui.html   | http://localhost:8083/v3/api-docs    |

**Authorising in Swagger UI**

1. `POST /auth/login` on the auth service, copy `accessToken` from the response.
2. Click **Authorize** (top right), paste the token (scheme name `Bearer Auth`, format `bearer <token>`).
3. All secured endpoints on that service now send the `Authorization: Bearer …` header.

The same token is accepted by all three services (shared HMAC secret).

---

## Database Schema

Hibernate manages the schema (`ddl-auto=update`). Column names below are the physical names.

### `authservice_db`

**`users`**

| Column         | Type            | Notes |
|----------------|-----------------|-------|
| `id`           | BIGINT PK AI    | internal key |
| `user_id`      | VARCHAR(36) UQ  | public UUID, used everywhere across services |
| `employee_id`  | VARCHAR(20) UQ  | human id, sequential `EMP001`, `EMP002`, … |
| `first_name`   | VARCHAR(50)     | |
| `last_name`    | VARCHAR(50)     | |
| `email`        | VARCHAR(120) UQ | login identifier, stored lower-cased |
| `password`     | VARCHAR(255)    | BCrypt hash |
| `role`         | VARCHAR(20)     | `EMPLOYEE` \| `HR` |
| `department`   | VARCHAR(60)     | |
| `salary`       | DOUBLE NULL     | monthly salary; propagated via Kafka |
| `created_at`   | DATETIME        | set on insert |

**`refresh_tokens`**

| Column       | Type           | Notes |
|--------------|----------------|-------|
| `id`         | BIGINT PK AI   | |
| `token`      | VARCHAR(36) UQ | opaque UUID handed to the client |
| `user_id`    | BIGINT FK      | → `users.id` |
| `expires_at` | DATETIME       | 24 h after issue |
| `revoked`    | BOOLEAN        | set true on logout / rotation |

**`blacklisted_tokens`**

| Column       | Type           | Notes |
|--------------|----------------|-------|
| `id`         | BIGINT PK AI   | |
| `jti`        | VARCHAR(64) UQ | JWT id of a logged-out access token |
| `expires_at` | DATETIME       | original token expiry; row can be purged afterwards |

### `attendanceservice_db`

**`attendance`** — unique `(user_id, date)`; indexes on `date` and `user_id`

| Column          | Type          | Notes |
|-----------------|---------------|-------|
| `id`            | BIGINT PK AI  | |
| `user_id`       | VARCHAR(36)   | = `users.user_id` |
| `date`          | DATE          | one row per employee per day |
| `check_in`      | TIME NULL     | |
| `check_out`     | TIME NULL     | |
| `working_hours` | DOUBLE NULL   | set on check-out, rounded to 2 dp |
| `status`        | VARCHAR(20)   | `PRESENT` \| `ABSENT` \| `LATE` \| `HALF_DAY` \| `ON_LEAVE` \| `WEEKEND` |
| `created_at`    | DATETIME      | |

**`employee_profiles`** — local mirror of identity data (from the Kafka event)

| Column        | Type           | Notes |
|---------------|----------------|-------|
| `id`          | BIGINT PK AI   | |
| `user_id`     | VARCHAR(36) UQ | |
| `employee_id` | VARCHAR(20) UQ | |
| `first_name`  | VARCHAR(50)    | |
| `last_name`   | VARCHAR(50)    | |
| `email`       | VARCHAR(120)   | |
| `department`  | VARCHAR(60)    | |
| `created_at`  | DATETIME       | |

### `leaveservice_db`

**`leave_balance`** — unique `(user_id, year)`

| Column           | Type            | Notes |
|------------------|-----------------|-------|
| `id`             | BIGINT PK AI    | |
| `user_id`        | VARCHAR(36)     | |
| `year`           | INT             | calendar year the balance applies to |
| `pl_total`       | INT             | paid-leave allowance (default 12) |
| `pl_used`        | INT             | incremented on approval |
| `sl_total`       | INT             | sick-leave allowance (default 6) |
| `sl_used`        | INT             | incremented on approval |
| `unpaid_used`    | INT             | approved unpaid days, for reporting |
| `monthly_salary` | DOUBLE NOT NULL | from the Kafka event; default `30000.0`; editable by HR |
| `created_at`     | DATETIME        | |

**`leave_request`** — indexes on `user_id`, `status`, `start_date`

| Column        | Type          | Notes |
|---------------|---------------|-------|
| `id`          | BIGINT PK AI  | |
| `user_id`     | VARCHAR(36)   | applicant |
| `employee_id` | VARCHAR(20)   | denormalised for reports |
| `first_name`  | VARCHAR(50)   | denormalised for reports |
| `last_name`   | VARCHAR(50)   | denormalised for reports |
| `leave_type`  | VARCHAR(20)   | `PAID` \| `SICK` \| `UNPAID` |
| `start_date`  | DATE          | |
| `end_date`    | DATE          | |
| `total_days`  | INT           | working days in range (weekends excluded) |
| `reason`      | VARCHAR(500)  | applicant's reason |
| `status`      | VARCHAR(20)   | `PENDING` \| `APPROVED` \| `REJECTED` |
| `approved_by` | VARCHAR(36)   | HR `user_id` that actioned it |
| `remarks`     | VARCHAR(500)  | HR note — currently the rejection reason |
| `applied_at`  | DATETIME      | set on insert |
| `updated_at`  | DATETIME      | touched on every update |

---

## Edge Cases & Validation

### Authentication & authorization

- **Duplicate email** on register → `409 Conflict` (`EmailAlreadyExistsException`).
- **Field validation** on register — first/last name required (≤50), valid email (≤120),
  password 8–72 chars, role required, department required (≤60), **salary required and ≥ 1000**.
- **Bad credentials** on login → `401 Unauthorized` (same message for unknown email and wrong
  password — no user enumeration).
- **Missing / malformed / expired JWT** → `401` via `JwtAuthenticationEntryPoint`.
- **Logged-out token** — its `jti` is blacklisted; reuse → `401`.
- **Role check** — HR-only routes (`/auth/employees`, all `/attendance/hr/**`, all `/leave/hr/**`)
  return `403 Forbidden` for `EMPLOYEE` tokens.
- **Refresh token** — expired or `revoked` → `401`; every login issues a fresh one and logout
  revokes all of the user's refresh tokens.
- Registration and the two consumers are **idempotent**: a replayed `user-registered` event does
  not create duplicate profiles or balances.

### Attendance

- **One check-in per day** — enforced in code *and* by a unique `(user_id, date)` constraint;
  a second attempt → `AttendanceException` ("already checked in today at …").
- **Check-out before check-in** → `AttendanceException` ("cannot check out before checking in").
- **Double check-out** → `AttendanceException` ("already checked out today at …").
- **Overnight shift** — check-in and check-out on different calendar days are spanned across the
  real dates, so working hours are never zero or negative.
- **Check-out earlier than check-in** (same day) → rejected.
- **Status derivation** — check-in after `09:30` ⇒ `LATE`; on check-out, `< 4 h` ⇒ `HALF_DAY`,
  otherwise a `LATE` check-in stays `LATE`, else `PRESENT`.
- A profile is auto-created on first check-in if the Kafka event has not arrived yet
  (`profileService.ensureProfile`).
- `month` must be `1–12` on history/summary/report endpoints.

### Leave

- **`endDate` before `startDate`** → `LeaveValidationException`.
- **Non-future start date** — leave can only be applied for dates strictly after today.
- **No working days** in the selected range (e.g. a pure weekend) → rejected.
- **Overlapping request** — a new request that overlaps any `PENDING`/`APPROVED` request → rejected.
- **Insufficient balance** — for `PAID`/`SICK`, `requestedDays` is checked against
  `remaining − pendingDays` so two pending requests cannot both consume the same allowance.
  `UNPAID` is always allowed.
- **Approval re-checks the balance** at action time; if the employee no longer has enough
  PL/SL the approval fails instead of driving the balance negative.
- **Cancel** — only the **owner** can cancel, and only while `PENDING`.
- **Approve / reject** — only `PENDING` requests; reject requires a non-blank reason (≤500 chars),
  stored in `remarks` and surfaced to the employee.
- **Deduction** — `perDaySalary = round(monthlySalary / 26, 2)`; unpaid days counted are the
  approved `UNPAID` working days that fall *within the requested month*; `totalDeduction` is
  rounded to 2 dp. Missing balance row is created on the fly with the default salary.
- **Salary update** (`PUT /leave/hr/employee/{userId}/salary`) — `salary` required and ≥ 1000;
  unknown `userId` (no balance for the current year) → `404 Not Found` ("Employee not found").

---

## Project Structure

```
attendance-management/
├── docker-compose.yml            # mysql + zookeeper + kafka + 3 services
├── README.md
│
├── authservice/                  # :8081  — identity, JWT, user directory
│   ├── Dockerfile                # multi-stage: maven:3.9-temurin-21 → temurin-21-jre
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/
│       ├── java/com/attendance/authservice/
│       │   ├── AuthServiceApplication.java
│       │   ├── config/           # OpenApiConfig, SecurityConfig
│       │   ├── controller/       # AuthController
│       │   ├── dto/{request,response}/
│       │   ├── entity/           # User, RefreshToken, BlacklistedToken, Role
│       │   ├── event/            # UserEventProducer, UserRegisteredEvent, KafkaTopicConfig
│       │   ├── exception/        # GlobalExceptionHandler + typed exceptions
│       │   ├── repository/       # User/RefreshToken/BlacklistedToken repositories
│       │   ├── security/         # JwtService, JwtAuthFilter, UserPrincipal, entry points
│       │   └── service/          # AuthServiceImpl, RefreshTokenService, EmployeeIdGenerator
│       └── resources/application.properties
│
├── attendanceservice/            # :8082  — check-in/out, history, HR views
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/
│       ├── java/com/attendance/attendanceservice/
│       │   ├── AttendanceServiceApplication.java
│       │   ├── config/           # OpenApiConfig, SecurityConfig
│       │   ├── controller/       # AttendanceController, AttendanceHrController, EmployeeProfileController
│       │   ├── dto/{request,response}/
│       │   ├── entity/           # Attendance, AttendanceStatus, EmployeeProfile
│       │   ├── event/            # UserRegisteredConsumer, UserRegisteredEvent
│       │   ├── exception/        # GlobalExceptionHandler + typed exceptions
│       │   ├── repository/       # AttendanceRepository, EmployeeProfileRepository
│       │   ├── security/         # JwtService, JwtAuthFilter, CurrentUserProvider, AuthenticatedUser
│       │   └── service/          # AttendanceServiceImpl, EmployeeProfileServiceImpl, support/MonthRange
│       └── resources/application.properties
│
├── leaveservice/                 # :8083  — leave apply/approve, balances, deduction, salary
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/
│       ├── java/com/attendance/leaveservice/
│       │   ├── LeaveServiceApplication.java
│       │   ├── config/           # OpenApiConfig, SecurityConfig
│       │   ├── controller/       # LeaveController, LeaveHrController
│       │   ├── dto/{request,response}/   # incl. UpdateSalaryRequest, LeaveDeductionResponse
│       │   ├── entity/           # LeaveBalance, LeaveRequest, LeaveStatus, LeaveType
│       │   ├── event/            # UserRegisteredConsumer, UserRegisteredEvent
│       │   ├── exception/        # GlobalExceptionHandler + typed exceptions
│       │   ├── repository/       # LeaveBalanceRepository, LeaveRequestRepository
│       │   ├── security/         # JwtService, JwtAuthFilter, CurrentUserProvider, AuthenticatedUser
│       │   └── service/          # LeaveServiceImpl, LeaveBalanceServiceImpl, support/WorkingDays
│       └── resources/application.properties
│
└── frontend2/                    # static UI — no build, no dependencies
    ├── login.html
    ├── register.html
    ├── employee-dashboard.html
    ├── employee-attendance.html      # table + colour-coded calendar view
    ├── employee-leaves.html
    ├── hr-dashboard.html             # sortable today's-attendance table
    ├── hr-employees.html             # directory + "Update Salary" modal
    ├── hr-employee-detail.html
    ├── hr-leaves.html
    ├── hr-attendance-report.html     # CSV export
    ├── css/
    │   └── style.css                 # design system (IBM Plex, cards, badges, modals)
    └── js/
        ├── auth.js                   # token storage, role guards, service base URLs
        ├── api.js                    # every endpoint as a typed fetch wrapper
        └── utils.js                  # formatting, toasts, table/empty-state helpers
```

---

## Future Enhancements

The following features are planned for future development:

### Phase 2 — Advanced Features

- **Push Notifications** — Real-time alerts for leave approval / rejection via Firebase.
- **Email Notifications** — Automated email on check-in and leave status updates.
- **Shift Management** — Multiple shift support (morning / evening / night).
- **Overtime Tracking** — Auto-detect and log overtime hours.
- **Holiday Calendar** — National / company holidays integration.

### Phase 3 — Analytics & Reporting

- **Advanced Analytics Dashboard** — Charts and graphs for attendance trends.
- **Payroll Integration** — Auto-generate payroll from attendance and deductions.
- **Department-wise Reports** — Detailed per-department analytics.
- **Export to Excel / PDF** — Full report export functionality.

### Phase 4 — Infrastructure

- **Redis Caching** — Cache frequently accessed data for better performance.
- **GCP Cloud Run Deployment** — Production deployment on Google Cloud.
- **GitHub Actions CI/CD** — Automated build and deployment pipeline.
- **Kubernetes Orchestration** — Container orchestration for scaling.
- **API Gateway** — Single entry point for all microservices.

### Phase 5 — Mobile App

- **React Native Mobile App** — Cross-platform mobile application.
- **GPS-based Attendance** — Location verification for check-in.
- **Face Recognition** — Biometric attendance marking.
- **Offline Support** — Work without an internet connection.

---

*Built with passion by Kishan Singh*
*4th Year IT Student, Haldia Institute of Technology*
*Open to internship opportunities in Backend + AI Engineering*
