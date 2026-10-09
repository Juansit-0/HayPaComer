import { api } from "./api.js";
import { foodName, formatNumber, placeName, plural, t } from "./i18n.js";
import { esc, foodOptions, formData, grams, showError, statusPill, toast, whenText, wireFoodSearch } from "./ui.js";

let selectedTray = null;

function twinLine(twin) {
  const door =
    twin.doorOpen === null || twin.doorOpen === undefined
      ? `<span class="status quiet">${t("fridge.door-unknown")}</span>`
      : twin.doorOpen
        ? `<span class="status attention">${t("fridge.door-open")}</span>`
        : `<span class="status">${t("fridge.door-closed")}</span>`;
  const cold =
    twin.celsius === null || twin.celsius === undefined
      ? `<span class="lead">${t("fridge.no-temperature")}</span>`
      : `<span class="data">${formatNumber(twin.celsius, 1)}\u2009°C</span>`;
  return `<div class="twin" data-twin="${esc(twin.fridgeId)}">${door}${cold}</div>`;
}

function cabinet(fridge, twin) {
  return `<div class="cabinet-head"><h2>${esc(placeName(fridge.name))}</h2>${twinLine(twin)}</div>
  <div class="cabinet" role="group" aria-label="${esc(placeName(fridge.name))}">${fridge.children
    .map(
      (zone) => `<div class="zone">
        <div class="zone-head"><span>${esc(placeName(zone.name))}</span><span class="data">${grams(zone.grams)}</span></div>
        ${zone.children
          .map(
            (tray) => `<button class="shelf" type="button" data-tray="${esc(tray.id)}" data-fridge="${esc(fridge.id)}" aria-pressed="${tray.id === selectedTray}">
              <span>${esc(placeName(tray.name))}</span>
              <span><span class="data">${grams(tray.grams)}</span> <span class="lead">${plural("fridge.items", tray.items)}</span></span>
            </button>`,
          )
          .join("")}
      </div>`,
    )
    .join("")}</div>`;
}

function trayDetail(tray, items) {
  return `<section class="stack" aria-labelledby="tray-title">
    <div>
      <h2 id="tray-title">${esc(placeName(tray.name))}</h2>
      <p class="lead">${t("fridge.on-shelf", { grams: `<span class="data">${grams(tray.grams)}</span>` })}</p>
    </div>
    ${
      items.length
        ? `<ul class="list">${items
            .map(
              (item) => `<li>
                <div class="stack tight">
                  <div class="item-meta"><span class="item-name">${esc(foodName(item.name))}</span>${statusPill(item.statuses)}</div>
                  <div class="item-meta"><span class="data">${grams(item.grams)}</span><span class="lead">${
                    item.name === "Private food" ? t("fridge.private") : whenText(item.expiresOn)
                  }</span></div>
                </div>
                ${
                  item.usable
                    ? `<form class="item-actions" data-item="${esc(item.id)}">
                        <input class="grams" name="grams" type="number" min="1" step="1" required value="${Math.min(100, Math.round(item.grams))}" aria-label="${esc(t("fridge.grams-of", { food: foodName(item.name) }))}">
                        <button type="submit" name="action" value="consume">${t("fridge.use")}</button>
                        <button class="ghost" type="submit" name="action" value="discard">${t("fridge.discard")}</button>
                      </form>`
                    : ""
                }
              </li>`,
            )
            .join("")}</ul>`
        : `<p class="lead">${t("fridge.empty-shelf")}</p>`
    }
    <form class="panel stack" data-stock>
      <h3>${t("fridge.add-title")}</h3>
      <div class="row-form">
        <label>${t("field.food")}<input name="food" list="foods" required autocomplete="off" placeholder="${esc(t("fridge.food-example"))}"></label>
        <label>${t("field.grams")}<input class="grams" name="grams" type="number" min="1" step="1" required></label>
        <label>${t("fridge.expires")}<input name="expiresOn" type="date"></label>
      </div>
      ${foodOptions("foods")}
      <button class="primary" type="submit">${t("fridge.add")}</button>
    </form>
  </section>`;
}

