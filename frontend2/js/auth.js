const AUTH_URL = "http://localhost:8081";
const ATTENDANCE_URL = "http://localhost:8082";
const LEAVE_URL = "http://localhost:8083";

function saveTokens(data) {
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  localStorage.setItem("userId", data.userId);
  localStorage.setItem("role", data.role);
  localStorage.setItem("employeeId", data.employeeId);
  localStorage.setItem("firstName", data.firstName);
  // save these when the login response already carries them; login.html also
  // calls /auth/me afterwards to fill any that are missing
  if (data.lastName) localStorage.setItem("lastName", data.lastName);
  if (data.department) localStorage.setItem("department", data.department);
}
// pull firstName / lastName / department from the GET /auth/me response
function saveProfile(profile) {
  if (!profile) return;
  if (profile.firstName) localStorage.setItem("firstName", profile.firstName);
  if (profile.lastName) localStorage.setItem("lastName", profile.lastName);
  if (profile.department) localStorage.setItem("department", profile.department);
  if (profile.employeeId) localStorage.setItem("employeeId", profile.employeeId);
}
function getToken() { return localStorage.getItem("accessToken"); }
function getRole() { return localStorage.getItem("role"); }
function getUserId() { return localStorage.getItem("userId"); }
function getFirstName() { return localStorage.getItem("firstName"); }
function getLastName() { return localStorage.getItem("lastName"); }
function getDepartment() { return localStorage.getItem("department"); }
function getEmployeeId() { return localStorage.getItem("employeeId"); }
function logout() { localStorage.clear(); window.location.href = "login.html"; }
function checkAuth() { if (!getToken()) window.location.href = "login.html"; }
function checkHR() { if (getRole() !== "HR") window.location.href = "employee-dashboard.html"; }
