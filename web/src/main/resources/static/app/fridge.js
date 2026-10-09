import { api, freshAccess } from "./api.js";
import { foodName, formatDay, formatNumber, placeName, plural, t } from "./i18n.js";
import {
  esc,
  expiryText,
  formData,
  grams,
  numberField,
  openSheet,
  showError,
  statusPill,
  toast,
} from "./ui.js";

let selectedTray = null;

const CATEGORY_ORDER = [
  "POULTRY",
  "MEAT",
  "FISH",
  "DAIRY",
  "EGGS",
  "VEGETABLE",
  "FRUIT",
  "PREPARED",
  "GRAIN",
  "LEGUME",
  "BEVERAGE",
  "CONDIMENT",
  "OTHER",
];

function twinLine(twin) {
  const door =
    twin.doorOpen === null || twin.doorOpen === undefined
      ? `<span class="status quiet">${t("fridge.door-unknown")}</span>`
      : twin.doorOpen
        ? `<span class="status attention">${t("fridge.door-open")}</span>`
        : `<span class="status">${t("fridge.door-closed")}</span>`;
  const cold =
    twin.celsius === null || twin.celsius === undefined
      ? `<span class="hint">${t("fridge.no-temperature")}</span>`
      : `<span class="data">${formatNumber(twin.celsius, 1)} °C</span>`;
  return `<div class="twin" data-twin="${esc(twin.fridgeId)}">${door}${cold}</div>`;
}

function cabinet(fridge, twin) {
  return `<section class="tile w-4" aria-label="${esc(placeName(fridge.name))}">
    <header><h2>${esc(placeName(fridge.name))}</h2>${twinLine(twin)}</header>
    <div class="cabinet" role="group" aria-label="${esc(placeName(fridge.name))}">${fridge.children
      .map(
        (zone) => `<div class="zone">
          <div class="zone-head"><span>${esc(placeName(zone.name))}</span><span class="data">${grams(zone.grams)}</span></div>
          ${zone.children
            .map(
              (tray) => `<button class="shelf" type="button" data-tray="${esc(tray.id)}" aria-pressed="${tray.id === selectedTray}">
                <span>${esc(placeName(tray.name))}</span>
                <span class="shelf-meta"><span class="data">${grams(tray.grams)}</span><span class="badge" aria-label="${esc(plural("fridge.items", tray.items))}">${tray.items}</span></span>
              </button>`,
            )
            .join("")}
        </div>`,
      )
      .join("")}</div>
  </section>`;
}

function foodCard(item) {
  const hidden = item.name === "Private food";
  return `<li class="food-card">
    <span class="value">${grams(item.grams)}</span>
    <span class="item-name">${esc(foodName(item.name))}</span>
    <div class="item-meta">${statusPill(item.statuses)}<span class="hint">${
      hidden ? t("fridge.private") : expiryText(item)
    }</span></div>
    ${
      item.usable
        ? `<form class="card-actions" data-item="${esc(item.id)}">
            <label class="visually-hidden" for="use-${esc(item.id)}">${esc(t("fridge.grams-of", { food: foodName(item.name) }))}</label>
            <input id="use-${esc(item.id)}" class="grams" name="grams" type="number" inputmode="numeric" min="1" step="1" required value="${Math.min(100, Math.round(item.grams))}">
            <button type="submit" name="action" value="consume">${t("fridge.use")}</button>
            ${item.openedOn ? "" : `<button class="ghost" type="submit" name="action" value="open" formnovalidate>${t("fridge.opened")}</button>`}
            <button class="ghost" type="submit" name="action" value="discard" formnovalidate>${t("fridge.discard")}</button>
          </form>`
        : ""
    }
  </li>`;
}

function shelfTile(tray, items) {
  return `<section class="tile w-8" aria-labelledby="tray-title">
    <header>
      <div>
        <h2 id="tray-title">${esc(placeName(tray.name))}</h2>
        <p class="hint">${t("fridge.on-shelf", { grams: grams(tray.grams) })}</p>
      </div>
      <button class="primary" type="button" data-add-food>${t("fridge.add")}</button>
    </header>
    ${
      items.length
        ? `<ul class="food-cards">${items.map(foodCard).join("")}</ul>`
        : `<div class="empty"><p>${t("fridge.empty-shelf")}</p></div>`
    }
  </section>`;
}

function trayOptions(fridges) {
  return fridges.flatMap((fridge) =>
    fridge.children.flatMap((zone) =>
      zone.children.map((tray) => ({
        id: tray.id,
        fridgeId: fridge.id,
        zone: zone.kind ?? "SHELF",
        label: `${placeName(zone.name)}: ${placeName(tray.name)}`,
      })),
    ),
  );
}

