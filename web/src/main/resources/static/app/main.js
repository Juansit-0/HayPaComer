import { api, session } from "./api.js";
import { chooseHousehold, householdLabel, renderSignIn } from "./auth.js";
import { renderFridge } from "./fridge.js";
import { renderMarket } from "./market.js";
import { renderNow } from "./now.js";
import { errorText } from "./ui.js";

const main = document.getElementById("main");
const tabs = document.querySelector(".tabs");
const sessionBar = document.querySelector(".session");
const views = { now: renderNow, fridge: renderFridge, market: renderMarket };
let household = null;

function current() {
  const route = location.hash.replace("#/", "");
  return views[route] ? route : "now";
}

async function refreshInbox() {
  const notifications = await api("/notifications").catch(() => []);
  const unread = notifications.filter((notification) => !notification.readAt).length;
  const link = document.querySelector("[data-inbox]");
  link.textContent = unread ? `${unread} unread ${unread === 1 ? "alert" : "alerts"}` : "";
  link.title = notifications[0]?.title ?? "";
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
  document.querySelector("[data-household]").innerHTML = householdLabel(household);
  const route = current();
  tabs.querySelectorAll("a").forEach((link) => {
    if (link.dataset.tab === route) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  try {
    await views[route](main, household);
    refreshInbox();
  } catch (error) {
    main.innerHTML = `<section class="empty"><h1>This screen did not load</h1><p>${errorText(error)}</p><button type="button" data-retry>Try again</button></section>`;
    main.querySelector("[data-retry]").addEventListener("click", render);
  }
}

window.addEventListener("hashchange", async () => {
  await render();
  window.scrollTo(0, 0);
  main.focus({ preventScroll: true });
});

window.addEventListener("hpc:signed-out", () => {
  household = null;
  render();
});

document.querySelector("[data-signout]").addEventListener("click", async () => {
  const refreshToken = session.refresh;
  session.clear();
  household = null;
  if (refreshToken) {
    await api("/auth/logout", { method: "POST", body: { refreshToken }, auth: false }).catch(() => null);
  }
  location.hash = "#/now";
  render();
});

render();
