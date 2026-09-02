# LeaveService

Leave application, approval and balance tracking for the **Employee Attendance Management System**.

- **Port:** `8083`
- **Database:** MySQL `leaveservice_db` (auto-created on first run)
- **Stack:** Spring Boot 3.3, Java 21, Spring Security + JWT (validation only), JPA/Hibernate, Lombok, springdoc-openapi
- Validates the JWTs issued by **authservice** (`:8081`). Shared `jwt.secret`.

---

## Running

### Prerequisites
- JDK 21 on `PATH` / `JAVA_HOME` (the bundled `mvnw` uses whatever `JAVA_HOME` points at)
- MySQL on `localhost:3306`, `root` / `root`
- A running authservice to obtain a token from

```bash
./mvnw spring-boot:run
# or
./mvnw clean package && java -jar target/leaveservice-0.0.1-SNAPSHOT.jar
```

Swagger UI: `http://localhost:8083/swagger-ui.html` — **Authorize** with the raw
access token from `authservice` `POST /auth/login`.

---

## Authentication & roles

Every request needs `Authorization: Bearer <accessToken>`. Claims (`sub` = userId,
`role`, `employeeId`, `email`) are trusted directly — no user table, no DB lookup.

| Role     | Access                                                            |
|----------|-----------------------------------------------------------------|
| EMPLOYEE | `/leave/apply`, `/leave/my-requests`, `/leave/balance`, `/leave/deduction`, `/leave/cancel/{id}` (own PENDING only) |
| HR       | everything above **plus** all `/leave/hr/**`                     |

---

## Endpoints

### Employee

| Method | Path                                   | Description |
|--------|----------------------------------------|-------------|
| POST   | `/leave/apply`                         | Apply for leave. `201`. |
| GET    | `/leave/my-requests`                   | Caller's requests, newest first. |
| GET    | `/leave/balance?year=2026`             | Caller's balance (year optional, defaults to current). |
| GET    | `/leave/deduction?month=8&year=2026`   | Unpaid-leave salary deduction (both optional, default current month). |
| PUT    | `/leave/cancel/{leaveId}`              | Cancel a PENDING request (deletes it). |

`POST /leave/apply` body:
```json
{ "leaveType": "PAID", "startDate": "2026-09-05", "endDate": "2026-09-07", "reason": "Personal work" }
```

### HR

| Method | Path                                          | Description |
|--------|-----------------------------------------------|-------------|
| GET    | `/leave/hr/pending`                            | All PENDING requests. |
| PUT    | `/leave/hr/approve/{leaveId}`                  | Approve + deduct balance. |
| PUT    | `/leave/hr/reject/{leaveId}`                   | Reject with `{ "reason": "..." }` (stored in `remarks`). |
| GET    | `/leave/hr/all?status=PENDING`                 | All requests, optional status filter. |
| GET    | `/leave/hr/employee/{userId}/balance?year=`    | One employee's balance. |
| GET    | `/leave/hr/report?month=8&year=2026`           | Per-employee leave summary for the month. |

---

## Business rules

| Rule | Notes |
|------|-------|
| Weekends (Sat/Sun) are never counted | `totalDays` = weekdays in `[startDate, endDate]` |
| A range with **zero** weekdays is rejected | |
| `startDate` must be **strictly after today** | "Future dates only" |
| No overlap with an existing PENDING/APPROVED request | inclusive date-range overlap |
| PAID → needs `plTotal − plUsed − pendingPaidDays ≥ totalDays` | pending requests are reserved |
| SICK → needs `slTotal − slUsed − pendingSickDays ≥ totalDays` | |
| UNPAID → always allowed | only tracked for payroll |
| Balance is deducted **on approval**, not on apply | re-checked at approval time |
| Reject / cancel never touch the balance | cancel = delete the PENDING row |
| Only a **PENDING** request can be approved / rejected / cancelled | |
| Deduction: `perDay = monthlySalary / workingDaysPerMonth` (30000 / 30 = 1000) | `leave.*` properties |
| Deduction counts APPROVED **UNPAID** weekdays that fall inside the target month | spans month boundaries correctly |

Default allowances (`application.properties`): `leave.paid-total=12`, `leave.sick-total=6`.
A `LeaveBalance` row is created lazily the first time a user's balance is needed.

---

## Architecture

```
controller/   LeaveController        employee: apply / my-requests / balance / deduction / cancel
              LeaveHrController      /leave/hr/** (HR only)
service/      LeaveService(+Impl)         apply / approval / reporting rules
              LeaveBalanceService(+Impl)  lazy get-or-create + read of LeaveBalance
              support/WorkingDays         weekday counting helpers
repository/    LeaveRequestRepository, LeaveBalanceRepository
entity/       LeaveRequest, LeaveBalance, LeaveType (enum), LeaveStatus (enum)
dto/          request/{ApplyLeaveRequest, RejectLeaveRequest} ; response/* (records)
security/     JwtService, JwtAuthFilter, AuthenticatedUser, CurrentUserProvider,
              JwtAuthenticationEntryPoint, RestAccessDeniedHandler
config/       SecurityConfig, OpenApiConfig
exception/    GlobalExceptionHandler + LeaveValidationException / ResourceNotFound / UnauthorizedAccess
```

---

## Design notes / deviations from the spec

1. **Cancel** — the spec allows an employee to cancel a PENDING request but the
   status enum has no `CANCELLED`. `PUT /leave/cancel/{leaveId}` **deletes** the
   PENDING row (no balance impact, enum stays as specified).
2. **`remarks` column added** to `LeaveRequest` to hold the HR rejection reason
   without overwriting the employee's original `reason`.
3. **`firstName` / `lastName` are not populated.** The authservice JWT carries
   `userId`, `employeeId`, `email` and `role` but not names, and this service has
   no profile table. The columns exist (per the entity spec) and stay null until
   a future enrichment / profile-sync step fills them.
4. **Balance is deducted on approval**, not on apply. On apply the request is only
   validated against `remaining − reservedByPendingRequests`; the actual
   `plUsed` / `slUsed` / `unpaidUsed` increment happens in `approve`, and is
   re-checked there.
5. **`startDate` must be strictly after today** (spec: "Future dates only" /
   "Past dates pe apply nahi hoga").
6. **`deduction` accepts optional `month` / `year`** query params (default: current
   month) — the spec response includes them but the request signature was unstated.
