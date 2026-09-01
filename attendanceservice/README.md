# AttendanceService

Check-in / check-out and attendance reporting for the **Employee Attendance Management System**.

- **Port:** `8082`
- **Database:** MySQL `attendanceservice_db` (auto-created on first run)
- **Stack:** Spring Boot 3.3, Java 21, Spring Security + JWT (validation only), JPA/Hibernate, Lombok, springdoc-openapi
- Depends on **authservice** (`:8081`) for issuing the JWTs it validates. Shared `jwt.secret`.

---

## Running

### Prerequisites
- JDK 21 on `PATH` / `JAVA_HOME` (the bundled `mvnw` still uses whatever `JAVA_HOME` points at)
- MySQL on `localhost:3306`, `root` / `root`
- A running authservice to log in against and obtain a token

```bash
./mvnw spring-boot:run
# or
./mvnw clean package && java -jar target/attendanceservice-0.0.1-SNAPSHOT.jar
```

Swagger UI: `http://localhost:8082/swagger-ui.html` — click **Authorize**, paste the
raw access token from `authservice` `POST /auth/login`.

---

## Authentication

Every request needs `Authorization: Bearer <accessToken>` from authservice.
The token's claims (`sub` = userId, `role`, `employeeId`, `email`) are trusted
directly — there is no user table and no DB lookup in the filter.

The employee endpoints also accept an optional **`X-User-Id`** header. It is only
a hint: the caller's identity always comes from the JWT, and for a non-HR caller a
mismatching `X-User-Id` is rejected with `403`.

| Role     | Can access                                                        |
|----------|-----------------------------------------------------------------|
| EMPLOYEE | own check-in/out, `/today`, `/history`, `/summary`, `/profiles/me` |
| HR       | everything above **plus** all `/attendance/hr/**` and `POST /attendance/profiles` |

---

## Endpoints

### Employee (`Authorization` + optional `X-User-Id`)

| Method | Path                                    | Description                              |
|--------|-----------------------------------------|-----------------------------------------|
| POST   | `/attendance/checkin`                   | Check in. After **09:30** → `LATE`. Second check-in → `400`. |
| POST   | `/attendance/checkout`                  | Check out. Needs a check-in. `< 4 h` worked → `HALF_DAY`. |
| GET    | `/attendance/today`                     | Today's record (or an `ABSENT` stub if none). |
| GET    | `/attendance/history?month=8&year=2026` | Records for the month.                   |
| GET    | `/attendance/summary?month=8&year=2026` | Monthly counts + total / average hours.  |
| GET    | `/attendance/profiles/me`              | Caller's profile as stored here.         |

### HR (`Authorization`, HR role)

| Method | Path                                                     | Description                       |
|--------|---------------------------------------------------------|----------------------------------|
| GET    | `/attendance/hr/all?date=2026-09-01`                   | Everyone's attendance for a date. |
| GET    | `/attendance/hr/employee/{userId}?month=8&year=2026`   | One employee's month history.     |
| GET    | `/attendance/hr/dashboard`                             | Today: total / present / absent / on-leave / late. |
| GET    | `/attendance/hr/report?month=8&year=2026`              | Per-employee monthly aggregates.  |
| POST   | `/attendance/profiles`                                 | Create / update an employee profile. |

### `POST /attendance/profiles` body
```json
{
  "userId": "uuid-from-authservice",
  "employeeId": "EMP001",
  "firstName": "Kishan",
  "lastName": "Singh",
  "email": "kishan@gmail.com",
  "department": "IT"
}
```

---

## Business rules

| Rule | Where |
|------|-------|
| Check-in after `09:30` → `LATE`, otherwise `PRESENT` | `attendance.late-threshold` |
| Second check-in on the same day → `400` | one row per `(user_id, date)` (unique constraint) |
| Check-out without a check-in → `400` | |
| Worked hours `< 4` → `HALF_DAY`; a `LATE` check-in stays `LATE` otherwise | `attendance.half-day-hours` |
| `workingHours` = minutes between check-in and check-out ÷ 60, 2 dp | |
| `summary` / `report` "absent" = elapsed weekdays in the month − (present + late + half-day + on-leave) | `MonthRange.weekdaysElapsed` |
| Dashboard `absentToday` = `totalEmployees − presentToday − onLeaveToday` | |

---

## Architecture

```
controller/   AttendanceController        employee check-in/out + own data
              AttendanceHrController      /attendance/hr/** (HR only)
              EmployeeProfileController   profile sync + /me
service/      AttendanceService(+Impl)    business rules & aggregation
              EmployeeProfileService(+Impl)
              support/MonthRange          month bounds + elapsed-weekday count
repository/    AttendanceRepository, EmployeeProfileRepository
entity/       Attendance, EmployeeProfile, AttendanceStatus (enum)
dto/          request/ProfileSyncRequest ; response/* (records, entities never leak)
security/     JwtService                  parse/verify authservice tokens
              JwtAuthFilter               per-request auth from JWT claims
              AuthenticatedUser           principal record
              CurrentUserProvider         X-User-Id vs JWT guard
              JwtAuthenticationEntryPoint / RestAccessDeniedHandler  (JSON 401 / 403)
config/       SecurityConfig, OpenApiConfig
exception/    GlobalExceptionHandler + AttendanceException / ResourceNotFound / UnauthorizedAccess
```

---

## Design notes / deviations from the spec

1. **`EmployeeProfile` has no create endpoint in the spec.** Added
   `POST /attendance/profiles` (HR only) to sync identity data from authservice,
   and a minimal profile row is auto-created on first check-in from the JWT
   claims (`userId`, `employeeId`, `email`; name / department filled in by a
   later sync). `GET /attendance/profiles/me` was added to read it back.
2. **Identity comes from the JWT, not `X-User-Id`.** The header is still accepted
   (and documented in Swagger) but only as a hint; a non-HR caller sending a
   mismatching `X-User-Id` gets `403`.
3. **No token blacklist here.** The blacklist lives in authservice's database;
   this service validates signature + expiry only. Revisit if a shared token
   store or introspection endpoint is added.
4. **`ABSENT` / `WEEKEND` / `ON_LEAVE` rows are not generated** by this service —
   it only writes a row when someone checks in. "Absent" figures in the summary,
   report and dashboard are therefore *derived* from elapsed weekdays. `ON_LEAVE`
   will come from `leaveservice` later.
5. **`today` returns a stub** (`status = ABSENT`, `checkedIn = false`) instead of
   `404` when there is no record yet, so the frontend can render a check-in button.
