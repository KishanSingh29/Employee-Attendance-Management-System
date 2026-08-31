# AuthService

Authentication & user-management microservice for the **Employee Attendance Management System**.

- **Port:** `8081`
- **Database:** MySQL `authservice_db` (auto-created on first run)
- **Stack:** Spring Boot 3.3, Java 21, Spring Security + JWT, JPA/Hibernate, Lombok, springdoc-openapi

---

## Running

### Prerequisites
- JDK 21 on `PATH` / `JAVA_HOME`
- MySQL running on `localhost:3306` with user `root` / password `root`

> The bundled Maven wrapper (`mvnw`) still launches Maven with whatever JDK
> `JAVA_HOME` points at. Make sure that is **JDK 21**, e.g. on Windows:
> `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`

```bash
./mvnw spring-boot:run
```

or build and run the jar:

```bash
./mvnw clean package
java -jar target/authservice-0.0.1-SNAPSHOT.jar
```

### Swagger UI
`http://localhost:8081/swagger-ui.html` — OpenAPI JSON at `/v3/api-docs`.
Use the **Authorize** button and paste a raw access token (no `Bearer ` prefix).

---

## Endpoints

| Method | Path              | Access            | Description                                   |
|--------|-------------------|-------------------|-----------------------------------------------|
| POST   | `/auth/register`  | public            | Register an employee, auto-generates `EMPxxx` |
| POST   | `/auth/login`     | public            | Returns access + refresh tokens               |
| POST   | `/auth/refresh`   | public\*          | New access token from a valid refresh token   |
| GET    | `/auth/me`        | EMPLOYEE, HR      | Current user's profile                        |
| GET    | `/auth/employees` | HR only           | List all employees                            |
| POST   | `/auth/logout`    | EMPLOYEE, HR      | Blacklists the access token, revokes refresh  |

\* See "Design notes" — `/auth/refresh` is `permitAll` because it must work when
the access token has already expired; it is secured by the refresh token itself.

### Sample requests

```bash
# register
curl -X POST localhost:8081/auth/register -H 'Content-Type: application/json' -d '{
  "firstName": "Kishan", "lastName": "Singh",
  "email": "kishan@gmail.com", "password": "password123",
  "role": "EMPLOYEE", "department": "IT"
}'

# login
curl -X POST localhost:8081/auth/login -H 'Content-Type: application/json' -d '{
  "email": "kishan@gmail.com", "password": "password123"
}'

# me
curl localhost:8081/auth/me -H "Authorization: Bearer <accessToken>"
```

---

## Architecture

```
controller/   AuthController                 REST layer, validation, Swagger
service/      AuthService / AuthServiceImpl  business logic
              RefreshTokenService            opaque refresh-token lifecycle
              EmployeeIdGenerator            EMP001, EMP002, ...
repository/    UserRepository, RefreshTokenRepository, BlacklistedTokenRepository
entity/       User, RefreshToken, BlacklistedToken, Role (enum)
dto/          request/*  response/*          records, never expose entities
security/     JwtService                     issue / parse HS256 access tokens
              JwtAuthFilter                  per-request token validation
              CustomUserDetailsService, UserPrincipal
              JwtAuthenticationEntryPoint    JSON 401
              RestAccessDeniedHandler        JSON 403
config/       SecurityConfig                 filter chain, RBAC, CORS, stateless
              OpenApiConfig                  bearer-auth scheme
exception/    GlobalExceptionHandler         @RestControllerAdvice, typed errors
```

### Security
- **Passwords:** BCrypt (`BCryptPasswordEncoder`), min 8 chars enforced by bean validation.
- **Access token:** HS256 JWT, 1 h expiry, subject = `userId`, carries `role`,
  `employeeId`, `email`, and a `jti` used for blacklisting.
- **Refresh token:** opaque UUID persisted in `refresh_tokens` with a 24 h expiry
  and a `revoked` flag; verified against the DB on every `/auth/refresh`.
- **Logout:** access-token `jti` is written to `blacklisted_tokens` and all of the
  user's refresh tokens are revoked. `JwtAuthFilter` rejects blacklisted JTIs.
- **Session:** stateless; CSRF disabled; CORS open for local development
  (tighten `SecurityConfig#corsConfigurationSource` for production).
- **Errors:** `401` (unauthenticated), `403` (wrong role), `400` (validation),
  `409` (duplicate email) all return the same `ErrorResponse` JSON shape.

---

## Design notes / deviations from the spec

1. **`id` vs `userId`.** The spec lists `id (Long, auto)` on `User` but every
   response returns `"userId": "uuid"`. Kept the numeric `id` as the primary key
   and added a separate immutable `userId` UUID column, which is what the API
   exposes. The JWT subject is `userId` so tokens never leak the PK.
2. **`/auth/refresh` is public.** The endpoint contract shows only
   `{ "refreshToken": "uuid" }` and no `Authorization` header, and a refresh call
   is pointless once the access token is still valid. It is therefore `permitAll`
   and trusts the DB-backed refresh token.
3. **`POST /auth/logout` was added.** It is not in the endpoint list but is
   required by the "token blacklist on logout" security requirement.
4. **Login is by email.** `CustomUserDetailsService` keys on `userId` (to match
   the JWT subject), so `AuthServiceImpl.login` verifies the email + BCrypt
   password directly and throws `BadCredentialsException` on mismatch.
5. **Employee id sequence** is derived from the current maximum and is safe up to
   `EMP999` for this project's scale; the unique constraint is the final guard.
