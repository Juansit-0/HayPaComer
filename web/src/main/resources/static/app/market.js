import { api } from "./api.js";
import { esc, foodOptions, formData, grams, showError, toast, wireFoodSearch } from "./ui.js";

const CATEGORY = (category) => category.charAt(0) + category.slice(1).toLowerCase();

function row(item, bought) {
  return `<li class="${bought ? "bought" : ""}">
    <label class="check">
      <input type="checkbox" data-check="${esc(item.id)}" ${bought ? "checked" : ""}>
      <span class="item-name">${esc(item.food)}</span>
    </label>
    <span class="item-meta"><span class="data">${grams(item.grams)}</span>${
      item.source === "PLAN" || item.source === "RECIPE"
        ? `<span class="status quiet">${item.source === "PLAN" ? "From the plan" : "From a recipe"}</span>`
        : ""
    }<button class="ghost" type="button" data-remove="${esc(item.id)}" aria-label="Remove ${esc(item.food)}">Remove</button></span>
  </li>`;
}

export async function renderMarket(main, household) {
  const base = `/households/${household.id}/market-list`;
  const list = await api(base);
  const pending = list.pending.reduce((sum, group) => sum + group.items.length, 0);

  main.innerHTML = `
    <section class="stack" aria-labelledby="market-title">
      <div>
        <h1 id="market-title">Market list</h1>
        <p class="lead">${
          pending
            ? `<span class="data">${pending}</span> ${pending === 1 ? "thing" : "things"} to buy, grouped by aisle.`
            : "Nothing to buy yet. Add food, or let the weekly plan fill the gaps."
        }</p>
      </div>
      <form class="row-form panel" data-add>
        <label>Food<input name="food" list="market-foods" required autocomplete="off" placeholder="Rice"></label>
        <label>Grams<input class="grams" name="grams" type="number" min="1" step="1" required></label>
        ${foodOptions("market-foods")}
        <button class="primary" type="submit">Add to list</button>
        <button type="button" data-plan>Add what the plan needs</button>
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
            <div class="item-meta"><h2 id="bought-title">In the cart</h2><button class="ghost" type="button" data-clear>Clear the cart</button></div>
            <ul class="list">${list.checked.map((item) => row(item, true)).join("")}</ul>
          </section>`
        : ""
    }`;

  const add = main.querySelector("[data-add]");
  wireFoodSearch(add.querySelector("[name=food]"), api);
  add.addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formData(add);
    try {
      await api(`${base}/items`, { method: "POST", body: { food: values.food, grams: Number(values.grams) } });
      toast(`Added ${values.food} to the list`);
      renderMarket(main, household);
    } catch (error) {
      showError(add, error);
    }
  });

  main.querySelector("[data-plan]").addEventListener("click", async () => {
    try {
      const delta = await api(`${base}/from-plan`, { method: "POST" });
      const added = delta.filter((item) => Number(item.addedGrams) > 0).length;
      toast(added ? `Added ${added} ${added === 1 ? "food" : "foods"} from this week's plan` : "The list already covers this week's plan");
      renderMarket(main, household);
    } catch (error) {
      toast(error.status === 404 ? "Generate a weekly plan first" : error.message);
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
    toast("Cleared the cart");
    renderMarket(main, household);
  });
}
