import { api, session } from "./api.js";
import { chooseHousehold, householdLabel, renderSignIn } from "./auth.js";
import { renderFridge } from "./fridge.js";
import { renderAnalytics } from "./analytics.js";
import { renderChef } from "./chef.js";
import { renderMarket } from "./market.js";
import { connectLive, disconnectLive } from "./live.js";
import { renderNow } from "./now.js";
import { renderSettings } from "./settings.js";
import { currentLocale, loadLocale, locales, plural, t, translatePage } from "./i18n.js";
import { errorText, esc } from "./ui.js";

const main = document.getElementById("main");
const tabs = document.querySelector(".tabs");
const sessionBar = document.querySelector(".session");
const views = {
  now: renderNow,
  fridge: renderFridge,
  market: renderMarket,
  numbers: renderAnalytics,
  chef: renderChef,
  settings: (main, household) => renderSettings(main, household, () => {
    drawLanguages();
    translatePage();
    render();
  }),
};
let household = null;

function current() {
  const route = location.hash.replace("#/", "");
  return views[route] ? route : "now";
}

async function refreshInbox() {
  const notifications = await api("/notifications").catch(() => []);
  const unread = notifications.filter((notification) => !notification.readAt).length;
  const link = document.querySelector("[data-inbox]");
  link.textContent = unread ? plural("inbox.unread", unread) : "";
  link.title = notifications[0]?.title ?? "";
}

async function refreshStatus() {
  const status = await api("/status").catch(() => null);
  const notice = document.querySelector("[data-degraded]");
  if (!status || status.state === "OK") {
    notice.hidden = true;
    return;
  }
  const parts = status.degraded.map((item) => item.component).join(", ");
  notice.textContent = t("status.saved-mode", { parts });
  notice.hidden = false;
}

async function render() {
  if (!session.signedIn) {
    tabs.hidden = true;
    sessionBar.hidden = true;
    renderSignIn(main, render);
    return;
  }
  if (!household) {
    await chooseHousehold(main, (chosen) => {
      household = chosen;
      render();
    });
    return;
  }
  tabs.hidden = false;
  sessionBar.hidden = false;
  connectLive(household.id);
  document.querySelector("[data-household]").innerHTML = householdLabel(household);
  const route = current();
  document.querySelectorAll(".tabs a, [data-settings-link]").forEach((link) => {
    if (link.dataset.tab === route) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  try {
    await views[route](main, household);
    refreshInbox();
    refreshStatus();
  } catch (error) {
    main.innerHTML = `<section class="empty"><h1>${t("screen.failed")}</h1><p>${esc(errorText(error))}</p><button type="button" data-retry>${t("action.retry")}</button></section>`;
    main.querySelector("[data-retry]").addEventListener("click", render);
  }
}

window.addEventListener("hashchange", async () => {
  await render();
  window.scrollTo(0, 0);
  main.focus({ preventScroll: true });
});

window.addEventListener("hpc:live-state", (event) => {
  document.querySelector("[data-live]").dataset.state = event.detail;
  document.querySelector("[data-live]").textContent = event.detail === "on" ? t("live.on") : t("live.reconnecting");
});

window.addEventListener("hpc:live", (event) => {
  if (event.detail.kind === "alert") refreshInbox();
});

window.addEventListener("hpc:session-changed", (event) => {
  if (event.detail && household) return;
  disconnectLive();
  household = null;
  render();
});

window.addEventListener("hpc:signed-out", () => {
  disconnectLive();
  household = null;
  render();
});

document.querySelector("[data-signout]").addEventListener("click", async () => {
  const refreshToken = session.refresh;
  session.clear();
  disconnectLive();
  household = null;
  if (refreshToken) {
    await api("/auth/logout", { method: "POST", body: { refreshToken }, auth: false }).catch(() => null);
  }
  location.hash = "#/now";
  render();
});

const languageSelect = document.querySelector("[data-language]");

function drawLanguages() {
  languageSelect.innerHTML = locales()
    .map(
      (option) =>
        `<option value="${esc(option.code)}" ${option.code === currentLocale() ? "selected" : ""}>${esc(option.name)}</option>`,
    )
    .join("");
}

languageSelect.addEventListener("change", async () => {
  await loadLocale(languageSelect.value);
  drawLanguages();
  if (session.signedIn) {
    await api("/me/locale", { method: "PUT", body: { locale: currentLocale() } }).catch(() => null);
  }
  translatePage();
  await render();
});

await loadLocale();
drawLanguages();
render();
