import { t } from "./i18n.js";

const KEYS = { access: "hpc.access", refresh: "hpc.refresh", household: "hpc.household" };
const LOCK = "hpc.refresh";
const EARLY_SECONDS = 30;

export class ApiError extends Error {
  constructor(problem, status) {
    super(problem.detail || problem.title || t("error.request"));
    this.status = status;
    this.problem = problem;
  }
}

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    return;
  }
}

function expiresAt(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(payload)).exp * 1000;
  } catch {
    return 0;
  }
}

export const session = {
  get access() { return read(KEYS.access); },
  get refresh() { return read(KEYS.refresh); },
  get household() { return read(KEYS.household); },
  set household(id) { write(KEYS.household, id); },
  store(tokens) {
    write(KEYS.access, tokens.accessToken);
    write(KEYS.refresh, tokens.refreshToken);
  },
  clear() { Object.values(KEYS).forEach((key) => write(key, null)); },
  get signedIn() { return Boolean(this.refresh || this.access); },
};

function signedOut() {
  session.clear();
  window.dispatchEvent(new Event("hpc:signed-out"));
}

async function exchange(used) {
  if (session.refresh !== used) return Boolean(session.access);
  const response = await fetch("/api/v1/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ refreshToken: used }),
  });
  if (response.status === 401 || response.status === 400) {
    if (session.refresh === used) signedOut();
    return false;
  }
  if (!response.ok) return false;
  session.store(await response.json());
  return true;
}

let pending = null;

function refreshTokens() {
  const used = session.refresh;
  if (!used) return Promise.resolve(false);
  if (!pending) {
    const run = () => exchange(used);
    pending = (navigator.locks ? navigator.locks.request(LOCK, run) : run())
      .catch(() => false)
      .finally(() => { pending = null; });
  }
  return pending;
}

export async function freshAccess() {
  const access = session.access;
  if (access && expiresAt(access) - EARLY_SECONDS * 1000 > Date.now()) return access;
  await refreshTokens();
  return session.access;
}

async function send(path, { method = "GET", body, auth = true } = {}) {
  const headers = { Accept: "application/json, application/problem+json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth) {
    const access = await freshAccess();
    if (access) headers.Authorization = `Bearer ${access}`;
  }
  return fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export async function api(path, options = {}) {
  let response = await send(path, options);
  if (response.status === 401 && options.auth !== false && (await refreshTokens())) {
    response = await send(path, options);
  }
  if (response.status === 401 && options.auth !== false) signedOut();
  if (response.status === 204) return null;
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) throw new ApiError(payload || {}, response.status);
  return payload;
}

window.addEventListener("storage", (event) => {
  if (event.key === KEYS.refresh) {
    window.dispatchEvent(new CustomEvent("hpc:session-changed", { detail: Boolean(event.newValue) }));
  }
  if (event.key === null) window.dispatchEvent(new CustomEvent("hpc:session-changed", { detail: false }));
});
