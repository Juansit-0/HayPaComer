const STORE = "hpc.locale";
const CACHE = "hpc.texts.";
const FALLBACK = "es-CO";

let texts = {};
let locale = FALLBACK;
let available = [];

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    return;
  }
}

export function t(key, params = {}) {
  const text = texts[key] ?? key;
  return text.replace(/\{(\w+)\}/g, (whole, name) => (name in params ? String(params[name]) : whole));
}

export function plural(key, count, params = {}) {
  const rule = new Intl.PluralRules(locale).select(count);
  const chosen = texts[`${key}.${rule}`] !== undefined ? `${key}.${rule}` : `${key}.other`;
  return t(chosen, { n: formatNumber(count), ...params });
}

export function foodName(name) {
  if (!name) return "";
  return texts[`food.${String(name).trim().toLowerCase()}`] ?? name;
}

export function placeName(name) {
  return texts[`place.${String(name ?? "").trim().toLowerCase()}`] ?? name;
}

export function currentLocale() {
  return locale;
}

export function locales() {
  return available;
}

export function formatNumber(value, digits = 0) {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(Number(value));
}

export function formatMoney(value, currency) {
  return new Intl.NumberFormat(locale, { style: "currency", currency, maximumFractionDigits: 0 }).format(
    Number(value),
  );
}

export function formatDay(iso, options = { day: "numeric", month: "short" }) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString(locale, options);
}

export function formatTime(iso) {
  return new Date(iso).toLocaleTimeString(locale, { hour: "2-digit", minute: "2-digit" });
}

async function fetchTexts(code) {
  const cached = read(CACHE + code);
  const known = cached ? JSON.parse(cached) : null;
  const response = await fetch(`/api/v1/i18n/${encodeURIComponent(code)}`, {
    headers: known?.etag ? { "If-None-Match": known.etag } : {},
    cache: "no-cache",
  }).catch(() => null);
  if (response?.status === 304 && known) return known.texts;
  if (response?.ok) {
    const fresh = await response.json();
    write(CACHE + code, JSON.stringify({ etag: response.headers.get("ETag"), texts: fresh }));
    return fresh;
  }
  return known?.texts ?? {};
}

export function translatePage(root = document) {
  root.querySelectorAll("[data-i18n]").forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
  root.querySelectorAll("[data-i18n-label]").forEach((element) => {
    element.setAttribute("aria-label", t(element.dataset.i18nLabel));
  });
}

export async function loadLocale(code) {
  if (!available.length) {
    const info = await fetch("/api/v1/i18n", { cache: "no-cache" })
      .then((response) => (response.ok ? response.json() : null))
      .catch(() => null);
    available = info?.available ?? [{ code: FALLBACK, name: "Español", isDefault: true }];
    code = code ?? read(STORE) ?? info?.current ?? FALLBACK;
  }
  if (!available.some((option) => option.code === code)) code = FALLBACK;
  texts = await fetchTexts(code);
  locale = code;
  write(STORE, code);
  document.documentElement.lang = code;
  translatePage();
  return code;
}
