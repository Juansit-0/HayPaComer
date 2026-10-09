import { api, session } from "./api.js";
import { foodName, formatDay, formatMoney, formatNumber, t } from "./i18n.js";
import { esc, formData, grams, showError, toast, wireFoodSearch, foodOptions } from "./ui.js";

const PERIODS = [7, 30, 90];
let chosenDays = 30;
let me = null;

function money(value, currency) {
  return formatMoney(value, currency);
}

function percent(rate) {
  return `${formatNumber(rate * 100, 0 < rate && rate < 0.1 ? 1 : 0)}\u2009%`;
}

function isoDay(date) {
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function shortDay(iso) {
  return formatDay(iso);
}

function chart(days) {
  const width = 640;
  const height = 220;
  const middle = 140;
  const gap = days.length > 40 ? 1 : 3;
  const step = width / days.length;
  const bar = Math.max(step - gap, 1);
  const top = Math.max(1, ...days.map((day) => Number(day.tally.consumedGrams)));
  const bottom = Math.max(1, ...days.map((day) => Number(day.tally.discardedGrams)));
  const scale = Math.max(top / (middle - 12), bottom / (height - middle - 8));
  const bars = days
    .map((day, index) => {
      const x = (index * step).toFixed(1);
      const eaten = Number(day.tally.consumedGrams) / scale;
      const rescued = Number(day.tally.rescuedGrams) / scale;
      const wasted = Number(day.tally.discardedGrams) / scale;
      return `<g>
        ${eaten ? `<rect class="eaten" x="${x}" y="${(middle - eaten).toFixed(1)}" width="${bar.toFixed(1)}" height="${eaten.toFixed(1)}"/>` : ""}
        ${rescued ? `<rect class="rescued" x="${x}" y="${(middle - rescued).toFixed(1)}" width="${bar.toFixed(1)}" height="${rescued.toFixed(1)}"/>` : ""}
        ${wasted ? `<rect class="wasted" x="${x}" y="${middle}" width="${bar.toFixed(1)}" height="${wasted.toFixed(1)}"/>` : ""}
      </g>`;
    })
    .join("");
  const active = days.filter((day) => Number(day.tally.consumedGrams) || Number(day.tally.discardedGrams));
  return `<figure class="chart">
    <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" role="img" aria-labelledby="chart-caption">
      ${bars}
      <line class="axis" x1="0" x2="${width}" y1="${middle}" y2="${middle}"/>
    </svg>
    <div class="chart-scale"><span>${shortDay(days[0].day)}</span><span>${shortDay(days[days.length - 1].day)}</span></div>
    <figcaption id="chart-caption">
      <span class="key rescued">${t("numbers.key.rescued")}</span>
      <span class="key eaten">${t("numbers.key.eaten")}</span>
      <span class="key wasted">${t("numbers.key.wasted")}</span>
    </figcaption>
    <table class="visually-hidden">
      <caption>${t("numbers.table.caption")}</caption>
      <thead><tr><th>${t("numbers.table.day")}</th><th>${t("numbers.table.eaten")}</th><th>${t("numbers.table.rescued")}</th><th>${t("numbers.table.wasted")}</th></tr></thead>
      <tbody>${active
        .map(
          (day) => `<tr><td>${day.day}</td><td>${grams(day.tally.consumedGrams)}</td><td>${grams(day.tally.rescuedGrams)}</td><td>${grams(day.tally.discardedGrams)}</td></tr>`,
        )
        .join("")}</tbody>
    </table>
  </figure>`;
}

function ranking(members) {
  const best = Math.max(1, ...members.map((member) => Number(member.tally.rescuedGrams)));
  return `<ol class="ranking">${members
    .map(
      (member) => `<li>
        <span class="item-name">${esc(member.name)}${member.userId === me ? ` <span class="status quiet">${t("numbers.you")}</span>` : ""}</span>
        <span class="meter" aria-hidden="true"><span style="width:${((Number(member.tally.rescuedGrams) / best) * 100).toFixed(0)}%"></span></span>
        <span class="data">${t("numbers.rescued-grams", { grams: grams(member.tally.rescuedGrams) })}</span>
        <span class="item-meta">${t("numbers.member-detail", { eaten: grams(member.tally.consumedGrams), wasted: grams(member.tally.discardedGrams) })}</span>
      </li>`,
    )
    .join("")}</ol>`;
}

function foods(list) {
  return `<ul class="list">${list
    .slice(0, 8)
    .map(
      (food) => `<li>
        <span class="item-name">${food.foodKey === "unknown" ? t("numbers.unknown-food") : esc(foodName(food.foodKey) === food.foodKey ? food.foodKey.charAt(0).toUpperCase() + food.foodKey.slice(1) : foodName(food.foodKey))}</span>
        <span class="item-meta">${
          Number(food.tally.consumedGrams) ? t("numbers.eaten-grams", { grams: `<span class="data">${grams(food.tally.consumedGrams)}</span>` }) : ""
        }${
          Number(food.tally.discardedGrams) ? `<span class="status attention">${t("numbers.wasted-grams", { grams: grams(food.tally.discardedGrams) })}</span>` : ""
        }</span>
      </li>`,
    )
    .join("")}</ul>`;
}

async function download(base, format, from, to) {
  const response = await fetch(`/api/v1${base}/analytics/report?format=${format}&from=${from}&to=${to}`, {
    headers: { Authorization: `Bearer ${session.access}` },
  });
  if (!response.ok) {
    toast(t("numbers.report-failed"));
    return;
  }
  const name = /filename="([^"]+)"/.exec(response.headers.get("Content-Disposition") ?? "")?.[1] ?? `report.${format}`;
  const link = document.createElement("a");
  link.href = URL.createObjectURL(await response.blob());
  link.download = name;
  link.click();
  URL.revokeObjectURL(link.href);
}

