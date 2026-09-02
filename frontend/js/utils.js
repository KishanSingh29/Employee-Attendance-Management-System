function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `alert alert-${type} position-fixed shadow`;
  toast.style.cssText = "top:20px;right:20px;z-index:9999;min-width:300px;";
  toast.innerHTML = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}
function showLoading(btnId, text = "Loading...") {
  const btn = document.getElementById(btnId);
  if (btn) { btn.disabled = true; btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>${text}`; }
}
function hideLoading(btnId, text) {
  const btn = document.getElementById(btnId);
  if (btn) { btn.disabled = false; btn.innerHTML = text; }
}
function getStatusBadge(status) {
  const map = {
    PRESENT: "bg-success", ABSENT: "bg-danger",
    LATE: "bg-warning text-dark", HALF_DAY: "bg-orange text-dark",
    ON_LEAVE: "bg-info", WEEKEND: "bg-secondary",
    PENDING: "bg-warning text-dark",
    APPROVED: "bg-success", REJECTED: "bg-danger"
  };
  return `<span class="badge ${map[status] || "bg-secondary"}">${status?.replace("_"," ")}</span>`;
}
function formatTime(t) { return t ? t.substring(0,5) : "-"; }
function formatDate(d) { return d ? new Date(d).toLocaleDateString("en-IN") : "-"; }
function getCurrentMonth() { return new Date().getMonth() + 1; }
function getCurrentYear() { return new Date().getFullYear(); }
function getTodayDate() { return new Date().toISOString().split("T")[0]; }

/* ---------- shared helpers ---------- */

// Extract a human-readable message from a thrown API error (ErrorResponse shape).
function apiError(err) {
  if (!err) return "Something went wrong. Please try again.";
  if (typeof err === "string") return err;
  if (err.fieldErrors && Object.keys(err.fieldErrors).length) {
    return Object.values(err.fieldErrors).join(", ");
  }
  return err.message || err.error || "Something went wrong. Please try again.";
}

// Logout: best-effort server call, then clear local session.
async function doLogout() {
  try { await logoutEmployee(); } catch (e) { /* ignore */ }
  logout();
}

// Populate the standard top navbar (#topName / #topEmpId / #greeting).
function fillTopbar() {
  const name = getFirstName() || "User";
  const nameEl = document.getElementById("topName");
  const empEl = document.getElementById("topEmpId");
  if (nameEl) nameEl.textContent = name;
  if (empEl) empEl.textContent = getEmployeeId() || "-";
}

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good Morning";
  if (h < 17) return "Good Afternoon";
  return "Good Evening";
}

function todayLong() {
  return new Date().toLocaleDateString("en-IN", {
    weekday: "long", day: "numeric", month: "long", year: "numeric"
  });
}

function hours(v) {
  return (v === null || v === undefined) ? "-" : Number(v).toFixed(2) + " hrs";
}

// Toggle the mobile sidebar.
function toggleSidebar() {
  document.querySelector(".sidebar")?.classList.toggle("open");
}

// <option> list of months / years for filter selects.
function monthOptions(selected) {
  const names = ["January","February","March","April","May","June",
    "July","August","September","October","November","December"];
  return names.map((n, i) =>
    `<option value="${i + 1}" ${i + 1 === Number(selected) ? "selected" : ""}>${n}</option>`
  ).join("");
}
function yearOptions(selected) {
  const y = getCurrentYear();
  let out = "";
  for (let i = y - 4; i <= y + 1; i++) {
    out += `<option value="${i}" ${i === Number(selected) ? "selected" : ""}>${i}</option>`;
  }
  return out;
}
