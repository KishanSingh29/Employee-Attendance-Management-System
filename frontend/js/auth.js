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
}
function getToken() { return localStorage.getItem("accessToken"); }
function getRole() { return localStorage.getItem("role"); }
function getUserId() { return localStorage.getItem("userId"); }
function getFirstName() { return localStorage.getItem("firstName"); }
function getEmployeeId() { return localStorage.getItem("employeeId"); }
function logout() { localStorage.clear(); window.location.href = "login.html"; }
function checkAuth() { if (!getToken()) window.location.href = "login.html"; }
function checkHR() { if (getRole() !== "HR") window.location.href = "employee-dashboard.html"; }
