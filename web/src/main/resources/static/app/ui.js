import { foodName, formatNumber, plural, t } from "./i18n.js";

const ESCAPES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

export function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ESCAPES[char]);
}

export function grams(value) {
  const number = Number(value);
  if (number >= 1000) return `${formatNumber(number / 1000, number % 1000 === 0 ? 0 : 2)} kg`;
  return `${formatNumber(Math.round(number))} g`;
}

export function daysLeft(isoDate) {
  if (!isoDate) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const day = new Date(`${isoDate}T00:00:00`);
  return Math.round((day - today) / 86400000);
}

export function whenText(isoDate) {
  const days = daysLeft(isoDate);
  if (days === null) return t("expiry.none");
  if (days < 0) return days === -1 ? t("expiry.yesterday") : plural("expiry.ago", -days);
  if (days === 0) return t("expiry.today");
  if (days === 1) return t("expiry.tomorrow");
  return plural("expiry.in", days);
}

export function expiryText(item) {
  const text = whenText(item.expiresOn);
  if (!item.expiresOn) return text;
  if (item.expirySource === "ESTIMATED" || item.expirySource === "AI_SUGGESTED") {
    return `\u2248\u2009${text} (${t("expiry.estimated")})`;
  }
  if (item.expirySource === "LABEL") return `${text} (${t("expiry.label")})`;
  return text;
}

const STATUS = {
  EXPIRED: ["attention", "status.expired"],
  UNDER_REVIEW: ["attention", "status.under-review"],
  AT_RISK: ["attention", "status.at-risk"],
  LEFTOVER: ["attention", "status.leftover"],
  PRIVATE: ["quiet", "status.private"],
  ASK_FIRST: ["quiet", "status.ask-first"],
};

const ORDER = ["EXPIRED", "UNDER_REVIEW", "AT_RISK", "LEFTOVER", "PRIVATE", "ASK_FIRST"];

export function statusPill(statuses = []) {
  const key = ORDER.find((status) => statuses.includes(status));
  if (!key) return `<span class="status">${t("status.fresh")}</span>`;
  const [tone, label] = STATUS[key];
  return `<span class="status ${tone}">${t(label)}</span>`;
}

