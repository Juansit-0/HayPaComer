import { api, session } from "./api.js";
import { currentLocale, t } from "./i18n.js";
import { esc, formData, showError } from "./ui.js";

export function renderSignIn(main, onDone) {
  let mode = "signin";
  const draw = () => {
    const creating = mode === "register";
    main.innerHTML = `
      <section class="auth stack" aria-labelledby="auth-title">
        <div>
          <h1 id="auth-title">${creating ? t("auth.create-title") : t("auth.signin-title")}</h1>
          <p class="lead">${
            creating
              ? t("auth.create-lead")
              : t("auth.signin-lead")
          }</p>
        </div>
        <form novalidate>
          ${
            creating
              ? `<label>${t("auth.name")}<input name="displayName" autocomplete="name" required maxlength="80"></label>`
              : ""
          }
          <label>${t("auth.email")}<input name="email" type="email" autocomplete="email" required></label>
          <label>${creating ? t("auth.password-new") : t("auth.password")}
            <input name="password" type="password" autocomplete="${
              creating ? "new-password" : "current-password"
            }" required minlength="${creating ? 10 : 1}">
          </label>
          <button class="primary" type="submit">${creating ? t("auth.create") : t("auth.signin")}</button>
        </form>
        <button class="ghost" type="button" data-switch>${
          creating ? t("auth.have-account") : t("auth.create-account")
        }</button>
      </section>`;
    main.querySelector("[data-switch]").addEventListener("click", () => {
      mode = creating ? "signin" : "register";
      draw();
    });
    main.querySelector("form").addEventListener("submit", async (event) => {
      event.preventDefault();
      const values = formData(event.target);
      try {
        if (creating) {
          await api("/auth/register", { method: "POST", body: values, auth: false });
        }
        session.store(
          await api("/auth/login", {
            method: "POST",
            body: { email: values.email, password: values.password },
            auth: false,
          }),
        );
        await api("/me/locale", { method: "PUT", body: { locale: currentLocale() } }).catch(() => null);
        onDone();
      } catch (error) {
        showError(event.target, error);
      }
    });
  };
  draw();
}

export async function chooseHousehold(main, onDone) {
  const households = await api("/households");
  if (households.length > 0) {
    if (!households.some((household) => household.id === session.household)) {
      session.household = households[0].id;
    }
    onDone(households.find((household) => household.id === session.household));
    return;
  }
  main.innerHTML = `
    <section class="auth stack" aria-labelledby="home-title">
      <div>
        <h1 id="home-title">${t("home.title")}</h1>
        <p class="lead">${t("home.lead")}</p>
      </div>
      <form>
        <label>${t("home.name")}<input name="name" required maxlength="60" placeholder="${esc(t("home.name-example"))}"></label>
        <label>${t("home.currency")}<input name="currency" required value="COP" maxlength="3" class="grams"></label>
        <button class="primary" type="submit">${t("home.create")}</button>
      </form>
    </section>`;
  main.querySelector("form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formData(event.target);
    try {
      const household = await api("/households", {
        method: "POST",
        body: {
          name: values.name,
          currency: values.currency.toUpperCase(),
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        },
      });
      session.household = household.id;
      onDone(household);
    } catch (error) {
      showError(event.target, error);
    }
  });
}

export function householdLabel(household) {
  return esc(household?.name ?? "");
}
