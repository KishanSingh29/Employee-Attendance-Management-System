-- =====================================================================
--  ATTENDTRACK DATABASE SCHEMA
--  Employee Attendance Management System
-- =====================================================================
--  Three independent databases, one per microservice. There are NO
--  cross-database foreign keys (MySQL cannot enforce them and the
--  services are deliberately decoupled). The only real FK is inside
--  authservice_db (refresh_tokens -> users).
--
--  Column names, lengths and types below mirror what Hibernate
--  (Spring Boot 3.3.5 / Hibernate 6, MySQL8Dialect) generates from the
--  JPA entities:
--    - Long                -> BIGINT (AUTO_INCREMENT for @GeneratedValue IDENTITY)
--    - int                 -> INT
--    - Double              -> DOUBLE            (Hibernate emits "float(53)", a synonym)
--    - boolean             -> BIT(1)
--    - String(length = n)  -> VARCHAR(n)
--    - String (no length)  -> VARCHAR(255)
--    - enum @Enumerated(STRING) -> VARCHAR(n) + CHECK (…)
--    - LocalDate           -> DATE
--    - LocalTime           -> TIME(6)
--    - LocalDateTime / Instant -> DATETIME(6)
--
--  Cross-service link: every "user_id VARCHAR(36)" column holds the
--  public UUID from authservice_db.users.user_id (propagated by the
--  Kafka "user-registered" event), NOT the numeric users.id.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
--  1. authservice_db   (port 8081 - identity, JWT, user directory)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS authservice_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE authservice_db;


