import { api } from "./api.js";
import { liveFeed } from "./live.js";
import { foodName, formatTime, plural, t } from "./i18n.js";
import { esc, expiryText, grams, kpi, numberField, showError, statusPill, toast, wireSteppers } from "./ui.js";

const URGENT = ["EXPIRED", "UNDER_REVIEW", "AT_RISK", "LEFTOVER"];
const LABELS = { inventory: "live.kind.inventory", sensor: "live.kind.sensor", alert: "live.kind.alert", copilot: "live.kind.copilot" };
const ACTIONS = { STOCK_FOOD: "live.action.stock", CONSUME_FOOD: "live.action.consume", DISCARD_FOOD: "live.action.discard" };
const sentence = (text) => {
  const stock = text.match(/^([A-Z_]+) ([\d.]+) g left$/);
  if (stock) return t("live.stock-left", { action: t(ACTIONS[stock[1]] ?? "live.action.other"), grams: grams(stock[2]) });
  return text.charAt(0).toUpperCase() + text.slice(1).replace(/ C$/, "\u2009°C");
};

function candidate(recipe) {
  return {
    name: recipe.name,
    servings: recipe.servings,
    minutes: recipe.minutes,
    requirements: recipe.requirements.map((requirement) => ({
      food: requirement.food,
      grams: requirement.grams,
      optional: requirement.optional,
    })),
    steps: recipe.steps.map((step) => ({
      instruction: step.instruction,
      timerSeconds: step.timerSeconds,
      weigh: step.weighFood ? { food: step.weighFood, grams: step.weighGrams } : null,
    })),
  };
}