function addFoodBody(catalog, trays, trayId) {
  const groups = new Map();
  catalog.forEach((food) => {
    const list = groups.get(food.category) ?? [];
    list.push(food);
    groups.set(food.category, list);
  });
  return `<form class="add-food" data-add-form novalidate>
    <fieldset class="step">
      <legend>${t("add.step.food")}</legend>
      <label for="add-search" class="visually-hidden">${t("add.search")}</label>
      <input id="add-search" type="search" data-food-search autocomplete="off" placeholder="${esc(t("add.search"))}">
      <div class="food-groups" data-food-groups>${[...groups.entries()]
        .sort(([left], [right]) => CATEGORY_ORDER.indexOf(left) - CATEGORY_ORDER.indexOf(right))
        .map(
          ([category, foods]) => `<div class="food-group" data-group>
            <h3>${t(`category.${category}`)}</h3>
            <div class="chips">${foods
              .map(
                (food) => `<label class="chip" data-chip="${esc(`${food.name} ${foodName(food.name)}`.toLowerCase())}"><input type="radio" name="food" value="${esc(food.name)}" required><span>${esc(foodName(food.name))}</span></label>`,
              )
              .join("")}</div>
          </div>`,
        )
        .join("")}</div>
    </fieldset>
    <fieldset class="step">
      <legend>${t("add.step.amount")}</legend>
      ${numberField({ name: "grams", label: t("field.grams"), unit: "g", min: 1, bigStep: 50, value: "" })}
    </fieldset>
    <fieldset class="step">
      <legend>${t("add.step.where")}</legend>
      <div class="chips">${trays
        .map(
          (tray) => `<label class="chip"><input type="radio" name="tray" value="${esc(tray.id)}" ${tray.id === trayId ? "checked" : ""}><span>${esc(tray.label)}</span></label>`,
        )
        .join("")}</div>
      <label class="check"><input type="checkbox" name="opened">${t("add.opened")}</label>
    </fieldset>
    <fieldset class="step">
      <legend>${t("add.step.expiry")}</legend>
      <div class="chips">
        <label class="chip"><input type="radio" name="expiryMode" value="estimate" checked><span>${t("add.expiry.estimate")}</span></label>
        <label class="chip"><input type="radio" name="expiryMode" value="known"><span>${t("add.expiry.known")}</span></label>
        <label class="chip"><input type="radio" name="expiryMode" value="photo"><span>${t("add.expiry.photo")}</span></label>
      </div>
      <div data-mode="known" hidden>
        <label>${t("fridge.expires")}<input type="date" name="expiresOn"></label>
      </div>
      <div data-mode="photo" hidden>
        <label>${t("add.photo")}<input type="file" name="photo" accept="image/jpeg,image/png,image/webp" capture="environment"></label>
      </div>
      <p class="expiry-preview" data-preview aria-live="polite"></p>
    </fieldset>
    <button class="primary" type="submit">${t("fridge.add")}</button>
  </form>`;
}

