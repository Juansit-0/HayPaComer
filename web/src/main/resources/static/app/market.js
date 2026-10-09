import { api } from "./api.js";
import { foodName, formatMoney, plural, t } from "./i18n.js";
import {
  esc,
  foodOptions,
  formData,
  grams,
  kpi,
  moneyField,
  moneyValue,
  numberField,
  showError,
  toast,
  wireFoodSearch,
  wireSteppers,
} from "./ui.js";

function perKilo(line, currency) {
  if (line?.estimatedCost === null || line?.estimatedCost === undefined || !Number(line.grams)) return "";
  return t("market.per-kilo", {
    price: formatMoney((Number(line.estimatedCost) / Number(line.grams)) * 1000, currency),
  });
}

function row(item, bought, line, currency) {
  return `<li class="market-row ${bought ? "bought" : ""}">
    <label class="check">
      <input type="checkbox" data-check="${esc(item.id)}" ${bought ? "checked" : ""}>
      <span class="item-name">${esc(foodName(item.food))}</span>
    </label>
    <span class="market-amount"><span class="data">${grams(item.grams)}</span>${
      line?.estimatedCost !== null && line?.estimatedCost !== undefined
        ? `<span class="hint">${formatMoney(line.estimatedCost, currency)}</span><span class="hint">${perKilo(line, currency)}</span>`
        : ""
    }</span>
    <span class="item-actions">${
      item.source === "PLAN" || item.source === "RECIPE"
        ? `<span class="status quiet">${item.source === "PLAN" ? t("market.from-plan") : t("market.from-recipe")}</span>`
        : ""
    }<button class="ghost" type="button" data-remove="${esc(item.id)}" aria-label="${esc(t("market.remove-food", { food: foodName(item.food) }))}">${t("market.remove")}</button></span>
  </li>`;
}

function budgetTile(budget, currency) {
  if (!budget) {
    return `<section class="tile w-5" aria-labelledby="budget-title">
      <h2 id="budget-title">${t("market.budget")}</h2>
      <p class="hint">${t("market.budget-hint")}</p>
      <form class="stack" data-budget>
        ${moneyField({ name: "monthly", label: t("market.budget"), currency })}
        <button class="primary" type="submit">${t("market.set-budget")}</button>
      </form>
    </section>`;
  }
  const monthly = Number(budget.monthly);
  const spent = Number(budget.spent);
  const planned = Number(budget.plannedCost);
  const remaining = Number(budget.remaining);
  const share = (value) => (monthly > 0 ? Math.min(100, Math.max(0, (value / monthly) * 100)) : 0);
  const over = budget.lines.filter((line) => line.estimatedCost !== null && !line.withinBudget);
  return `<section class="tile w-5 budget-tile" aria-labelledby="budget-title">
    <h2 id="budget-title">${t("market.budget")}</h2>
    ${kpi(formatMoney(remaining, budget.currency), t("market.left"), remaining > 0 ? "positive" : "attention")}
    <div class="budget-bar" role="img" aria-label="${esc(
      t("market.bar-label", {
        spent: formatMoney(spent, budget.currency),
        planned: formatMoney(planned, budget.currency),
        monthly: formatMoney(monthly, budget.currency),
      }),
    )}">
      <span class="spent" style="width:${share(spent).toFixed(1)}%"></span>
      <span class="planned" style="width:${share(planned).toFixed(1)}%"></span>
    </div>
    <dl class="budget-figures">
      <div><dt><span class="key spent"></span>${t("market.spent")}</dt><dd class="data">${formatMoney(spent, budget.currency)}</dd></div>
      <div><dt><span class="key planned"></span>${t("market.planned")}</dt><dd class="data">${formatMoney(planned, budget.currency)}</dd></div>
      <div><dt>${t("market.monthly")}</dt><dd class="data">${formatMoney(monthly, budget.currency)}</dd></div>
    </dl>
    ${
      over.length
        ? `<ul class="list cheaper-list">${over
            .map(
              (line) => `<li>
                <span class="item-name">${esc(t("market.does-not-fit", { food: foodName(line.food), cost: formatMoney(line.estimatedCost, budget.currency) }))}</span>
                ${
                  line.cheaper
                    ? `<span class="status">${esc(t("market.cheaper", { food: foodName(line.cheaper.food), grams: grams(line.cheaper.grams), cost: formatMoney(line.cheaper.estimatedCost, budget.currency) }))}</span>`
                    : ""
                }
              </li>`,
            )
            .join("")}</ul>`
        : `<p class="hint">${t("market.fits")}</p>`
    }
    <details>
      <summary>${t("market.change-budget")}</summary>
      <form class="stack" data-budget>
        ${moneyField({ name: "monthly", label: t("market.budget"), currency: budget.currency, value: Math.round(monthly) })}
        <button type="submit">${t("market.update-budget")}</button>
      </form>
    </details>
  </section>`;
}

