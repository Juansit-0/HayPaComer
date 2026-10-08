import { api } from "./api.js";
import { esc, foodOptions, formData, grams, showError, statusPill, toast, whenText, wireFoodSearch } from "./ui.js";

let selectedTray = null;

function twinLine(twin) {
  const door =
    twin.doorOpen === null || twin.doorOpen === undefined
      ? `<span class="status quiet">Door unknown</span>`
      : twin.doorOpen
        ? `<span class="status attention">Door open</span>`
        : `<span class="status">Door closed</span>`;
  const cold =
    twin.celsius === null || twin.celsius === undefined
      ? `<span class="lead">No temperature yet</span>`
      : `<span class="data">${Number(twin.celsius).toFixed(1)}\u2009°C</span>`;
  return `<div class="twin" data-twin="${esc(twin.fridgeId)}">${door}${cold}</div>`;
}

function cabinet(fridge, twin) {
  return `<div class="cabinet-head"><h2>${esc(fridge.name)}</h2>${twinLine(twin)}</div>
  <div class="cabinet" role="group" aria-label="${esc(fridge.name)}">${fridge.children
    .map(
      (zone) => `<div class="zone">
        <div class="zone-head"><span>${esc(zone.name)}</span><span class="data">${grams(zone.grams)}</span></div>
        ${zone.children
          .map(
            (tray) => `<button class="shelf" type="button" data-tray="${esc(tray.id)}" data-fridge="${esc(fridge.id)}" aria-pressed="${tray.id === selectedTray}">
              <span>${esc(tray.name)}</span>
              <span><span class="data">${grams(tray.grams)}</span> <span class="lead">${tray.items} ${tray.items === 1 ? "item" : "items"}</span></span>
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
      <h2 id="tray-title">${esc(tray.name)}</h2>
      <p class="lead"><span class="data">${grams(tray.grams)}</span> on this shelf.</p>
    </div>
    ${
      items.length
        ? `<ul class="list">${items
            .map(
              (item) => `<li>
                <div class="stack tight">
                  <div class="item-meta"><span class="item-name">${esc(item.name)}</span>${statusPill(item.statuses)}</div>
                  <div class="item-meta"><span class="data">${grams(item.grams)}</span><span class="lead">${
                    item.name === "Private food" ? "Belongs to another member" : whenText(item.expiresOn)
                  }</span></div>
                </div>
                ${
                  item.usable
                    ? `<form class="item-actions" data-item="${esc(item.id)}">
                        <input class="grams" name="grams" type="number" min="1" step="1" required value="${Math.min(100, Math.round(item.grams))}" aria-label="Grams of ${esc(item.name)}">
                        <button type="submit" name="action" value="consume">Use</button>
                        <button class="ghost" type="submit" name="action" value="discard">Throw away</button>
                      </form>`
                    : ""
                }
              </li>`,
            )
            .join("")}</ul>`
        : `<p class="lead">This shelf is empty.</p>`
    }
    <form class="panel stack" data-stock>
      <h3>Put food on this shelf</h3>
      <div class="row-form">
        <label>Food<input name="food" list="foods" required autocomplete="off" placeholder="Milk"></label>
        <label>Grams<input class="grams" name="grams" type="number" min="1" step="1" required></label>
        <label>Expires<input name="expiresOn" type="date"></label>
      </div>
      ${foodOptions("foods")}
      <button class="primary" type="submit">Add to fridge</button>
    </form>
  </section>`;
}

export async function renderFridge(main, household) {
  const base = `/households/${household.id}`;
  const [fridges, inventory] = await Promise.all([api(`${base}/fridges`), api(`${base}/inventory`)]);
  const twins = await Promise.all(fridges.map((fridge) => api(`${base}/fridges/${fridge.id}/twin`)));

  if (fridges.length === 0) {
    main.innerHTML = `<section class="empty" aria-labelledby="fridge-title">
      <h1 id="fridge-title">Set up your fridge</h1>
      <p>We create the usual zones and shelves: door, upper and lower shelves, and the crisper drawer. You can rename them later.</p>
      <form class="row-form" data-setup>
        <label>Fridge name<input name="name" required maxlength="60" value="Kitchen"></label>
        <button class="primary" type="submit">Set up fridge</button>
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
      <h1>Fridge</h1>
      <p class="lead"><span class="data">${grams(total)}</span> of measured food. Pick a shelf to see and change what is on it.</p>
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
    if (event.detail.kind === "alert") return;
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
          toast(`Threw away ${item.name}`);
        } else {
          const amount = Number(new FormData(form).get("grams"));
          await api(`${base}/items/${item.id}/consume`, { method: "POST", body: { grams: amount } });
          toast(`Used ${grams(amount)} of ${item.name}`);
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
        toast(`Added ${grams(values.grams)} of ${values.food}`);
        renderFridge(main, household);
      } catch (error) {
        showError(stock, error);
      }
    });
  }
}