-- ---------------------------------------------------------------------
--  users
--  Master record for every employee. `user_id` is the stable public
--  identifier used across all services; `employee_id` (EMP001, EMP002…)
--  is the human-facing code. `salary` feeds the leave-deduction maths
--  and is shipped to leaveservice on the user-registered event.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(36)  NOT NULL COMMENT 'public UUID, referenced by every other service',
    employee_id  VARCHAR(20)  NOT NULL COMMENT 'sequential human id: EMP001, EMP002, ...',
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    email        VARCHAR(120) NOT NULL COMMENT 'login identifier, stored lower-cased',
    password     VARCHAR(255) NOT NULL COMMENT 'BCrypt hash',
    role         VARCHAR(20)  NOT NULL COMMENT 'EMPLOYEE | HR',
    department   VARCHAR(60)  NOT NULL,
    salary       DOUBLE       NULL     COMMENT 'monthly salary; nullable for legacy rows',
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_user_id     UNIQUE (user_id),
    CONSTRAINT uk_users_employee_id UNIQUE (employee_id),
    CONSTRAINT uk_users_email       UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('EMPLOYEE', 'HR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Employee master data owned by authservice';


-- ---------------------------------------------------------------------
--  refresh_tokens
--  One row per issued refresh token (opaque UUID handed to the client).
--  Login issues a new one; logout / rotation sets `revoked = 1`.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    token      VARCHAR(36) NOT NULL COMMENT 'opaque UUID given to the client',
    user_id    BIGINT      NOT NULL COMMENT 'FK -> users.id (numeric key, not the UUID)',
    expires_at DATETIME(6) NOT NULL COMMENT 'issue time + 24h',
    revoked    BIT(1)      NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Active/revoked refresh tokens per user';


-- ---------------------------------------------------------------------
--  blacklisted_tokens
--  Access-token JTIs invalidated by an explicit logout. Rows may be
--  purged once `expires_at` has passed (the JWT is rejected anyway).
-- ---------------------------------------------------------------------
CREATE TABLE blacklisted_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    jti        VARCHAR(64) NOT NULL COMMENT 'JWT id of a logged-out access token',
    expires_at DATETIME(6) NOT NULL COMMENT 'original token expiry',
    PRIMARY KEY (id),
    CONSTRAINT uk_blacklisted_tokens_jti UNIQUE (jti),
    INDEX idx_blacklisted_tokens_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Logout blacklist for access-token JTIs';


-- =====================================================================
--  2. attendanceservice_db   (port 8082 - check-in/out, reports)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS attendanceservice_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE attendanceservice_db;


-- ---------------------------------------------------------------------
--  employee_profiles
--  Local mirror of identity data owned by authservice. Populated by the
--  Kafka "user-registered" consumer, and lazily on first check-in.
--  `user_id` logically references authservice_db.users.user_id.
-- ---------------------------------------------------------------------
CREATE TABLE employee_profiles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     VARCHAR(36)  NOT NULL COMMENT 'public UUID from authservice',
    employee_id VARCHAR(20)  NOT NULL,
    first_name  VARCHAR(50)  NULL,
    last_name   VARCHAR(50)  NULL,
    email       VARCHAR(120) NULL,
    department  VARCHAR(60)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_profiles_user_id     UNIQUE (user_id),
    CONSTRAINT uk_employee_profiles_employee_id UNIQUE (employee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Replicated employee identity (source of truth: authservice)';


-- ---------------------------------------------------------------------
--  attendance
--  One row per employee per calendar day. `check_in` after the
--  09:30 threshold => status LATE; working_hours < 4 on check-out
--  => HALF_DAY. Unique (user_id, date) guarantees a single row/day.
-- ---------------------------------------------------------------------
CREATE TABLE attendance (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       VARCHAR(36) NOT NULL COMMENT 'public UUID from authservice',
    date          DATE        NOT NULL,
    check_in      TIME(6)     NULL,
    check_out     TIME(6)     NULL,
    working_hours DOUBLE      NULL COMMENT 'set on check-out, rounded to 2 dp',
    status        VARCHAR(20) NOT NULL COMMENT 'PRESENT | ABSENT | LATE | HALF_DAY | ON_LEAVE | WEEKEND',
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_user_date UNIQUE (user_id, date),
    CONSTRAINT chk_attendance_status CHECK
        (status IN ('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'ON_LEAVE', 'WEEKEND')),
    INDEX idx_attendance_date (date),
    INDEX idx_attendance_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Daily attendance records';


-- =====================================================================
--  3. leaveservice_db   (port 8083 - leave workflow, balances, payroll)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS leaveservice_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leaveservice_db;


-- ---------------------------------------------------------------------
--  leave_balance
--  One row per employee per calendar year. Seeded by the Kafka
--  "user-registered" consumer (pl_total 12, sl_total 6, monthly_salary
--  from the event, default 30000). `monthly_salary` is editable by HR
--  and drives the unpaid-leave deduction (perDay = monthly_salary / 26).
-- ---------------------------------------------------------------------
CREATE TABLE leave_balance (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        VARCHAR(36) NOT NULL COMMENT 'public UUID from authservice',
    year           INT         NOT NULL COMMENT 'calendar year this balance applies to',
    pl_total       INT         NOT NULL DEFAULT 12 COMMENT 'paid-leave allowance',
    pl_used        INT         NOT NULL DEFAULT 0,
    sl_total       INT         NOT NULL DEFAULT 6  COMMENT 'sick-leave allowance',
    sl_used        INT         NOT NULL DEFAULT 0,
    unpaid_used    INT         NOT NULL DEFAULT 0  COMMENT 'approved unpaid days (reporting)',
    monthly_salary DOUBLE      NOT NULL DEFAULT 30000.0,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_balance_user_year UNIQUE (user_id, year),
    INDEX idx_balance_year (year),
    INDEX idx_balance_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Yearly PL/SL allowance + monthly salary per employee';


-- ---------------------------------------------------------------------
--  leave_request
--  (Hibernate table name is singular: "leave_request".)
--  Every leave application. `total_days` excludes weekends. On approval
--  the matching leave_balance counter is incremented; `remarks` holds
--  the HR rejection reason shown back to the employee.
-- ---------------------------------------------------------------------
CREATE TABLE leave_request (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     VARCHAR(36)  NOT NULL COMMENT 'applicant public UUID',
    employee_id VARCHAR(20)  NULL COMMENT 'denormalised for HR reports',
    first_name  VARCHAR(50)  NULL COMMENT 'denormalised for HR reports',
    last_name   VARCHAR(50)  NULL COMMENT 'denormalised for HR reports',
    leave_type  VARCHAR(20)  NOT NULL COMMENT 'PAID | SICK | UNPAID',
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    total_days  INT          NOT NULL COMMENT 'working days in range (weekends excluded)',
    reason      VARCHAR(500) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED',
    approved_by VARCHAR(36)  NULL COMMENT 'HR user_id that approved/rejected',
    remarks     VARCHAR(500) NULL COMMENT 'HR note, currently the rejection reason',
    applied_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_leave_type   CHECK (leave_type IN ('PAID', 'SICK', 'UNPAID')),
    CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    INDEX idx_leave_user   (user_id),
    INDEX idx_leave_status (status),
    INDEX idx_leave_start  (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
  COMMENT = 'Leave applications and their approval state';


-- =====================================================================
--  End of schema. Load database/sample-data.sql next for demo data.
-- =====================================================================
