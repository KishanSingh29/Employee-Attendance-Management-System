-- =====================================================================
--  ATTENDTRACK  --  SAMPLE / DEMO DATA
-- =====================================================================
--  Run AFTER database/schema.sql.
--
--  Login password for EVERY sample user below: password123
--  (BCrypt hash, strength 10 - accepted by Spring Security's
--   BCryptPasswordEncoder. Regenerate if your build rejects it.)
--
--  5 employees:
--    EMP001  Neha Rao        HR         hr / neha.rao@attendtrack.io
--    EMP002  Arjun Mehta     HR         operations / arjun.mehta@attendtrack.io
--    EMP003  Priya Menon     EMPLOYEE   IT / priya.menon@attendtrack.io
--    EMP004  Rahul Iyer      EMPLOYEE   sales / rahul.iyer@attendtrack.io
--    EMP005  Sneha Kulkarni  EMPLOYEE   finance / sneha.kulkarni@attendtrack.io
--
--  Fixed UUIDs (the cross-service key):
--    EMP001 = 11111111-1111-1111-1111-111111111111
--    EMP002 = 22222222-2222-2222-2222-222222222222
--    EMP003 = 33333333-3333-3333-3333-333333333333
--    EMP004 = 44444444-4444-4444-4444-444444444444
--    EMP005 = 55555555-5555-5555-5555-555555555555
--
--  Dates are relative to CURDATE() so the data always looks "recent".
--  This script is re-runnable (each section truncates first).
-- =====================================================================

SET NAMES utf8mb4;


-- =====================================================================
--  1. authservice_db
-- =====================================================================
USE authservice_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE blacklisted_tokens;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---- users ----------------------------------------------------------
INSERT INTO users
    (user_id, employee_id, first_name, last_name, email, password, role, department, salary, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP001', 'Neha',  'Rao',
     'neha.rao@attendtrack.io',
     '$2a$10$5hAcnUB7wSweIR57HWc/AutljF9OulHhVJyDDPwN/OXGixNjULT4e',
     'HR', 'HR', 90000.0, '2026-01-05 09:15:00.000000'),

    ('22222222-2222-2222-2222-222222222222', 'EMP002', 'Arjun', 'Mehta',
     'arjun.mehta@attendtrack.io',
     '$2a$10$5hAcnUB7wSweIR57HWc/AutljF9OulHhVJyDDPwN/OXGixNjULT4e',
     'HR', 'Operations', 85000.0, '2026-01-05 09:15:00.000000'),

    ('33333333-3333-3333-3333-333333333333', 'EMP003', 'Priya', 'Menon',
     'priya.menon@attendtrack.io',
     '$2a$10$5hAcnUB7wSweIR57HWc/AutljF9OulHhVJyDDPwN/OXGixNjULT4e',
     'EMPLOYEE', 'IT', 55000.0, '2026-01-06 10:00:00.000000'),

    ('44444444-4444-4444-4444-444444444444', 'EMP004', 'Rahul', 'Iyer',
     'rahul.iyer@attendtrack.io',
     '$2a$10$5hAcnUB7wSweIR57HWc/AutljF9OulHhVJyDDPwN/OXGixNjULT4e',
     'EMPLOYEE', 'Sales', 48000.0, '2026-01-06 10:05:00.000000'),

    ('55555555-5555-5555-5555-555555555555', 'EMP005', 'Sneha', 'Kulkarni',
     'sneha.kulkarni@attendtrack.io',
     '$2a$10$5hAcnUB7wSweIR57HWc/AutljF9OulHhVJyDDPwN/OXGixNjULT4e',
     'EMPLOYEE', 'Finance', 52000.0, '2026-01-07 09:45:00.000000');

-- ---- refresh_tokens (FK -> users.id) -------------------------------
INSERT INTO refresh_tokens (token, user_id, expires_at, revoked)
VALUES
    ('a1a1a1a1-active-priya-000000000001',
     (SELECT id FROM users WHERE employee_id = 'EMP003'),
     DATE_ADD(NOW(6), INTERVAL 24 HOUR), 0),

    ('b2b2b2b2-active-neha-0000000000002',
     (SELECT id FROM users WHERE employee_id = 'EMP001'),
     DATE_ADD(NOW(6), INTERVAL 24 HOUR), 0),

    ('c3c3c3c3-revoked-rahul-00000000003',
     (SELECT id FROM users WHERE employee_id = 'EMP004'),
     DATE_SUB(NOW(6), INTERVAL 2 DAY), 1);

