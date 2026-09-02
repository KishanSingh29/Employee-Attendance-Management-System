async function apiCall(url, method = "GET", body = null, auth = true) {
  try {
    const headers = { "Content-Type": "application/json" };
    if (auth) headers["Authorization"] = "Bearer " + getToken();
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    const response = await fetch(url, options);
    if (response.status === 401) { logout(); return; }
    if (response.status === 204) return { success: true };
    const data = await response.json();
    if (!response.ok) throw data;
    return data;
  } catch (err) { throw err; }
}

// AUTH
async function registerEmployee(body) { return apiCall(AUTH_URL + "/auth/register", "POST", body, false); }
async function loginEmployee(body) { return apiCall(AUTH_URL + "/auth/login", "POST", body, false); }
async function refreshToken(rt) { return apiCall(AUTH_URL + "/auth/refresh", "POST", {refreshToken: rt}, false); }
async function getMyProfile() { return apiCall(AUTH_URL + "/auth/me"); }
async function getAllEmployees() { return apiCall(AUTH_URL + "/auth/employees"); }
async function logoutEmployee() { return apiCall(AUTH_URL + "/auth/logout", "POST"); }

// ATTENDANCE
async function checkIn() { return apiCall(ATTENDANCE_URL + "/attendance/checkin", "POST"); }
async function checkOut() { return apiCall(ATTENDANCE_URL + "/attendance/checkout", "POST"); }
async function getTodayAttendance() { return apiCall(ATTENDANCE_URL + "/attendance/today"); }
async function getAttendanceHistory(month, year) { return apiCall(ATTENDANCE_URL + `/attendance/history?month=${month}&year=${year}`); }
async function getAttendanceSummary(month, year) { return apiCall(ATTENDANCE_URL + `/attendance/summary?month=${month}&year=${year}`); }
async function hrGetDashboard() { return apiCall(ATTENDANCE_URL + "/attendance/hr/dashboard"); }
async function hrGetAllAttendance(date) { return apiCall(ATTENDANCE_URL + `/attendance/hr/all?date=${date}`); }
async function hrGetEmployeeAttendance(userId, month, year) { return apiCall(ATTENDANCE_URL + `/attendance/hr/employee/${userId}?month=${month}&year=${year}`); }
async function hrGetReport(month, year) { return apiCall(ATTENDANCE_URL + `/attendance/hr/report?month=${month}&year=${year}`); }

// LEAVE
async function applyLeave(body) { return apiCall(LEAVE_URL + "/leave/apply", "POST", body); }
async function getMyLeaves() { return apiCall(LEAVE_URL + "/leave/my-requests"); }
async function getLeaveBalance(year) { return apiCall(LEAVE_URL + `/leave/balance?year=${year}`); }
async function getLeaveDeduction(month, year) { return apiCall(LEAVE_URL + `/leave/deduction?month=${month}&year=${year}`); }
async function cancelLeave(id) { return apiCall(LEAVE_URL + `/leave/cancel/${id}`, "PUT"); }
async function hrGetPendingLeaves() { return apiCall(LEAVE_URL + "/leave/hr/pending"); }
async function hrApproveLeave(id) { return apiCall(LEAVE_URL + `/leave/hr/approve/${id}`, "PUT"); }
async function hrRejectLeave(id, reason) { return apiCall(LEAVE_URL + `/leave/hr/reject/${id}`, "PUT", {reason}); }
async function hrGetAllLeaves(status) { return apiCall(LEAVE_URL + `/leave/hr/all${status ? "?status="+status : ""}`); }
async function hrGetEmployeeLeaveBalance(userId, year) { return apiCall(LEAVE_URL + `/leave/hr/employee/${userId}/balance?year=${year}`); }
async function hrGetLeaveReport(month, year) { return apiCall(LEAVE_URL + `/leave/hr/report?month=${month}&year=${year}`); }