async function readLabel(base, values, file, tray) {
  const body = new FormData();
  body.append("photo", file);
  body.append("food", values.food);
  body.append("zone", tray.zone);
  body.append("opened", String(Boolean(values.opened)));
  const access = await freshAccess();
  const response = await fetch(`/api/v1${base}/items/expiry-from-photo`, {
    method: "POST",
    headers: { Authorization: `Bearer ${access}` },
    body,
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.detail || payload.title || t("error.request"));
  return payload;
}

function wireAddFood(dialog, base, trays, onDone) {
  const form = dialog.querySelector("[data-add-form]");
  const preview = form.querySelector("[data-preview]");
  const search = form.querySelector("[data-food-search]");
  let proposal = null;

  search.addEventListener("input", () => {
    const query = search.value.trim().toLowerCase().normalize("NFD").replace(/\p{M}/gu, "");
    form.querySelectorAll("[data-chip]").forEach((chip) => {
      const text = chip.dataset.chip.normalize("NFD").replace(/\p{M}/gu, "");
      chip.hidden = Boolean(query) && !text.includes(query);
    });
    form.querySelectorAll("[data-group]").forEach((group) => {
      group.hidden = [...group.querySelectorAll("[data-chip]")].every((chip) => chip.hidden);
    });
  });

  const mode = () => form.querySelector("[name=expiryMode]:checked").value;
  const chosenTray = () => trays.find((tray) => tray.id === form.querySelector("[name=tray]:checked")?.value);

  async function refresh() {
    const values = formData(form);
    form.querySelectorAll("[data-mode]").forEach((box) => {
      box.hidden = box.dataset.mode !== mode();
    });
    proposal = null;
    preview.textContent = "";
    const tray = chosenTray();
    if (mode() !== "estimate" || !values.food || !tray) return;
    try {
      const estimate = await api(
        `${base}/expiry-estimate?food=${encodeURIComponent(values.food)}&zone=${tray.zone}&opened=${Boolean(values.opened)}`,
      );
      preview.textContent = t("add.estimate-result", {
        date: formatDay(estimate.expiresOn, { day: "numeric", month: "long" }),
        days: estimate.shelfDays,
      });
    } catch (error) {
      preview.textContent = error.message;
    }
  }

  form.addEventListener("change", async (event) => {
    if (event.target.name === "photo" && event.target.files[0]) {
      const tray = chosenTray();
      const values = formData(form);
      if (!values.food || !tray) {
        preview.textContent = t("add.photo-needs-food");
        return;
      }
      preview.textContent = t("add.photo-reading");
      try {
        proposal = await readLabel(base, values, event.target.files[0], tray);
        preview.textContent = t("add.photo-result", {
          date: formatDay(proposal.expiresOn, { day: "numeric", month: "long" }),
          reason: proposal.reason,
        });
      } catch (error) {
        preview.textContent = error.message;
      }
      return;
    }
    if (event.target.name !== "expiresOn") await refresh();
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formData(form);
    const tray = chosenTray();
    if (!values.food || !tray || !Number(values.grams)) {
      showError(form, { message: t("add.missing") });
      return;
    }
    const body = {
      fridgeId: tray.fridgeId,
      trayId: tray.id,
      food: values.food,
      grams: Number(values.grams),
      opened: Boolean(values.opened),
      expiresOn: null,
      expirySource: null,
    };
    if (mode() === "known" && values.expiresOn) {
      body.expiresOn = values.expiresOn;
      body.expirySource = "USER";
    }
    if (mode() === "photo" && proposal?.source === "LABEL") {
      body.expiresOn = proposal.expiresOn;
      body.expirySource = "LABEL";
    }
    try {
      await api(`${base}/items`, { method: "POST", body });
      dialog.close();
      toast(t("fridge.toast-added", { grams: grams(body.grams), food: foodName(body.food) }));
      onDone(tray.id);
    } catch (error) {
      showError(form, error);
    }
  });
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

  const trays = trayOptions(fridges);
  if (!trays.some((tray) => tray.id === selectedTray)) selectedTray = trays[0]?.id ?? null;
  const tray = fridges
    .flatMap((fridge) => fridge.children.flatMap((zone) => zone.children))
    .find((candidate) => candidate.id === selectedTray);
  const total = fridges.reduce((sum, fridge) => sum + Number(fridge.grams), 0);

  main.innerHTML = `
    <header>
      <h1>${t("fridge.title")}</h1>
      <p class="lead">${t("fridge.lead", { grams: `<span class="data">${grams(total)}</span>` })}</p>
    </header>
    <div class="bento fridge-bento">
      ${fridges.map((fridge, index) => cabinet(fridge, twins[index])).join("")}
      ${tray ? shelfTile(tray, inventory.filter((item) => item.trayId === tray.id)) : ""}
    </div>`;

  const shown = main.querySelector(".fridge-bento");
  let pending;
  const onLive = (event) => {
    if (!document.body.contains(shown)) {
      window.removeEventListener("hpc:live", onLive);
      return;
    }
    if (event.detail.kind === "alert" || event.detail.kind === "copilot") return;
    if (main.querySelector("form :focus") || document.querySelector("dialog[open]")) return;
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
        } else if (action === "open") {
          const opened = await api(`${base}/items/${item.id}/open`, { method: "POST" });
          toast(
            t("fridge.toast-opened", {
              food: foodName(item.name),
              date: formatDay(opened.expiresOn, { day: "numeric", month: "long" }),
            }),
          );
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

  main.querySelector("[data-add-food]")?.addEventListener("click", async () => {
    const catalog = await api("/foods?q=").catch(() => []);
    openSheet(t("fridge.add-title"), addFoodBody(catalog, trays, selectedTray), (dialog) => {
      wireAddFood(dialog, base, trays, (trayId) => {
        selectedTray = trayId;
        renderFridge(main, household);
      });
    });
  });
}
