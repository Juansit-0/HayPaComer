import { api } from "./api.js";
import { currentLocale, formatNumber, loadLocale, locales, t } from "./i18n.js";
import { esc, formData, numberField, showError, toast, wireSteppers } from "./ui.js";

const DIETS = ["OMNIVORE", "PESCATARIAN", "VEGETARIAN", "VEGAN"];
const ALLERGENS = [
  "GLUTEN",
  "CRUSTACEANS",
  "EGGS",
  "FISH",
  "PEANUTS",
  "SOY",
  "MILK",
  "TREE_NUTS",
  "CELERY",
  "MUSTARD",
  "SESAME",
  "SULPHITES",
  "LUPIN",
  "MOLLUSCS",
];
const UNITS = { seconds: "s", celsius: "°C", minutes: "min", grams: "g", days: "settings.unit.days", hour: "h" };

function unitOf(key) {
  if (key.endsWith("seconds")) return UNITS.seconds;
  if (key.endsWith("celsius")) return UNITS.celsius;
  if (key.endsWith("minutes")) return UNITS.minutes;
  if (key.endsWith("grams")) return UNITS.grams;
  if (key.endsWith("days")) return t(UNITS.days);
  if (key.endsWith("hour")) return UNITS.hour;
  return "";
}

function languageTile() {
  return `<section class="tile" aria-labelledby="language-title">
    <h2 id="language-title">${t("settings.language")}</h2>
    <p class="hint">${t("settings.language-hint")}</p>
    <form data-language-form>
      <fieldset class="chips"><legend class="visually-hidden">${t("settings.language")}</legend>${locales()
        .map(
          (option) => `<label class="chip"><input type="radio" name="locale" value="${esc(option.code)}" ${
            option.code === currentLocale() ? "checked" : ""
          }><span>${esc(option.name)}</span></label>`,
        )
        .join("")}</fieldset>
    </form>
  </section>`;
}

function profileTile(profile) {
  const diet = profile?.diet ?? "OMNIVORE";
  const allergies = new Set(profile?.allergies ?? []);
  return `<section class="tile w-7" aria-labelledby="profile-title">
    <h2 id="profile-title">${t("settings.profile")}</h2>
    <p class="hint">${t("settings.profile-hint")}</p>
    <form class="stack" data-profile-form>
      <fieldset class="chips"><legend>${t("settings.diet")}</legend>${DIETS.map(
        (value) => `<label class="chip"><input type="radio" name="diet" value="${value}" ${value === diet ? "checked" : ""}><span>${t(`diet.${value}`)}</span></label>`,
      ).join("")}</fieldset>
      <fieldset class="chips"><legend>${t("settings.allergies")}</legend>${ALLERGENS.map(
        (value) => `<label class="chip"><input type="checkbox" name="allergies" value="${value}" ${allergies.has(value) ? "checked" : ""}><span>${t(`allergen.${value}`)}</span></label>`,
      ).join("")}</fieldset>
      <label>${t("settings.avoided")}<input name="avoided" autocomplete="off" value="${esc([...(profile?.avoidedFoods ?? [])].join(", "))}" placeholder="${esc(t("settings.avoided-example"))}"></label>
      <button class="primary" type="submit">${t("settings.save-profile")}</button>
    </form>
  </section>`;
}

function notificationsTile(preferences) {
  const channels = new Set(preferences?.channels ?? ["WEB"]);
  return `<section class="tile" aria-labelledby="alerts-title">
    <h2 id="alerts-title">${t("settings.alerts")}</h2>
    <p class="hint">${t("settings.alerts-hint")}</p>
    <form class="stack" data-alerts-form>
      <fieldset class="chips"><legend class="visually-hidden">${t("settings.alerts")}</legend>
        <label class="chip"><input type="checkbox" name="channels" value="WEB" ${channels.has("WEB") ? "checked" : ""}><span>${t("channel.WEB")}</span></label>
        <label class="chip"><input type="checkbox" name="channels" value="TELEGRAM" ${channels.has("TELEGRAM") ? "checked" : ""}><span>${t("channel.TELEGRAM")}</span></label>
      </fieldset>
      <label>${t("settings.telegram")}<input name="telegramChatId" inputmode="numeric" autocomplete="off" value="${esc(preferences?.telegramChatId ?? "")}"></label>
      <button type="submit">${t("settings.save-alerts")}</button>
    </form>
  </section>`;
}