export async function renderNow(main, household) {
  const base = `/households/${household.id}`;
  const [snapshot, inventory, recipes] = await Promise.all([
    api(`${base}/kitchen`),
    api(`${base}/inventory?rescueFirst=true`),
    api(`${base}/recipes`),
  ]);
  const urgent = inventory.filter(
    (item) => item.usable && item.statuses.some((status) => URGENT.includes(status)),
  );
  const [first, ...rest] = urgent;

  main.innerHTML = `
    <header>
      <h1 id="now-title">${first ? t("now.title") : t("now.title-fresh")}</h1>
      <p class="lead">${
        first
          ? t("now.lead")
          : t("now.lead-fresh")
      }</p>
    </header>

    <div class="bento">
      ${
        first
          ? `<article class="tile alert w-8" aria-label="${esc(foodName(first.name))}">
              <div class="item-meta"><h2>${esc(foodName(first.name))}</h2>${statusPill(first.statuses)}</div>
              ${kpi(grams(first.grams), expiryText(first), "attention")}
              <form class="row-form" data-consume="${esc(first.id)}">
                ${numberField({ name: "grams", label: t("now.grams-used"), unit: "g", value: Math.round(first.grams), min: 1, bigStep: 50 })}
                <button class="primary" type="submit">${t("now.mark-used")}</button>
              </form>
            </article>`
          : ""
      }

      <section class="tile ${first ? "w-4" : "w-12"}" aria-labelledby="totals-title">
        <h2 id="totals-title">${t("now.in-fridge")}</h2>
        <div class="bento">
          <div class="w-6">${kpi(grams(snapshot.totalGrams), t("now.kpi.measured"))}</div>
          <div class="w-6">${kpi(String(snapshot.items), t("now.kpi.items"))}</div>
          <div class="w-6">${kpi(String(snapshot.atRisk), t("now.kpi.at-risk"), snapshot.atRisk ? "attention" : "")}</div>
          <div class="w-6">${kpi(String(snapshot.expired), t("now.kpi.expired"), snapshot.expired ? "attention" : "")}</div>
        </div>
      </section>

      ${
        rest.length
          ? `<section class="tile w-6" aria-labelledby="also-title">
              <h2 id="also-title">${t("now.also")}</h2>
              <ul class="list">${rest
                .map(
                  (item) => `<li>
                    <div class="item-meta"><span class="item-name">${esc(foodName(item.name))}</span>${statusPill(item.statuses)}<span>${expiryText(item)}</span></div>
                    <span class="data">${grams(item.grams)}</span>
                  </li>`,
                )
                .join("")}</ul>
            </section>`
          : ""
      }

      <section class="tile ${rest.length ? "w-6" : "w-12"}" aria-labelledby="live-title">
        <header><h2 id="live-title">${t("now.live")}</h2></header>
        <p class="hint">${t("now.live-hint")}</p>
        <ul class="list feed" data-feed aria-live="polite"></ul>
      </section>

      <section class="tile w-12" aria-labelledby="cook-title">
        <h2 id="cook-title">${t("now.cook")}</h2>
        <p class="lead">${
          recipes.length
            ? plural("now.cook-lead", recipes.length)
            : t("now.cook-empty")
        }</p>
        ${
          recipes.length
            ? `<form class="row-form" data-suggest>
                ${numberField({ name: "servings", label: t("now.people"), value: 2, min: 1, max: 20 })}
                ${numberField({ name: "maxMinutes", label: t("now.max-minutes"), unit: "min", min: 1, bigStep: 10, required: false })}
                <button type="submit">${t("now.find")}</button>
              </form>
              <div class="dishes" data-dishes></div>`
            : ""
        }
      </section>
    </div>`;
  wireSteppers(main);

  const feed = main.querySelector("[data-feed]");
  const drawFeed = () => {
    const events = liveFeed();
    feed.innerHTML = events.length
      ? events
          .map(
            (event) => `<li>
              <div class="item-meta"><span class="status ${event.kind === "alert" ? "attention" : "quiet"}">${esc(LABELS[event.kind] ? t(LABELS[event.kind]) : event.kind)}</span><span>${esc(sentence(event.detail))}</span></div>
              <time class="data" datetime="${esc(event.at)}">${formatTime(event.at)}</time>
            </li>`,
          )
          .join("")
      : `<li><span class="lead">${t("now.live-empty")}</span></li>`;
  };
  drawFeed();
  const onLive = () => {
    if (!document.body.contains(feed)) {
      window.removeEventListener("hpc:live", onLive);
      return;
    }
    drawFeed();
  };
  window.addEventListener("hpc:live", onLive);

  main.querySelector("[data-consume]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const amount = Number(new FormData(event.target).get("grams"));
    try {
      await api(`${base}/items/${event.target.dataset.consume}/consume`, {
        method: "POST",
        body: { grams: amount },
      });
      toast(t("now.toast-used", { grams: grams(amount), food: foodName(first.name) }));
      renderNow(main, household);
    } catch (error) {
      showError(event.target, error);
    }
  });

  main.querySelector("[data-suggest]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = new FormData(event.target);
    const box = main.querySelector("[data-dishes]");
    try {
      const result = await api(`${base}/suggestions`, {
        method: "POST",
        body: {
          servings: Number(values.get("servings")),
          maxMinutes: values.get("maxMinutes") ? Number(values.get("maxMinutes")) : null,
          candidates: recipes.slice(0, 50).map(candidate),
        },
      });
      box.innerHTML = result.suggestions.length
        ? result.suggestions
            .map(
              (dish) => `<article class="dish">
                <div class="item-meta"><h3>${esc(dish.recipe)}</h3><span class="data">${dish.minutes} min</span>${
                  dish.evaluation.verdict === "ENOUGH"
                    ? `<span class="status">${t("verdict.ENOUGH")}</span>`
                    : `<span class="status attention">${esc(t(`verdict.${dish.evaluation.verdict}`))}</span>`
                }</div>
                <p>${esc(dish.reason)}</p>
              </article>`,
            )
            .join("")
        : `<p class="lead">${t("now.no-dish")}</p>`;
    } catch (error) {
      showError(event.target, error);
    }
  });
}
