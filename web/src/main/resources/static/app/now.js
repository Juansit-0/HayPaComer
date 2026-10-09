import { api } from "./api.js";
import { liveFeed } from "./live.js";
import { esc, grams, kpi, numberField, showError, statusPill, toast, whenText, wireSteppers } from "./ui.js";

const URGENT = ["EXPIRED", "UNDER_REVIEW", "AT_RISK", "LEFTOVER"];
const LABELS = { inventory: "Stock", sensor: "Sensor", alert: "Alert", copilot: "Scale" };
const ACTIONS = { STOCK_FOOD: "Food added", CONSUME_FOOD: "Food used", DISCARD_FOOD: "Food thrown away" };
const sentence = (text) => {
  const stock = text.match(/^([A-Z_]+) ([\d.]+) g left$/);
  if (stock) return `${ACTIONS[stock[1]] ?? "Stock changed"}, ${grams(stock[2])} left`;
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
      <h1 id="now-title">${first ? "Use this first" : "Nothing is about to expire"}</h1>
      <p class="lead">${
        first
          ? "Measured stock in your fridge, ordered by what will go bad soonest."
          : "Everything you can use is fresh. Plan a dish or restock the market list."
      }</p>
    </header>

    <div class="bento">
      ${
        first
          ? `<article class="tile alert w-8" aria-label="${esc(first.name)}">
              <div class="item-meta"><h2>${esc(first.name)}</h2>${statusPill(first.statuses)}</div>
              ${kpi(grams(first.grams), whenText(first.expiresOn), "attention")}
              <form class="row-form" data-consume="${esc(first.id)}">
                ${numberField({ name: "grams", label: "Grams used", unit: "g", value: Math.round(first.grams), min: 1, bigStep: 50 })}
                <button class="primary" type="submit">Mark as used</button>
              </form>
            </article>`
          : ""
      }

      <section class="tile ${first ? "w-4" : "w-12"}" aria-labelledby="totals-title">
        <h2 id="totals-title">In the fridge</h2>
        <div class="bento">
          <div class="w-6">${kpi(grams(snapshot.totalGrams), "measured food")}</div>
          <div class="w-6">${kpi(String(snapshot.items), "items")}</div>
          <div class="w-6">${kpi(String(snapshot.atRisk), "expiring soon", snapshot.atRisk ? "attention" : "")}</div>
          <div class="w-6">${kpi(String(snapshot.expired), "expired", snapshot.expired ? "attention" : "")}</div>
        </div>
      </section>

      ${
        rest.length
          ? `<section class="tile w-6" aria-labelledby="also-title">
              <h2 id="also-title">Also expiring</h2>
              <ul class="list">${rest
                .map(
                  (item) => `<li>
                    <div class="item-meta"><span class="item-name">${esc(item.name)}</span>${statusPill(item.statuses)}<span>${whenText(item.expiresOn)}</span></div>
                    <span class="data">${grams(item.grams)}</span>
                  </li>`,
                )
                .join("")}</ul>
            </section>`
          : ""
      }

      <section class="tile ${rest.length ? "w-6" : "w-12"}" aria-labelledby="live-title">
        <header><h2 id="live-title">Happening now</h2></header>
        <p class="hint">Door, temperature, and stock changes appear here as they happen.</p>
        <ul class="list feed" data-feed aria-live="polite"></ul>
      </section>

      <section class="tile w-12" aria-labelledby="cook-title">
        <h2 id="cook-title">Cook now</h2>
        <p class="lead">${
          recipes.length
            ? `Checks your ${recipes.length} saved ${recipes.length === 1 ? "recipe" : "recipes"} against the grams in the fridge and the allergies at home.`
            : "Save a recipe first, then this shows what you can cook with what is really here."
        }</p>
        ${
          recipes.length
            ? `<form class="row-form" data-suggest>
                ${numberField({ name: "servings", label: "People", value: 2, min: 1, max: 20 })}
                ${numberField({ name: "maxMinutes", label: "Minutes, at most", unit: "min", min: 1, bigStep: 10, required: false })}
                <button type="submit">Find dishes</button>
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
              <div class="item-meta"><span class="status ${event.kind === "alert" ? "attention" : "quiet"}">${esc(LABELS[event.kind] ?? event.kind)}</span><span>${esc(sentence(event.detail))}</span></div>
              <time class="data" datetime="${esc(event.at)}">${new Date(event.at).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</time>
            </li>`,
          )
          .join("")
      : `<li><span class="lead">Quiet for now. Open the fridge door or use some food to see it here.</span></li>`;
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
      toast(`Marked ${grams(amount)} of ${first.name} as used`);
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
                    ? `<span class="status">Enough at home</span>`
                    : `<span class="status attention">${esc(dish.evaluation.verdict.toLowerCase())}</span>`
                }</div>
                <p>${esc(dish.reason)}</p>
              </article>`,
            )
            .join("")
        : `<p class="lead">No saved recipe fits right now. Try more minutes, fewer people, or check the market list.</p>`;
    } catch (error) {
      showError(event.target, error);
    }
  });
}
