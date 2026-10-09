import { api } from "./api.js";
import { currentLocale, locales, t } from "./i18n.js";
import { esc, toast } from "./ui.js";
import { commandOf, defaultLanguage, listen, speak, speechSupported, voiceSupported } from "./voice.js";

const state = { conversationId: null, messages: [], lang: null, handsFree: false, recognition: null };

function voiceLanguage() {
  return state.lang ?? (currentLocale().startsWith("es") ? "es-CO" : currentLocale().startsWith("en") ? "en-US" : defaultLanguage());
}

const SPECIALISTS = { chef: "chef.specialist.chef", market: "chef.specialist.market", cold: "chef.specialist.cold", coach: "chef.specialist.coach" };

function spoken(answer) {
  return answer.replace(/^Offline answer based on measured data:\s*/i, "").replace(/\b[a-z_]+: /g, "");
}

function message(entry) {
  if (entry.role === "user") return `<li class="said"><span class="who">${t("chef.you")}</span><p>${esc(entry.text)}</p></li>`;
  const runs = (entry.runs || []).map((run) => (SPECIALISTS[run.specialist] ? t(SPECIALISTS[run.specialist]) : run.specialist)).join(", ");
  return `<li class="answer">
    <span class="who">${esc(runs || t("chef.kitchen"))}</span>
    <p>${esc(entry.text).replace(/\n/g, "<br>")}</p>
    ${
      entry.evidence?.length
        ? `<details><summary>${t("chef.evidence")}</summary><ul>${entry.evidence
            .map((item) => `<li><span class="data">${esc(item.tool)}</span> ${esc(item.content)}</li>`)
            .join("")}</ul></details>`
        : ""
    }
  </li>`;
}

function stepCard(session) {
  if (!session) return "";
  const step = session.step;
  return `<article class="panel stack cooking" aria-label="${esc(t("chef.cooking", { recipe: session.recipe }))}">
    <div class="item-meta"><span class="item-name">${esc(session.recipe)}</span><span class="status quiet">${t("chef.step-of", { step: session.currentStep, total: session.totalSteps })}</span><span class="status">${esc(t(`chef.phase.${session.phase}`))}</span></div>
    <p class="step-text">${step ? esc(step.instruction) : t("chef.all-done")}</p>
    <div class="row-form">
      <button class="primary" type="button" data-cook="next">${t("chef.next")}</button>
      <button type="button" data-cook="${session.phase === "PAUSED" ? "resume" : "pause"}">${session.phase === "PAUSED" ? t("chef.resume") : t("chef.pause")}</button>
      <button type="button" data-cook="repeat">${t("chef.read-aloud")}</button>
    </div>
    <p class="hint">${t("chef.voice-hint")}</p>
  </article>`;
}

function confirmations(list) {
  if (!list.length) return "";
  return `<section class="stack" aria-labelledby="confirm-title">
    <h2 id="confirm-title">${t("chef.waiting")}</h2>
    <ul class="list">${list
      .map(
        (pending) => `<li><span class="item-name">${esc(pending.summary)}</span>
          <span class="item-actions"><button class="primary" type="button" data-approve="${esc(pending.id)}">${t("chef.approve")}</button><button type="button" data-reject="${esc(pending.id)}">${t("chef.reject")}</button></span></li>`,
      )
      .join("")}</ul>
  </section>`;
}