export async function renderMarket(main, household) {
  const base = `/households/${household.id}/market-list`;
  const [list, budget] = await Promise.all([
    api(base),
    api(`/households/${household.id}/market-budget`).catch(() => null),
  ]);
  const pending = list.pending.reduce((sum, group) => sum + group.items.length, 0);
  const currency = budget?.currency ?? household.currency ?? "COP";
  const lines = new Map((budget?.lines ?? []).map((line) => [line.itemId, line]));

  main.innerHTML = `
    <header>
      <h1 id="market-title">${t("market.title")}</h1>
      <p class="lead">${pending ? plural("market.pending", pending) : t("market.empty")}</p>
    </header>
    <div class="bento">
      ${budgetTile(budget, currency)}
      <section class="tile w-7" aria-labelledby="add-title">
        <h2 id="add-title">${t("market.add-title")}</h2>
        <form class="row-form" data-add>
          <label class="grow">${t("field.food")}<input name="food" list="market-foods" required autocomplete="off" placeholder="${esc(t("market.food-example"))}"></label>
          ${numberField({ name: "grams", label: t("field.grams"), unit: "g", min: 1, bigStep: 250, value: 500 })}
          ${foodOptions("market-foods")}
          <button class="primary" type="submit">${t("market.add")}</button>
        </form>
        <button type="button" data-plan>${t("market.add-plan")}</button>
      </section>
      <section class="tile ${list.checked.length ? "w-7" : "w-12"}" aria-labelledby="aisles-title">
        <h2 id="aisles-title">${t("market.aisles")}</h2>
        ${
          list.pending.length
            ? list.pending
                .map(
                  (group) => `<div class="market-group">
                    <h3>${t(`category.${group.category}`)}</h3>
                    <ul class="list">${group.items.map((item) => row(item, false, lines.get(item.id), currency)).join("")}</ul>
                  </div>`,
                )
                .join("")
            : `<p class="hint">${t("market.empty")}</p>`
        }
      </section>
      ${
        list.checked.length
          ? `<section class="tile w-5" aria-labelledby="bought-title">
              <header><h2 id="bought-title">${t("market.cart")}</h2><button class="ghost" type="button" data-clear>${t("market.clear")}</button></header>
              <ul class="list">${list.checked.map((item) => row(item, true, lines.get(item.id), currency)).join("")}</ul>
            </section>`
          : ""
      }
    </div>`;

  wireSteppers(main);
  main.querySelectorAll("[data-budget]").forEach((budgetForm) =>
    budgetForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const monthly = moneyValue(formData(budgetForm).monthly);
      if (!monthly) {
        showError(budgetForm, { message: t("market.budget-missing") });
        return;
      }
      try {
        await api(`/households/${household.id}/market-budget`, { method: "PUT", body: { monthly } });
        toast(t("market.toast-budget"));
        renderMarket(main, household);
      } catch (error) {
        showError(budgetForm, error);
      }
    }),
  );

  const add = main.querySelector("[data-add]");
  wireFoodSearch(add.querySelector("[name=food]"), api);
  add.addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formData(add);
    try {
      await api(`${base}/items`, { method: "POST", body: { food: values.food, grams: Number(values.grams) } });
      toast(t("market.toast-added", { food: foodName(values.food) }));
      renderMarket(main, household);
    } catch (error) {
      showError(add, error);
    }
  });

  main.querySelector("[data-plan]").addEventListener("click", async () => {
    try {
      const delta = await api(`${base}/from-plan`, { method: "POST" });
      const added = delta.filter((item) => Number(item.addedGrams) > 0).length;
      toast(added ? plural("market.toast-plan", added) : t("market.plan-covered"));
      renderMarket(main, household);
    } catch (error) {
      toast(error.status === 404 ? t("market.no-plan") : error.message);
    }
  });

  main.querySelectorAll("[data-check]").forEach((box) =>
    box.addEventListener("change", async () => {
      await api(`${base}/items/${box.dataset.check}/check`, { method: box.checked ? "POST" : "DELETE" });
      renderMarket(main, household);
    }),
  );

  main.querySelectorAll("[data-remove]").forEach((button) =>
    button.addEventListener("click", async () => {
      await api(`${base}/items/${button.dataset.remove}`, { method: "DELETE" });
      renderMarket(main, household);
    }),
  );

  main.querySelector("[data-clear]")?.addEventListener("click", async () => {
    await api(`${base}/checked`, { method: "DELETE" });
    toast(t("market.toast-cleared"));
    renderMarket(main, household);
  });
}
