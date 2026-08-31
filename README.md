# Employee Attendance Management System

Microservices-based attendance management platform.

| Service             | Port | Status         | Description                              |
|---------------------|------|----------------|------------------------------------------|
| `authservice`       | 8081 | ✅ implemented  | Auth, JWT, users, role-based access      |
| `attendanceservice` | 8082 | ✅ implemented  | Check-in / check-out, attendance reports |
| `leaveservice`      | 8083 | ✅ implemented  | Leave apply / approval, balances, deductions |

Setup and API details:
[`authservice/README.md`](authservice/README.md) ·
[`attendanceservice/README.md`](attendanceservice/README.md) ·
[`leaveservice/README.md`](leaveservice/README.md)