export async function renderChef(main, household) {
  const base = `/households/${household.id}`;
  const [session, pending] = await Promise.all([
    api(`${base}/cooking-sessions/active`).catch(() => null),
    api("/agent/confirmations").catch(() => []),
  ]);

  main.innerHTML = `
    <section class="stack" aria-labelledby="chef-title">
      <div>
        <h1 id="chef-title">${t("chef.title")}</h1>
        <p class="lead">${t("chef.lead")}</p>
      </div>
      ${stepCard(session)}
      ${confirmations(pending)}
      <ol class="chat" aria-live="polite">${state.messages.map(message).join("") || `<li class="lead">${t("chef.empty")}</li>`}</ol>
      <form class="ask panel" data-ask>
        <label class="grow">${t("chef.question")}<input name="message" autocomplete="off" maxlength="1000" required placeholder="${esc(t("chef.question-example"))}"></label>
        <button class="primary" type="submit">${t("chef.ask")}</button>
        <button type="button" data-mic aria-pressed="false" ${voiceSupported ? "" : "disabled"}>${t("chef.speak")}</button>
      </form>
      <div class="row-form voice-options">
        <label class="check"><input type="checkbox" data-hands-free ${state.handsFree ? "checked" : ""} ${voiceSupported ? "" : "disabled"}> ${t("chef.hands-free")}</label>
        <label>${t("chef.voice-language")}<select data-lang>${locales()
          .map((option) => {
            const value = option.code.startsWith("en") ? "en-US" : option.code;
            return `<option value="${esc(value)}" ${voiceLanguage() === value ? "selected" : ""}>${esc(option.name)}</option>`;
          })
          .join("")}</select></label>
      </div>
      ${voiceSupported ? "" : `<p class="hint">${t("chef.no-voice")}</p>`}
    </section>`;

  const form = main.querySelector("[data-ask]");
  const input = form.querySelector("[name=message]");
  const mic = main.querySelector("[data-mic]");

  async function cook(action) {
    if (!session) return false;
    if (action === "repeat") {
      speak(session.step?.instruction ?? t("chef.all-done"), voiceLanguage());
      return true;
    }
    const updated = await api(`${base}/cooking-sessions/${session.id}/${action}`, { method: "POST" }).catch((error) => {
      toast(error.message);
      return null;
    });
    if (updated) {
      if (action === "next") speak(updated.step?.instruction ?? t("chef.last-step"), voiceLanguage());
      await renderChef(main, household);
    }
    return true;
  }

  async function ask(text) {
    const command = commandOf(text);
    if (command && (await cook(command))) return;
    state.messages.push({ role: "user", text });
    input.value = "";
    try {
      const reply = await api(`${base}/agent/chat`, {
        method: "POST",
        body: { message: text, conversationId: state.conversationId },
      });
      state.conversationId = reply.conversationId;
      state.messages.push({ role: "assistant", text: reply.answer, runs: reply.runs, evidence: reply.evidence });
      if (state.handsFree || mic.getAttribute("aria-pressed") === "true") speak(spoken(reply.answer), voiceLanguage());
    } catch (error) {
      state.messages.push({ role: "assistant", text: error.message, runs: [] });
    }
    await renderChef(main, household);
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    ask(input.value.trim());
  });

  function startListening(continuous) {
    state.recognition?.stop();
    state.recognition = listen({
      lang: voiceLanguage(),
      continuous,
      onText: (text) => ask(text.trim()),
      onState: (now) => {
        mic.setAttribute("aria-pressed", String(now === "listening"));
        mic.textContent = now === "listening" ? t("chef.listening") : t("chef.speak");
        if (now === "blocked") toast(t("chef.mic-blocked"));
      },
    });
  }

  mic.addEventListener("click", () => {
    if (mic.getAttribute("aria-pressed") === "true") {
      state.recognition?.stop();
      return;
    }
    startListening(false);
  });

  main.querySelector("[data-hands-free]").addEventListener("change", (event) => {
    state.handsFree = event.target.checked;
    if (state.handsFree) startListening(true);
    else state.recognition?.stop();
  });

  main.querySelector("[data-lang]").addEventListener("change", (event) => {
    state.lang = event.target.value;
  });

  main.querySelectorAll("[data-cook]").forEach((button) => button.addEventListener("click", () => cook(button.dataset.cook)));

  main.querySelectorAll("[data-approve]").forEach((button) =>
    button.addEventListener("click", async () => {
      try {
        const result = await api(`/agent/confirmations/${button.dataset.approve}/approve`, { method: "POST" });
        toast(result.result);
      } catch (error) {
        toast(error.message);
      }
      renderChef(main, household);
    }),
  );

  main.querySelectorAll("[data-reject]").forEach((button) =>
    button.addEventListener("click", async () => {
      await api(`/agent/confirmations/${button.dataset.reject}/reject`, { method: "POST" }).catch(() => null);
      toast(t("chef.nothing-changed"));
      renderChef(main, household);
    }),
  );

  if (state.handsFree && !state.recognition) startListening(true);
  if (!speechSupported) state.handsFree = false;
}