export async function renderFridge(main, household) {
  const base = `/households/${household.id}`;
  const [fridges, inventory] = await Promise.all([api(`${base}/fridges`), api(`${base}/inventory`)]);
  const twins = await Promise.all(fridges.map((fridge) => api(`${base}/fridges/${fridge.id}/twin`)));

  if (fridges.length === 0) {
    main.innerHTML = `<section class="empty" aria-labelledby="fridge-title">
      <h1 id="fridge-title">${t("fridge.setup-title")}</h1>
      <p>${t("fridge.setup-lead")}</p>
      <form class="row-form" data-setup>
        <label>${t("fridge.name")}<input name="name" required maxlength="60" value="${esc(t("fridge.name-default"))}"></label>
        <button class="primary" type="submit">${t("fridge.setup")}</button>
      </form>
    </section>`;
    main.querySelector("[data-setup]").addEventListener("submit", async (event) => {
      event.preventDefault();
      try {
        await api(`${base}/fridges`, {
          method: "POST",
          body: { name: formData(event.target).name, layout: "STANDARD" },
        });
        renderFridge(main, household);
      } catch (error) {
        showError(event.target, error);
      }
    });
    return;
  }

  const trays = fridges.flatMap((fridge) =>
    fridge.children.flatMap((zone) => zone.children.map((tray) => ({ ...tray, fridgeId: fridge.id }))),
  );
  if (!trays.some((tray) => tray.id === selectedTray)) selectedTray = trays[0]?.id ?? null;
  const tray = trays.find((candidate) => candidate.id === selectedTray);
  const total = fridges.reduce((sum, fridge) => sum + Number(fridge.grams), 0);

  main.innerHTML = `
    <div>
      <h1>${t("fridge.title")}</h1>
      <p class="lead">${t("fridge.lead", { grams: `<span class="data">${grams(total)}</span>` })}</p>
    </div>
    <div class="fridge-layout">
      <div class="stack">${fridges.map((fridge, index) => cabinet(fridge, twins[index])).join("")}</div>
      ${tray ? trayDetail(tray, inventory.filter((item) => item.trayId === tray.id)) : ""}
    </div>`;

  const shown = main.querySelector(".fridge-layout");
  let pending;
  const onLive = (event) => {
    if (!document.body.contains(shown)) {
      window.removeEventListener("hpc:live", onLive);
      return;
    }
    if (event.detail.kind === "alert" || event.detail.kind === "copilot") return;
    if (main.querySelector("form :focus")) return;
    clearTimeout(pending);
    pending = setTimeout(() => {
      window.removeEventListener("hpc:live", onLive);
      renderFridge(main, household);
    }, 400);
  };
  window.addEventListener("hpc:live", onLive);

  main.querySelectorAll("[data-tray]").forEach((button) =>
    button.addEventListener("click", () => {
      selectedTray = button.dataset.tray;
      renderFridge(main, household);
    }),
  );

  main.querySelectorAll("[data-item]").forEach((form) =>
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const action = event.submitter?.value ?? "consume";
      const item = inventory.find((entry) => entry.id === form.dataset.item);
      try {
        if (action === "discard") {
          await api(`${base}/items/${item.id}/discard`, { method: "POST" });
          toast(t("fridge.toast-discarded", { food: foodName(item.name) }));
        } else {
          const amount = Number(new FormData(form).get("grams"));
          await api(`${base}/items/${item.id}/consume`, { method: "POST", body: { grams: amount } });
          toast(t("fridge.toast-used", { grams: grams(amount), food: foodName(item.name) }));
        }
        renderFridge(main, household);
      } catch (error) {
        showError(form, error);
      }
    }),
  );

  const stock = main.querySelector("[data-stock]");
  if (stock) {
    wireFoodSearch(stock.querySelector("[name=food]"), api);
    stock.addEventListener("submit", async (event) => {
      event.preventDefault();
      const values = formData(stock);
      try {
        await api(`${base}/items`, {
          method: "POST",
          body: {
            fridgeId: tray.fridgeId,
            trayId: tray.id,
            food: values.food,
            grams: Number(values.grams),
            expiresOn: values.expiresOn || null,
          },
        });
        toast(t("fridge.toast-added", { grams: grams(values.grams), food: foodName(values.food) }));
        renderFridge(main, household);
      } catch (error) {
        showError(stock, error);
      }
    });
  }
}
