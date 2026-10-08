import { api, session } from "./api.js";
import { esc, formData, showError } from "./ui.js";

export function renderSignIn(main, onDone) {
  let mode = "signin";
  const draw = () => {
    const creating = mode === "register";
    main.innerHTML = `
      <section class="auth stack" aria-labelledby="auth-title">
        <div>
          <h1 id="auth-title">${creating ? "Create your account" : "What can you cook right now?"}</h1>
          <p class="lead">${
            creating
              ? "One account per person. You can share a fridge with your household after this."
              : "Sign in to see what is in your fridge, in grams, and what to use first."
          }</p>
        </div>
        <form novalidate>
          ${
            creating
              ? `<label>Name<input name="displayName" autocomplete="name" required maxlength="80"></label>`
              : ""
          }
          <label>Email<input name="email" type="email" autocomplete="email" required></label>
          <label>Password${creating ? " (at least 10 characters)" : ""}
            <input name="password" type="password" autocomplete="${
              creating ? "new-password" : "current-password"
            }" required minlength="${creating ? 10 : 1}">
          </label>
          <button class="primary" type="submit">${creating ? "Create account" : "Sign in"}</button>
        </form>
        <button class="ghost" type="button" data-switch>${
          creating ? "I already have an account" : "Create an account"
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
        <h1 id="home-title">Name your home</h1>
        <p class="lead">Your household shares one fridge, one market list, and the same alerts.</p>
      </div>
      <form>
        <label>Household name<input name="name" required maxlength="60" placeholder="Apartment 402"></label>
        <label>Currency<input name="currency" required value="COP" maxlength="3" class="grams"></label>
        <button class="primary" type="submit">Create household</button>
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
