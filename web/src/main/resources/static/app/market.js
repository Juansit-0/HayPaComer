import { api } from "./api.js";
import { foodName, formatMoney, plural, t } from "./i18n.js";
import { esc, foodOptions, formData, grams, numberField, showError, toast, wireFoodSearch, wireSteppers } from "./ui.js";

const CATEGORY = (category) => t(`category.${category}`);

function row(item, bought) {
  return `<li class="${bought ? "bought" : ""}">
    <label class="check">
      <input type="checkbox" data-check="${esc(item.id)}" ${bought ? "checked" : ""}>
      <span class="item-name">${esc(foodName(item.food))}</span>
    </label>
    <span class="item-meta"><span class="data">${grams(item.grams)}</span>${
      item.source === "PLAN" || item.source === "RECIPE"
        ? `<span class="status quiet">${item.source === "PLAN" ? t("market.from-plan") : t("market.from-recipe")}</span>`
        : ""
    }<button class="ghost" type="button" data-remove="${esc(item.id)}" aria-label="${esc(t("market.remove-food", { food: foodName(item.food) }))}">${t("market.remove")}</button></span>
  </li>`;
}

function money(value, currency) {
  return formatMoney(value, currency);
}

function budgetPanel(budget, currency) {
  if (!budget) {
    return `<form class="row-form panel" data-budget>
      ${numberField({ name: "monthly", label: t("market.budget"), unit: currency, min: 1, bigStep: 10000, money: true })}
      <button type="submit">${t("market.set-budget")}</button>
    </form>`;
  }
  const over = budget.lines.filter((line) => line.estimatedCost !== null && !line.withinBudget);
  return `<div class="panel stack budget">
    <div class="totals">
      <div><span class="data">${money(budget.remaining, budget.currency)}</span><span>${t("market.left")}</span></div>
      <div><span class="data">${money(budget.plannedCost, budget.currency)}</span><span>${t("market.planned")}</span></div>
      <div><span class="data">${money(budget.monthly, budget.currency)}</span><span>${t("market.monthly")}</span></div>
    </div>
    ${
      over.length
        ? `<ul class="list">${over
            .map(
              (line) => `<li><span class="item-name">${esc(t("market.does-not-fit", { food: foodName(line.food), cost: money(line.estimatedCost, budget.currency) }))}</span>${
                line.cheaper
                  ? `<span class="item-meta">${esc(t("market.cheaper", { food: foodName(line.cheaper.food), grams: grams(line.cheaper.grams), cost: money(line.cheaper.estimatedCost, budget.currency) }))}</span>`
                  : ""
              }</li>`,
            )
            .join("")}</ul>`
        : `<p class="lead">${t("market.fits")}</p>`
    }
    <form class="row-form" data-budget>
      ${numberField({ name: "monthly", label: t("market.budget"), unit: budget.currency, value: Math.round(budget.monthly), min: 1, bigStep: 10000, money: true })}
      <button type="submit">${t("market.update-budget")}</button>
    </form>
  </div>`;
}

export async function renderMarket(main, household) {
  const base = `/households/${household.id}/market-list`;
  const [list, budget] = await Promise.all([
    api(base),
    api(`/households/${household.id}/market-budget`).catch(() => null),
  ]);
  const pending = list.pending.reduce((sum, group) => sum + group.items.length, 0);

  main.innerHTML = `
    <section class="stack" aria-labelledby="market-title">
      <div>
        <h1 id="market-title">${t("market.title")}</h1>
        <p class="lead">${
          pending
            ? plural("market.pending", pending)
            : t("market.empty")
        }</p>
      </div>
      ${budgetPanel(budget, household.currency ?? "")}
      <form class="row-form panel" data-add>
        <label>${t("field.food")}<input name="food" list="market-foods" required autocomplete="off" placeholder="${esc(t("market.food-example"))}"></label>
        <label>${t("field.grams")}<input class="grams" name="grams" type="number" min="1" step="1" required></label>
        ${foodOptions("market-foods")}
        <button class="primary" type="submit">${t("market.add")}</button>
        <button type="button" data-plan>${t("market.add-plan")}</button>
      </form>
      ${list.pending
        .map(
          (group) => `<div class="market-group">
            <h3>${CATEGORY(group.category)}</h3>
            <ul class="list">${group.items.map((item) => row(item, false)).join("")}</ul>
          </div>`,
        )
        .join("")}
    </section>
    ${
      list.checked.length
        ? `<section class="stack" aria-labelledby="bought-title">
            <div class="item-meta"><h2 id="bought-title">${t("market.cart")}</h2><button class="ghost" type="button" data-clear>${t("market.clear")}</button></div>
            <ul class="list">${list.checked.map((item) => row(item, true)).join("")}</ul>
          </section>`
        : ""
    }`;

  wireSteppers(main);
  const budgetForm = main.querySelector("[data-budget]");
  budgetForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api(`/households/${household.id}/market-budget`, {
        method: "PUT",
        body: { monthly: Number(formData(budgetForm).monthly) },
      });
      toast(t("market.toast-budget"));
      renderMarket(main, household);
    } catch (error) {
      showError(budgetForm, error);
    }
  });

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
