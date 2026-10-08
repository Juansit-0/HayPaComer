import { api } from "./api.js";
import { esc, grams, showError, statusPill, toast, whenText } from "./ui.js";

const URGENT = ["EXPIRED", "UNDER_REVIEW", "AT_RISK", "LEFTOVER"];

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
    <section class="stack" aria-labelledby="now-title">
      <div>
        <h1 id="now-title">${first ? "Use this first" : "Nothing is about to expire"}</h1>
        <p class="lead">${
          first
            ? "Measured stock in your fridge, ordered by what will go bad soonest."
            : "Everything you can use is fresh. Plan a dish or restock the market list."
        }</p>
      </div>
      ${
        first
          ? `<article class="use-first" aria-label="${esc(first.name)}">
              <div class="stack">
                <div class="item-meta"><span class="item-name">${esc(first.name)}</span>${statusPill(first.statuses)}</div>
                <span class="hero-number">${grams(first.grams)}</span>
                <span class="when">${whenText(first.expiresOn)}</span>
              </div>
              <form class="row-form" data-consume="${esc(first.id)}">
                <label>Grams used<input class="grams" name="grams" type="number" min="1" step="1" required value="${Math.round(first.grams)}"></label>
                <button class="primary" type="submit">Mark as used</button>
              </form>
            </article>`
          : ""
      }
      ${
        rest.length
          ? `<ul class="list" aria-label="Also expiring">${rest
              .map(
                (item) => `<li>
                  <div class="item-meta"><span class="item-name">${esc(item.name)}</span>${statusPill(item.statuses)}<span>${whenText(item.expiresOn)}</span></div>
                  <span class="data">${grams(item.grams)}</span>
                </li>`,
              )
              .join("")}</ul>`
          : ""
      }
    </section>

    <section class="stack" aria-labelledby="totals-title">
      <h2 id="totals-title">In the fridge</h2>
      <div class="totals">
        <div><span class="data">${grams(snapshot.totalGrams)}</span><span>measured food</span></div>
        <div><span class="data">${snapshot.items}</span><span>items</span></div>
        <div><span class="data">${snapshot.atRisk}</span><span>expiring soon</span></div>
        <div><span class="data">${snapshot.expired}</span><span>expired</span></div>
      </div>
    </section>

    <section class="stack" aria-labelledby="cook-title">
      <div>
        <h2 id="cook-title">Cook now</h2>
        <p class="lead">${
          recipes.length
            ? `Checks your ${recipes.length} saved ${recipes.length === 1 ? "recipe" : "recipes"} against the grams in the fridge and the allergies at home.`
            : "Save a recipe first, then this shows what you can cook with what is really here."
        }</p>
      </div>
      ${
        recipes.length
          ? `<form class="row-form" data-suggest>
              <label>People<input class="grams" name="servings" type="number" min="1" max="20" value="2" required></label>
              <label>Minutes, at most<input class="grams" name="maxMinutes" type="number" min="1" placeholder="any"></label>
              <button type="submit">Find dishes</button>
            </form>
            <div class="dishes" data-dishes></div>`
          : ""
      }
    </section>`;

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