export function toast(message) {
  const element = document.querySelector("[data-toast]");
  element.textContent = message;
  element.classList.add("visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => element.classList.remove("visible"), 3200);
}

export function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

export function errorText(error) {
  if (error?.problem?.errors) {
    return Object.entries(error.problem.errors)
      .map(([field, message]) => `${field} ${message}`)
      .join(". ");
  }
  return error?.message || t("error.generic");
}

export function showError(form, error) {
  let box = form.querySelector(".error");
  if (!box) {
    box = document.createElement("p");
    box.className = "error";
    box.setAttribute("role", "alert");
    form.append(box);
  }
  box.textContent = errorText(error);
}

export function foodOptions(id) {
  return `<datalist id="${id}"></datalist>`;
}

export function wireFoodSearch(input, api) {
  const list = document.getElementById(input.getAttribute("list"));
  let timer;
  input.addEventListener("input", () => {
    clearTimeout(timer);
    const query = input.value.trim();
    if (query.length < 2) return;
    timer = setTimeout(async () => {
      const foods = await api(`/foods?q=${encodeURIComponent(query)}`);
      list.innerHTML = foods
        .map((food) => `<option value="${esc(food.name)}" label="${esc(foodName(food.name))}"></option>`)
        .join("");
    }, 200);
  });
}

export function kpi(value, label, tone = "") {
  return `<div class="kpi ${tone}"><span class="value">${value}</span><span class="label">${esc(label)}</span></div>`;
}

export function numberField({ name, label, unit, value = "", min = 0, max, step = 1, bigStep, money = false, hint, required = true }) {
  const id = `field-${name}-${Math.random().toString(36).slice(2, 8)}`;
  return `<div class="number-field">
    <label for="${id}">${esc(label)}</label>
    ${hint ? `<p class="hint">${esc(hint)}</p>` : ""}
    <div class="number-input" data-stepper>
      <button type="button" data-step="-1" aria-label="${esc(t("stepper.less", { label }))}">−</button>
      <input id="${id}" class="${money ? "money" : "grams"}" name="${esc(name)}" type="number" inputmode="${step < 1 ? "decimal" : "numeric"}" min="${min}" ${max === undefined ? "" : `max="${max}"`} step="${step}" ${bigStep ? `data-big-step="${bigStep}"` : ""} value="${esc(value)}" ${required ? "required" : ""}>
      ${unit ? `<span class="unit" aria-hidden="true">${esc(unit)}</span>` : ""}
      <button type="button" data-step="1" aria-label="${esc(t("stepper.more", { label }))}">+</button>
    </div>
  </div>`;
}

export function moneyField({ name, label, currency, value = "", step = 10000, hint }) {
  const id = `field-${name}-${Math.random().toString(36).slice(2, 8)}`;
  const shown = value === "" ? "" : formatNumber(value);
  return `<div class="number-field">
    <label for="${id}">${esc(label)}</label>
    ${hint ? `<p class="hint">${esc(hint)}</p>` : ""}
    <div class="number-input" data-stepper>
      <button type="button" data-step="-1" aria-label="${esc(t("stepper.less", { label }))}">\u2212</button>
      <input id="${id}" class="money" name="${esc(name)}" type="text" inputmode="numeric" autocomplete="off" data-money data-big-step="${step}" value="${esc(shown)}" required>
      <span class="unit" aria-hidden="true">${esc(currency)}</span>
      <button type="button" data-step="1" aria-label="${esc(t("stepper.more", { label }))}">+</button>
    </div>
  </div>`;
}

export function moneyValue(text) {
  const digits = String(text ?? "").replace(/\D/g, "");
  return digits ? Number(digits) : 0;
}

export function wireSteppers(root) {
  root.querySelectorAll("[data-stepper]").forEach((box) => {
    const input = box.querySelector("input");
    const money = input.hasAttribute("data-money");
    if (money) {
      input.addEventListener("input", () => {
        const amount = moneyValue(input.value);
        input.value = amount ? formatNumber(amount) : "";
      });
    }
    box.querySelectorAll("[data-step]").forEach((button) => {
      button.addEventListener("click", () => {
        const step = Number(input.step) || 1;
        const big = Number(input.dataset.bigStep) || step;
        const current = money ? moneyValue(input.value) : Number(input.value) || 0;
        const next = current + Number(button.dataset.step) * big;
        const min = input.min === "" ? -Infinity : Number(input.min);
        const max = input.max === "" ? Infinity : Number(input.max);
        const bounded = Math.min(max, Math.max(min, next));
        input.value = money ? formatNumber(Math.max(0, bounded)) : String(bounded);
        input.dispatchEvent(new Event("input", { bubbles: true }));
      });
    });
  });
}

export function chips(name, legend, options, selected) {
  return `<fieldset class="chips"><legend>${esc(legend)}</legend>${options
    .map(
      ([value, text]) => `<label class="chip"><input type="radio" name="${esc(name)}" value="${esc(value)}" ${
        value === selected ? "checked" : ""
      }><span>${esc(text)}</span></label>`,
    )
    .join("")}</fieldset>`;
}

export function dateField({ name, label, value = "", unknownLabel = t("field.unknown-date") }) {
  const id = `field-${name}-${Math.random().toString(36).slice(2, 8)}`;
  return `<div class="date-field" data-date-field>
    <label for="${id}">${esc(label)}</label>
    <div class="row-form">
      <input id="${id}" name="${esc(name)}" type="date" value="${esc(value)}">
      <label class="check"><input type="checkbox" data-unknown>${esc(unknownLabel)}</label>
    </div>
  </div>`;
}

export function wireDateFields(root) {
  root.querySelectorAll("[data-date-field]").forEach((field) => {
    const date = field.querySelector('input[type="date"]');
    field.querySelector("[data-unknown]").addEventListener("change", (event) => {
      date.disabled = event.target.checked;
      if (event.target.checked) date.value = "";
    });
  });
}

export function openSheet(title, body, onReady) {
  const dialog = document.createElement("dialog");
  dialog.className = "sheet";
  dialog.setAttribute("aria-label", title);
  dialog.innerHTML = `<div><div class="sheet-head"><h2>${esc(title)}</h2><button type="button" class="ghost" data-close>${t("action.close")}</button></div>${body}</div>`;
  document.body.append(dialog);
  dialog.querySelector("[data-close]").addEventListener("click", () => dialog.close());
  dialog.addEventListener("close", () => dialog.remove());
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });
  wireSteppers(dialog);
  wireDateFields(dialog);
  dialog.showModal();
  onReady?.(dialog);
  return dialog;
}