-- ---- blacklisted_tokens ------------------------------------------
INSERT INTO blacklisted_tokens (jti, expires_at)
VALUES
    ('jti-logout-sample-000000000000001', DATE_ADD(NOW(6), INTERVAL 30 MINUTE));


-- =====================================================================
--  2. attendanceservice_db
-- =====================================================================
USE attendanceservice_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE attendance;
TRUNCATE TABLE employee_profiles;
SET FOREIGN_KEY_CHECKS = 1;

-- ---- employee_profiles (mirror of authservice.users) --------------
INSERT INTO employee_profiles
    (user_id, employee_id, first_name, last_name, email, department, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP001', 'Neha',  'Rao',      'neha.rao@attendtrack.io',       'HR',         '2026-01-05 09:15:01.000000'),
    ('22222222-2222-2222-2222-222222222222', 'EMP002', 'Arjun', 'Mehta',    'arjun.mehta@attendtrack.io',    'Operations',  '2026-01-05 09:15:01.000000'),
    ('33333333-3333-3333-3333-333333333333', 'EMP003', 'Priya', 'Menon',    'priya.menon@attendtrack.io',    'IT',          '2026-01-06 10:00:01.000000'),
    ('44444444-4444-4444-4444-444444444444', 'EMP004', 'Rahul', 'Iyer',     'rahul.iyer@attendtrack.io',     'Sales',       '2026-01-06 10:05:01.000000'),
    ('55555555-5555-5555-5555-555555555555', 'EMP005', 'Sneha', 'Kulkarni', 'sneha.kulkarni@attendtrack.io', 'Finance',     '2026-01-07 09:45:01.000000');

-- ---- attendance : last 5 days (CURDATE()-4 .. CURDATE()) ----------
-- Priya (EMP003): present / late / present / half-day / checked-in-only
INSERT INTO attendance (user_id, date, check_in, check_out, working_hours, status, created_at) VALUES
    ('33333333-3333-3333-3333-333333333333', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:05:00', '18:15:00', 9.17, 'PRESENT',  NOW(6)),
    ('33333333-3333-3333-3333-333333333333', DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:48:00', '18:40:00', 8.87, 'LATE',     NOW(6)),
    ('33333333-3333-3333-3333-333333333333', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:58:00', '18:05:00', 9.12, 'PRESENT',  NOW(6)),
    ('33333333-3333-3333-3333-333333333333', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:12:00', '12:50:00', 3.63, 'HALF_DAY', NOW(6)),
    ('33333333-3333-3333-3333-333333333333', CURDATE(),                           '09:02:00', NULL,       NULL, 'PRESENT',  NOW(6));

-- Rahul (EMP004): present / absent / present / late / present
INSERT INTO attendance (user_id, date, check_in, check_out, working_hours, status, created_at) VALUES
    ('44444444-4444-4444-4444-444444444444', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:20:00', '18:30:00', 9.17, 'PRESENT', NOW(6)),
    ('44444444-4444-4444-4444-444444444444', DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL,       NULL,       NULL, 'ABSENT',  NOW(6)),
    ('44444444-4444-4444-4444-444444444444', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:15:00', '18:20:00', 9.08, 'PRESENT', NOW(6)),
    ('44444444-4444-4444-4444-444444444444', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:05:00', '18:45:00', 8.67, 'LATE',    NOW(6)),
    ('44444444-4444-4444-4444-444444444444', CURDATE(),                           '09:10:00', '18:00:00', 8.83, 'PRESENT', NOW(6));

-- Sneha (EMP005): present / present / on-leave / on-leave / present
INSERT INTO attendance (user_id, date, check_in, check_out, working_hours, status, created_at) VALUES
    ('55555555-5555-5555-5555-555555555555', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:00:00', '18:00:00', 9.00, 'PRESENT',  NOW(6)),
    ('55555555-5555-5555-5555-555555555555', DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:03:00', '18:10:00', 9.12, 'PRESENT',  NOW(6)),
    ('55555555-5555-5555-5555-555555555555', DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL,       NULL,       NULL, 'ON_LEAVE', NOW(6)),
    ('55555555-5555-5555-5555-555555555555', DATE_SUB(CURDATE(), INTERVAL 1 DAY), NULL,       NULL,       NULL, 'ON_LEAVE', NOW(6)),
    ('55555555-5555-5555-5555-555555555555', CURDATE(),                           '09:07:00', '18:12:00', 9.08, 'PRESENT',  NOW(6));

-- HR staff (EMP001 / EMP002): a couple of recent days
INSERT INTO attendance (user_id, date, check_in, check_out, working_hours, status, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:30:00', '18:30:00', 9.00, 'PRESENT', NOW(6)),
    ('11111111-1111-1111-1111-111111111111', CURDATE(),                           '09:25:00', '18:20:00', 8.92, 'PRESENT', NOW(6)),
    ('22222222-2222-2222-2222-222222222222', CURDATE(),                           '09:55:00', '18:50:00', 8.92, 'LATE',    NOW(6));


-- =====================================================================
--  3. leaveservice_db
-- =====================================================================
USE leaveservice_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE leave_request;
TRUNCATE TABLE leave_balance;
SET FOREIGN_KEY_CHECKS = 1;

-- ---- leave_balance : current year -------------------------------
INSERT INTO leave_balance
    (user_id, year, pl_total, pl_used, sl_total, sl_used, unpaid_used, monthly_salary, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', YEAR(CURDATE()), 12, 0, 6, 0, 0, 90000.0, NOW(6)),
    ('22222222-2222-2222-2222-222222222222', YEAR(CURDATE()), 12, 0, 6, 0, 0, 85000.0, NOW(6)),
    ('33333333-3333-3333-3333-333333333333', YEAR(CURDATE()), 12, 3, 6, 0, 2, 55000.0, NOW(6)),  -- 3 PL + 2 unpaid used
    ('44444444-4444-4444-4444-444444444444', YEAR(CURDATE()), 12, 5, 6, 0, 0, 48000.0, NOW(6)),  -- 5 PL used
    ('55555555-5555-5555-5555-555555555555', YEAR(CURDATE()), 12, 0, 6, 2, 0, 52000.0, NOW(6));  -- 2 SL used

-- ---- leave_request : approved / pending / rejected -------------
INSERT INTO leave_request
    (user_id, employee_id, first_name, last_name, leave_type, start_date, end_date,
     total_days, reason, status, approved_by, remarks, applied_at, updated_at)
VALUES
    -- APPROVED  Priya - 3 paid days (last month)
    ('33333333-3333-3333-3333-333333333333', 'EMP003', 'Priya', 'Menon', 'PAID',
     DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 18 DAY),
     3, 'Family function out of town', 'APPROVED',
     '11111111-1111-1111-1111-111111111111', NULL,
     DATE_SUB(CURDATE(), INTERVAL 25 DAY), NOW(6)),

    -- APPROVED  Priya - 2 UNPAID days THIS month (drives the deduction)
    ('33333333-3333-3333-3333-333333333333', 'EMP003', 'Priya', 'Menon', 'UNPAID',
     DATE_FORMAT(CURDATE(), '%Y-%m-10'), DATE_FORMAT(CURDATE(), '%Y-%m-11'),
     2, 'Personal work, paid balance exhausted', 'APPROVED',
     '11111111-1111-1111-1111-111111111111', NULL,
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), NOW(6)),

    -- APPROVED  Sneha - 2 sick days (matches her ON_LEAVE attendance)
    ('55555555-5555-5555-5555-555555555555', 'EMP005', 'Sneha', 'Kulkarni', 'SICK',
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY),
     2, 'Viral fever, doctor advised rest', 'APPROVED',
     '22222222-2222-2222-2222-222222222222', NULL,
     DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW(6)),

    -- APPROVED  Rahul - 5 paid days (two weeks ago)
    ('44444444-4444-4444-4444-444444444444', 'EMP004', 'Rahul', 'Iyer', 'PAID',
     DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     5, 'Planned vacation', 'APPROVED',
     '11111111-1111-1111-1111-111111111111', NULL,
     DATE_SUB(CURDATE(), INTERVAL 22 DAY), NOW(6)),

    -- PENDING  Rahul - 3 paid days next week
    ('44444444-4444-4444-4444-444444444444', 'EMP004', 'Rahul', 'Iyer', 'PAID',
     DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),
     3, 'Cousin''s wedding', 'PENDING',
     NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY)),

    -- PENDING  Priya - 1 sick day (follow-up appointment)
    ('33333333-3333-3333-3333-333333333333', 'EMP003', 'Priya', 'Menon', 'SICK',
     DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
     1, 'Post-procedure follow-up with doctor', 'PENDING',
     NULL, NULL,
     CURDATE(), CURDATE()),

    -- REJECTED  Sneha - 3 paid days
    ('55555555-5555-5555-5555-555555555555', 'EMP005', 'Sneha', 'Kulkarni', 'PAID',
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY),
     3, 'Short trip with family', 'REJECTED',
     '22222222-2222-2222-2222-222222222222',
     'Team understaffed that week - please reschedule to next month.',
     DATE_SUB(CURDATE(), INTERVAL 7 DAY), NOW(6));


-- =====================================================================
--  Done. Sign in at the frontend with any address above / password123
-- =====================================================================
