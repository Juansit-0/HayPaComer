const KEYS = { access: "hpc.access", refresh: "hpc.refresh", household: "hpc.household" };

export class ApiError extends Error {
  constructor(problem, status) {
    super(problem.detail || problem.title || "The request failed");
    this.status = status;
    this.problem = problem;
  }
}

export const session = {
  get access() { return sessionStorage.getItem(KEYS.access); },
  get refresh() { return sessionStorage.getItem(KEYS.refresh); },
  get household() { return sessionStorage.getItem(KEYS.household); },
  set household(id) { sessionStorage.setItem(KEYS.household, id); },
  store(tokens) {
    sessionStorage.setItem(KEYS.access, tokens.accessToken);
    sessionStorage.setItem(KEYS.refresh, tokens.refreshToken);
  },
  clear() { Object.values(KEYS).forEach((key) => sessionStorage.removeItem(key)); },
  get signedIn() { return Boolean(this.access); },
};

async function send(path, { method = "GET", body, auth = true } = {}) {
  const headers = { Accept: "application/json, application/problem+json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth && session.access) headers.Authorization = `Bearer ${session.access}`;
  return fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

async function refreshTokens() {
  if (!session.refresh) return false;
  const response = await send("/auth/refresh", {
    method: "POST",
    body: { refreshToken: session.refresh },
    auth: false,
  });
  if (!response.ok) return false;
  session.store(await response.json());
  return true;
}

export async function api(path, options = {}) {
  let response = await send(path, options);
  if (response.status === 401 && options.auth !== false && (await refreshTokens())) {
    response = await send(path, options);
  }
  if (response.status === 401 && options.auth !== false) {
    session.clear();
    window.dispatchEvent(new Event("hpc:signed-out"));
  }
  if (response.status === 204) return null;
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) throw new ApiError(payload || {}, response.status);
  return payload;
}
