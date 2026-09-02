/* ============================================================
   utils.js — shared helpers (spec functions + shell wiring)
   ============================================================ */

function showToast(message, type = "success") {
  const t = document.createElement("div");
  t.className = `alert alert-${type} position-fixed shadow`;
  t.style.cssText = "top:20px;right:20px;z-index:9999;min-width:300px;";
  t.innerHTML = message;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}
function showLoading(btnId, text = "Loading...") {
  const b = document.getElementById(btnId);
  if (b) { b.disabled = true; b.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>${text}`; }
}
function hideLoading(btnId, text) {
  const b = document.getElementById(btnId);
  if (b) { b.disabled = false; b.innerHTML = text; }
}
function getStatusBadge(status) {
  const m = { PRESENT:"bg-success", ABSENT:"bg-danger", LATE:"bg-warning text-dark",
    HALF_DAY:"bg-orange text-dark", ON_LEAVE:"bg-info",
    PENDING:"bg-warning text-dark", APPROVED:"bg-success", REJECTED:"bg-danger" };
  return `<span class="badge ${m[status]||"bg-secondary"}">${status?.replace("_"," ")}</span>`;
}
function formatTime(t) { return t ? t.substring(0,5) : "-"; }
function formatDate(d) { return d ? new Date(d).toLocaleDateString("en-IN") : "-"; }
function getCurrentMonth() { return new Date().getMonth() + 1; }
function getCurrentYear() { return new Date().getFullYear(); }
function getTodayDate() { return new Date().toISOString().split("T")[0]; }

/* ---------- extra helpers used by the pages ---------- */

// resilient field getter: pick(obj, ["a","b","c"], fallback)
function pick(obj, keys, fallback = undefined) {
  if (!obj) return fallback;
  for (const k of (Array.isArray(keys) ? keys : [keys])) {
    if (obj[k] !== undefined && obj[k] !== null) return obj[k];
  }
  return fallback;
}

// numeric with fallback
function num(v, fallback = 0) {
  const n = Number(v);
  return Number.isFinite(n) ? n : fallback;
}

// extract a human message from an API error object / Error / string
function errMsg(err, fallback = "Something went wrong. Please try again.") {
  if (!err) return fallback;
  if (typeof err === "string") return err;
  if (err instanceof Error && err.message) {
    return /failed to fetch|networkerror/i.test(err.message)
      ? "Cannot reach the server. Check that the services are running."
      : err.message;
  }
  return err.message || err.error || err.detail ||
    (Array.isArray(err.errors) ? err.errors.join(", ") : null) || fallback;
}

// safe text -> html
function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"']/g, c => (
    { "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[c]
  ));
}

// initials from a full name or a first/last pair
function initials(a, b) {
  if (b !== undefined) return ((a?.[0] || "") + (b?.[0] || "")).toUpperCase() || "?";
  const p = String(a || "").trim().split(/\s+/);
  return ((p[0]?.[0] || "") + (p[1]?.[0] || "")).toUpperCase() || "?";
}

// INR currency
function formatMoney(n) {
  const v = num(n, 0);
  return "₹" + v.toLocaleString("en-IN");
}

// full date like "02 Sep 2026"
function formatDateLong(d) {
  if (!d) return "-";
  const dt = new Date(d);
  if (isNaN(dt)) return d;
  return dt.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

// weekday name
function dayName(d) {
  const dt = new Date(d);
  return isNaN(dt) ? "-" : dt.toLocaleDateString("en-IN", { weekday: "long" });
}

// hours value -> "8h 12m" ( accepts number of hours, or "HH:mm:ss", or minutes-ish )
function formatHours(v) {
  if (v === null || v === undefined || v === "") return "-";
  if (typeof v === "string" && v.includes(":")) {
    const [h, m] = v.split(":");
    return `${num(h)}h ${String(num(m)).padStart(2, "0")}m`;
  }
  const h = num(v, 0);
  if (h <= 0) return "-";
  const whole = Math.floor(h);
  const mins = Math.round((h - whole) * 60);
  return `${whole}h ${String(mins).padStart(2, "0")}m`;
}

// date range label
function formatRange(a, b) {
  if (!a) return "-";
  if (!b || a === b) return formatDateLong(a);
  return `${formatDateLong(a)} — ${formatDateLong(b)}`;
}

// inclusive day count between two ISO dates
function dayCount(a, b) {
  if (!a || !b) return "-";
  const d1 = new Date(a), d2 = new Date(b);
  if (isNaN(d1) || isNaN(d2)) return "-";
  return Math.round((d2 - d1) / 86400000) + 1;
}

// empty-state markup
function emptyState(title, sub) {
  return `<div class="empty"><div class="empty-ico"></div>
    <div class="empty-title">${escapeHtml(title)}</div>
    <div class="empty-sub">${escapeHtml(sub || "")}</div></div>`;
}

// standard table error row
function errorRow(colspan, message) {
  return `<tr><td colspan="${colspan}" style="text-align:center;color:var(--red-dark);padding:34px 20px">
    ${escapeHtml(message)}</td></tr>`;
}

// full-screen loading overlay control (expects <div id="overlay" class="overlay" hidden>)
function setOverlay(on, label) {
  const o = document.getElementById("overlay");
  if (!o) return;
  const l = o.querySelector(".overlay-label");
  if (l && label) l.textContent = label;
  o.hidden = !on;
}

/* ---------- app shell (sidebar + topbar) ---------- */

// call on every authenticated page. `active` = nav key, `title`/`sub` = topbar text.
function initShell(active, title, sub) {
  const role = getRole();
  const first = getFirstName() || "User";
  const last = getLastName() || "";
  const dept = getDepartment() || "";
  const empId = getEmployeeId() || "-";
  const isHR = role === "HR";

  const fullName = (first + " " + last).trim();
  const chipInitials = ((first[0] || "") + (last[0] || "")).toUpperCase() || "?";
  const chipSub = `${empId} · ${dept || role || "-"}`;

  const roleEl = document.getElementById("roleLabel");
  if (roleEl) roleEl.textContent = isHR ? "HR Console" : "Employee";

  document.querySelectorAll("[data-init]").forEach(el => el.textContent = chipInitials);
  const nameEls = document.querySelectorAll("[data-user-name]");
  nameEls.forEach(el => el.textContent = fullName);
  const idEls = document.querySelectorAll("[data-user-id]");
  idEls.forEach(el => el.textContent = empId);
  const metaEl = document.getElementById("tbMeta");
  if (metaEl) metaEl.textContent = chipSub;

  const t = document.getElementById("pageTitle");
  if (t && title) t.textContent = title;
  const s = document.getElementById("pageSub");
  if (s && sub) s.textContent = sub;

  // mark active nav item
  document.querySelectorAll(".nav-item").forEach(el => {
    el.classList.toggle("active", el.dataset.nav === active);
  });

  // hamburger toggle
  const app = document.querySelector(".app");
  const burger = document.getElementById("hamburger");
  if (burger && app) {
    burger.addEventListener("click", () => {
      app.classList.toggle("sidebar-collapsed");
      app.classList.toggle("sidebar-open");
    });
  }

  // logout
  const lo = document.getElementById("logoutBtn");
  if (lo) lo.addEventListener("click", async () => {
    try { await logoutEmployee(); } catch (e) { /* ignore */ }
    logout();
  });
}

// month <select> options 1..12
function monthOptions(selected) {
  const names = ["January","February","March","April","May","June","July",
    "August","September","October","November","December"];
  return names.map((n, i) =>
    `<option value="${i + 1}"${i + 1 === num(selected) ? " selected" : ""}>${n}</option>`).join("");
}
// year <select> options (current-2 .. current+1)
function yearOptions(selected) {
  const y = getCurrentYear();
  let out = "";
  for (let i = y - 2; i <= y + 1; i++) {
    out += `<option value="${i}"${i === num(selected) ? " selected" : ""}>${i}</option>`;
  }
  return out;
}
function monthName(m) {
  return ["January","February","March","April","May","June","July",
    "August","September","October","November","December"][num(m) - 1] || "";
}