function householdTile(settings, owner) {
  return `<section class="tile w-12" aria-labelledby="household-settings-title">
    <h2 id="household-settings-title">${t("settings.household")}</h2>
    <p class="hint">${owner ? t("settings.household-hint") : t("settings.household-readonly")}</p>
    <ul class="settings-list">${settings
      .map((setting) => {
        const step = setting.kind === "DECIMAL" ? 0.5 : 1;
        return `<li>
          <form class="setting-row" data-setting="${esc(setting.key)}">
            <div class="setting-text">
              <span class="item-name">${esc(setting.description)}</span>
              <span class="hint">${t("settings.default", { value: `${formatNumber(setting.defaultValue, 1)} ${unitOf(setting.key)}` })}${
                setting.overridden ? ` <span class="status">${t("settings.changed")}</span>` : ""
              }</span>
            </div>
            ${
              owner
                ? `${numberField({
                    name: "value",
                    label: t("settings.value"),
                    unit: unitOf(setting.key),
                    value: setting.value,
                    min: setting.min,
                    max: setting.max,
                    step,
                  })}
                  <div class="row-form">
                    <button type="submit" name="action" value="save">${t("settings.save")}</button>
                    ${setting.overridden ? `<button class="ghost" type="submit" name="action" value="reset" formnovalidate>${t("settings.reset")}</button>` : ""}
                  </div>`
                : `<span class="data setting-value">${formatNumber(setting.value, 1)} ${unitOf(setting.key)}</span>`
            }
          </form>
        </li>`;
      })
      .join("")}</ul>
  </section>`;
}

export async function renderSettings(main, household, rerender) {
  const base = `/households/${household.id}`;
  const [me, detail, profiles, preferences, settings] = await Promise.all([
    api("/me"),
    api(base),
    api(`${base}/profiles`).catch(() => []),
    api("/me/notification-preferences").catch(() => null),
    api(`${base}/settings`).catch(() => []),
  ]);
  const membership = detail.members.find((member) => member.userId === me.id);
  const profile = profiles.find((candidate) => candidate.memberId === membership?.memberId);
  const owner = detail.myRole === "OWNER";

  main.innerHTML = `
    <header>
      <h1>${t("settings.title")}</h1>
      <p class="lead">${t("settings.lead")}</p>
    </header>
    <div class="bento">
      ${profileTile(profile)}
      <div class="w-5 stack">
        ${languageTile()}
        ${notificationsTile(preferences)}
      </div>
      ${householdTile(settings, owner)}
    </div>`;
  wireSteppers(main);

  main.querySelector("[data-language-form]").addEventListener("change", async (event) => {
    await loadLocale(event.target.value);
    await api("/me/locale", { method: "PUT", body: { locale: currentLocale() } }).catch(() => null);
    toast(t("settings.toast-language"));
    rerender();
  });

  const profileForm = main.querySelector("[data-profile-form]");
  profileForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(profileForm);
    try {
      await api(`${base}/profile`, {
        method: "PUT",
        body: {
          diet: data.get("diet"),
          allergies: data.getAll("allergies"),
          avoidedFoods: String(data.get("avoided") ?? "")
            .split(",")
            .map((food) => food.trim())
            .filter(Boolean),
        },
      });
      toast(t("settings.toast-profile"));
    } catch (error) {
      showError(profileForm, error);
    }
  });

  const alertsForm = main.querySelector("[data-alerts-form]");
  alertsForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(alertsForm);
    try {
      await api("/me/notification-preferences", {
        method: "PUT",
        body: { channels: data.getAll("channels"), telegramChatId: formData(alertsForm).telegramChatId || null },
      });
      toast(t("settings.toast-alerts"));
    } catch (error) {
      showError(alertsForm, error);
    }
  });

  main.querySelectorAll("[data-setting]").forEach((form) =>
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const reset = event.submitter?.value === "reset";
      try {
        await api(`${base}/settings`, {
          method: "PUT",
          body: { key: form.dataset.setting, value: reset ? null : String(new FormData(form).get("value")) },
        });
        toast(reset ? t("settings.toast-reset") : t("settings.toast-saved"));
        renderSettings(main, household, rerender);
      } catch (error) {
        showError(form, error);
      }
    }),
  );
}