export async function renderAnalytics(main, household) {
  const base = `/households/${household.id}`;
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - (chosenDays - 1));
  const [metrics, profile] = await Promise.all([
    api(`${base}/analytics?from=${isoDay(from)}&to=${isoDay(to)}`),
    me ? Promise.resolve(null) : api("/me").catch(() => null),
  ]);
  if (profile) me = profile.id;
  const total = metrics.total;
  const moved = Number(total.consumedGrams) + Number(total.discardedGrams);

  main.innerHTML = `
    <section class="stack" aria-labelledby="numbers-title">
      <div class="numbers-head">
        <div>
          <h1 id="numbers-title">${t("numbers.title")}</h1>
          <p class="lead">${t("numbers.lead", { from: shortDay(metrics.from), to: shortDay(metrics.to) })}</p>
        </div>
        <div class="segmented" role="group" aria-label="${esc(t("numbers.period"))}">${PERIODS.map(
          (days) => `<button type="button" data-days="${days}" aria-pressed="${days === chosenDays}">${t("numbers.days", { n: days })}</button>`,
        ).join("")}</div>
      </div>
      ${
        moved === 0
          ? `<div class="empty"><h2>${t("numbers.empty-title")}</h2><p>${t("numbers.empty-lead")}</p></div>`
          : `<div class="saved">
              <div><span class="hero-number">${grams(total.rescuedGrams)}</span><span>${t("numbers.rescued")}</span></div>
              <div class="totals">
                <div><span class="data">${money(metrics.moneySaved, metrics.currency)}</span><span>${t("numbers.saved")}</span></div>
                <div><span class="data">${grams(total.consumedGrams)}</span><span>${t("numbers.eaten")}</span></div>
                <div><span class="data attention-text">${grams(total.discardedGrams)}</span><span>${t("numbers.wasted", { rate: percent(total.wasteRate) })}</span></div>
                <div><span class="data">${money(metrics.moneyWasted, metrics.currency)}</span><span>${t("numbers.lost")}</span></div>
              </div>
            </div>
            ${chart(metrics.days)}`
      }
    </section>
    ${
      metrics.members.length
        ? `<section class="stack" aria-labelledby="ranking-title"><h2 id="ranking-title">${t("numbers.ranking")}</h2>${ranking(metrics.members)}</section>`
        : ""
    }
    ${
      metrics.foods.length
        ? `<section class="stack" aria-labelledby="foods-title"><h2 id="foods-title">${t("numbers.foods")}</h2>${foods(metrics.foods)}</section>`
        : ""
    }
    <section class="stack" aria-labelledby="prices-title">
      <h2 id="prices-title">${t("numbers.prices")}</h2>
      <p class="lead">${
        metrics.unpriced.length
          ? t("numbers.unpriced", { foods: metrics.unpriced.map((food) => esc(foodName(food))).join(", ") })
          : t("numbers.reference-prices")
      }</p>
      <form class="row-form panel" data-price>
        <label>${t("field.food")}<input name="food" list="price-foods" required autocomplete="off" value="${esc(metrics.unpriced[0] ?? "")}"></label>
        <label>${t("numbers.price-per-kg")}<input class="money" name="price" type="number" min="1" step="1" required></label>
        ${foodOptions("price-foods")}
        <button class="primary" type="submit">${t("numbers.save-price")}</button>
      </form>
      <div class="row-form">
        <button type="button" data-download="markdown">${t("numbers.download-report")}</button>
        <button type="button" data-download="csv">${t("numbers.download-csv")}</button>
      </div>
    </section>`;

  main.querySelectorAll("[data-days]").forEach((button) =>
    button.addEventListener("click", () => {
      chosenDays = Number(button.dataset.days);
      renderAnalytics(main, household);
    }),
  );
  main.querySelectorAll("[data-download]").forEach((button) =>
    button.addEventListener("click", () => download(base, button.dataset.download, metrics.from, metrics.to)),
  );
  const price = main.querySelector("[data-price]");
  wireFoodSearch(price.querySelector("[name=food]"), api);
  price.addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formData(price);
    try {
      await api(`${base}/prices`, { method: "PUT", body: { food: values.food, pricePerKg: Number(values.price) } });
      toast(t("numbers.toast-price", { food: foodName(values.food) }));
      renderAnalytics(main, household);
    } catch (error) {
      showError(price, error);
    }
  });
}
