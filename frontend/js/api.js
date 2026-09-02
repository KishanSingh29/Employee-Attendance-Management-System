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
  } catch (err) {
    throw err;
  }
}

// AUTH (6 endpoints)
async function registerEmployee(body) {
  return apiCall(AUTH_URL + "/auth/register", "POST", body, false);
}
async function loginEmployee(body) {
  return apiCall(AUTH_URL + "/auth/login", "POST", body, false);
}
async function refreshToken(refreshToken) {
  return apiCall(AUTH_URL + "/auth/refresh", "POST", {refreshToken}, false);
}
async function getMyProfile() {
  return apiCall(AUTH_URL + "/auth/me", "GET", null, true);
}
async function getAllEmployees() {
  return apiCall(AUTH_URL + "/auth/employees", "GET", null, true);
}
async function logoutEmployee() {
  return apiCall(AUTH_URL + "/auth/logout", "POST", null, true);
}

// ATTENDANCE EMPLOYEE (5 endpoints)
async function checkIn() {
  return apiCall(ATTENDANCE_URL + "/attendance/checkin", "POST", null, true);
}
async function checkOut() {
  return apiCall(ATTENDANCE_URL + "/attendance/checkout", "POST", null, true);
}
async function getTodayAttendance() {
  return apiCall(ATTENDANCE_URL + "/attendance/today", "GET", null, true);
}
async function getAttendanceHistory(month, year) {
  return apiCall(ATTENDANCE_URL + `/attendance/history?month=${month}&year=${year}`, "GET", null, true);
}
async function getAttendanceSummary(month, year) {
  return apiCall(ATTENDANCE_URL + `/attendance/summary?month=${month}&year=${year}`, "GET", null, true);
}

// ATTENDANCE HR (4 endpoints)
async function hrGetDashboard() {
  return apiCall(ATTENDANCE_URL + "/attendance/hr/dashboard", "GET", null, true);
}
async function hrGetAllAttendance(date) {
  return apiCall(ATTENDANCE_URL + `/attendance/hr/all?date=${date}`, "GET", null, true);
}
async function hrGetEmployeeAttendance(userId, month, year) {
  return apiCall(ATTENDANCE_URL + `/attendance/hr/employee/${userId}?month=${month}&year=${year}`, "GET", null, true);
}
async function hrGetReport(month, year) {
  return apiCall(ATTENDANCE_URL + `/attendance/hr/report?month=${month}&year=${year}`, "GET", null, true);
}

// EMPLOYEE PROFILE (2 endpoints)
async function syncEmployeeProfile(body) {
  return apiCall(ATTENDANCE_URL + "/attendance/profiles", "POST", body, true);
}
async function getMyAttendanceProfile() {
  return apiCall(ATTENDANCE_URL + "/attendance/profiles/me", "GET", null, true);
}

// LEAVE EMPLOYEE (5 endpoints)
async function applyLeave(body) {
  return apiCall(LEAVE_URL + "/leave/apply", "POST", body, true);
}
async function getMyLeaves() {
  return apiCall(LEAVE_URL + "/leave/my-requests", "GET", null, true);
}
async function getLeaveBalance(year) {
  return apiCall(LEAVE_URL + `/leave/balance?year=${year}`, "GET", null, true);
}
async function getLeaveDeduction(month, year) {
  return apiCall(LEAVE_URL + `/leave/deduction?month=${month}&year=${year}`, "GET", null, true);
}
async function cancelLeave(leaveId) {
  return apiCall(LEAVE_URL + `/leave/cancel/${leaveId}`, "PUT", null, true);
}

// LEAVE HR (6 endpoints)
async function hrGetPendingLeaves() {
  return apiCall(LEAVE_URL + "/leave/hr/pending", "GET", null, true);
}
async function hrApproveLeave(leaveId) {
  return apiCall(LEAVE_URL + `/leave/hr/approve/${leaveId}`, "PUT", null, true);
}
async function hrRejectLeave(leaveId, reason) {
  return apiCall(LEAVE_URL + `/leave/hr/reject/${leaveId}`, "PUT", {reason}, true);
}
async function hrGetAllLeaves(status) {
  const url = status
    ? LEAVE_URL + `/leave/hr/all?status=${status}`
    : LEAVE_URL + "/leave/hr/all";
  return apiCall(url, "GET", null, true);
}
async function hrGetEmployeeLeaveBalance(userId, year) {
  return apiCall(LEAVE_URL + `/leave/hr/employee/${userId}/balance?year=${year}`, "GET", null, true);
}
async function hrGetLeaveReport(month, year) {
  return apiCall(LEAVE_URL + `/leave/hr/report?month=${month}&year=${year}`, "GET", null, true);
}
