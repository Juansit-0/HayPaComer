const ESCAPES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

export function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ESCAPES[char]);
}

export function grams(value) {
  const number = Number(value);
  if (number >= 1000) return `${(number / 1000).toFixed(number % 1000 === 0 ? 0 : 2)} kg`;
  return `${Math.round(number)} g`;
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
  if (days === null) return "No expiry date";
  if (days < 0) return days === -1 ? "Expired yesterday" : `Expired ${-days} days ago`;
  if (days === 0) return "Expires today";
  if (days === 1) return "Expires tomorrow";
  return `Expires in ${days} days`;
}

const STATUS = {
  EXPIRED: ["attention", "Expired"],
  UNDER_REVIEW: ["attention", "Under review"],
  AT_RISK: ["attention", "Expiring"],
  LEFTOVER: ["attention", "Leftover"],
  PRIVATE: ["quiet", "Private"],
  ASK_FIRST: ["quiet", "Ask first"],
};

const ORDER = ["EXPIRED", "UNDER_REVIEW", "AT_RISK", "LEFTOVER", "PRIVATE", "ASK_FIRST"];

export function statusPill(statuses = []) {
  const key = ORDER.find((status) => statuses.includes(status));
  if (!key) return `<span class="status">Fresh</span>`;
  const [tone, label] = STATUS[key];
  return `<span class="status ${tone}">${label}</span>`;
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
  return error?.message || "Something went wrong. Try again.";
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
      list.innerHTML = foods.map((food) => `<option value="${esc(food.name)}"></option>`).join("");
    }, 200);
  });
}
