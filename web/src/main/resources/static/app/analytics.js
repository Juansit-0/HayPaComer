import { api, session } from "./api.js";
import { esc, formData, grams, showError, toast, wireFoodSearch, foodOptions } from "./ui.js";

const PERIODS = [7, 30, 90];
let chosenDays = 30;
let me = null;

function money(value, currency) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency, maximumFractionDigits: 0 }).format(Number(value));
}

function percent(rate) {
  return `${(rate * 100).toFixed(rate > 0 && rate < 0.1 ? 1 : 0)}%`;
}

function isoDay(date) {
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function shortDay(iso) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { day: "numeric", month: "short" });
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
      <span class="key rescued">Rescued before expiring</span>
      <span class="key eaten">Eaten</span>
      <span class="key wasted">Thrown away, below the line</span>
    </figcaption>
    <table class="visually-hidden">
      <caption>Grams per day</caption>
      <thead><tr><th>Day</th><th>Eaten</th><th>Rescued</th><th>Thrown away</th></tr></thead>
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
        <span class="item-name">${esc(member.name)}${member.userId === me ? ` <span class="status quiet">You</span>` : ""}</span>
        <span class="meter" aria-hidden="true"><span style="width:${((Number(member.tally.rescuedGrams) / best) * 100).toFixed(0)}%"></span></span>
        <span class="data">${grams(member.tally.rescuedGrams)} rescued</span>
        <span class="item-meta">${grams(member.tally.consumedGrams)} eaten, ${grams(member.tally.discardedGrams)} thrown away</span>
      </li>`,
    )
    .join("")}</ol>`;
}

function foods(list) {
  return `<ul class="list">${list
    .slice(0, 8)
    .map(
      (food) => `<li>
        <span class="item-name">${food.foodKey === "unknown" ? "Not recorded (older moves)" : esc(food.foodKey.charAt(0).toUpperCase() + food.foodKey.slice(1))}</span>
        <span class="item-meta">${
          Number(food.tally.consumedGrams) ? `<span class="data">${grams(food.tally.consumedGrams)}</span> eaten` : ""
        }${
          Number(food.tally.discardedGrams) ? `<span class="status attention">${grams(food.tally.discardedGrams)} thrown away</span>` : ""
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
    toast("The report did not download. Try again.");
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
          <h1 id="numbers-title">What your fridge saved</h1>
          <p class="lead">Measured on the scale and the inventory, ${shortDay(metrics.from)} to ${shortDay(metrics.to)}.</p>
        </div>
        <div class="segmented" role="group" aria-label="Period">${PERIODS.map(
          (days) => `<button type="button" data-days="${days}" aria-pressed="${days === chosenDays}">${days} days</button>`,
        ).join("")}</div>
      </div>
      ${
        moved === 0
          ? `<div class="empty"><h2>No food has left the fridge in this period</h2><p>Use or throw away food from the Fridge screen, or weigh it on the scale, and the numbers start here.</p></div>`
          : `<div class="saved">
              <div><span class="hero-number">${grams(total.rescuedGrams)}</span><span>rescued before expiring</span></div>
              <div class="totals">
                <div><span class="data">${money(metrics.moneySaved, metrics.currency)}</span><span>saved</span></div>
                <div><span class="data">${grams(total.consumedGrams)}</span><span>eaten</span></div>
                <div><span class="data attention-text">${grams(total.discardedGrams)}</span><span>thrown away, ${percent(total.wasteRate)} of what left the fridge</span></div>
                <div><span class="data">${money(metrics.moneyWasted, metrics.currency)}</span><span>lost to waste</span></div>
              </div>
            </div>
            ${chart(metrics.days)}`
      }
    </section>
    ${
      metrics.members.length
        ? `<section class="stack" aria-labelledby="ranking-title"><h2 id="ranking-title">Who rescued the most</h2>${ranking(metrics.members)}</section>`
        : ""
    }
    ${
      metrics.foods.length
        ? `<section class="stack" aria-labelledby="foods-title"><h2 id="foods-title">Foods</h2>${foods(metrics.foods)}</section>`
        : ""
    }
    <section class="stack" aria-labelledby="prices-title">
      <h2 id="prices-title">Prices</h2>
      <p class="lead">${
        metrics.unpriced.length
          ? `Money leaves out ${metrics.unpriced.map(esc).join(", ")} until they have a price per kilogram.`
          : "Money uses reference prices per kilogram. Set your own to match your market."
      }</p>
      <form class="row-form panel" data-price>
        <label>Food<input name="food" list="price-foods" required autocomplete="off" value="${esc(metrics.unpriced[0] ?? "")}"></label>
        <label>Price per kg<input class="grams" name="price" type="number" min="1" step="1" required></label>
        ${foodOptions("price-foods")}
        <button class="primary" type="submit">Save price</button>
      </form>
      <div class="row-form">
        <button type="button" data-download="markdown">Download report</button>
        <button type="button" data-download="csv">Download CSV</button>
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
      toast(`Saved the price of ${values.food}`);
      renderAnalytics(main, household);
    } catch (error) {
      showError(price, error);
    }
  });
}
