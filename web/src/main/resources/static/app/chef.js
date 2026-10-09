import { api } from "./api.js";
import { esc, toast } from "./ui.js";
import { commandOf, defaultLanguage, listen, speak, speechSupported, voiceSupported } from "./voice.js";

const state = { conversationId: null, messages: [], lang: defaultLanguage(), handsFree: false, recognition: null };

const SPECIALISTS = { chef: "Chef", market: "Market", cold: "Cold", coach: "Coach" };

function spoken(answer) {
  return answer.replace(/^Offline answer based on measured data:\s*/i, "").replace(/\b[a-z_]+: /g, "");
}

function message(entry) {
  if (entry.role === "user") return `<li class="said"><span class="who">You</span><p>${esc(entry.text)}</p></li>`;
  const runs = (entry.runs || []).map((run) => SPECIALISTS[run.specialist] ?? run.specialist).join(", ");
  return `<li class="answer">
    <span class="who">${esc(runs || "Kitchen")}</span>
    <p>${esc(entry.text).replace(/\n/g, "<br>")}</p>
    ${
      entry.evidence?.length
        ? `<details><summary>What this is based on</summary><ul>${entry.evidence
            .map((item) => `<li><span class="data">${esc(item.tool)}</span> ${esc(item.content)}</li>`)
            .join("")}</ul></details>`
        : ""
    }
  </li>`;
}

function stepCard(session) {
  if (!session) return "";
  const step = session.step;
  return `<article class="panel stack cooking" aria-label="Cooking ${esc(session.recipe)}">
    <div class="item-meta"><span class="item-name">${esc(session.recipe)}</span><span class="status quiet">Step ${session.currentStep} of ${session.totalSteps}</span><span class="status">${esc(session.phase.toLowerCase())}</span></div>
    <p class="step-text">${step ? esc(step.instruction) : "All steps are done."}</p>
    <div class="row-form">
      <button class="primary" type="button" data-cook="next">Next step</button>
      <button type="button" data-cook="${session.phase === "PAUSED" ? "resume" : "pause"}">${session.phase === "PAUSED" ? "Resume" : "Pause"}</button>
      <button type="button" data-cook="repeat">Read it aloud</button>
    </div>
    <p class="hint">Say "next", "pause", "go on", or "repeat" (or "siguiente", "pausa", "sigue", "repite").</p>
  </article>`;
}

function confirmations(list) {
  if (!list.length) return "";
  return `<section class="stack" aria-labelledby="confirm-title">
    <h2 id="confirm-title">Waiting for your yes</h2>
    <ul class="list">${list
      .map(
        (pending) => `<li><span class="item-name">${esc(pending.summary)}</span>
          <span class="item-actions"><button class="primary" type="button" data-approve="${esc(pending.id)}">Do it</button><button type="button" data-reject="${esc(pending.id)}">No</button></span></li>`,
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
        <h1 id="chef-title">Ask the kitchen</h1>
        <p class="lead">Answers use what is measured in your fridge. Anything that changes it waits for your yes.</p>
      </div>
      ${stepCard(session)}
      ${confirmations(pending)}
      <ol class="chat" aria-live="polite">${state.messages.map(message).join("") || `<li class="lead">Try "What can I cook tonight?" or "¿Qué compro?"</li>`}</ol>
      <form class="ask panel" data-ask>
        <label class="grow">Your question<input name="message" autocomplete="off" maxlength="1000" required placeholder="What should we use first?"></label>
        <button class="primary" type="submit">Ask</button>
        <button type="button" data-mic aria-pressed="false" ${voiceSupported ? "" : "disabled"}>Speak</button>
      </form>
      <div class="row-form voice-options">
        <label class="check"><input type="checkbox" data-hands-free ${state.handsFree ? "checked" : ""} ${voiceSupported ? "" : "disabled"}> Hands-free: keep listening and read answers aloud</label>
        <label>Language<select data-lang><option value="es-CO" ${state.lang === "es-CO" ? "selected" : ""}>Español</option><option value="en-US" ${state.lang === "en-US" ? "selected" : ""}>English</option></select></label>
      </div>
      ${voiceSupported ? "" : `<p class="hint">This browser cannot listen. Typing works everywhere; Chrome and Edge also listen.</p>`}
    </section>`;

  const form = main.querySelector("[data-ask]");
  const input = form.querySelector("[name=message]");
  const mic = main.querySelector("[data-mic]");

  async function cook(action) {
    if (!session) return false;
    if (action === "repeat") {
      speak(session.step?.instruction ?? "All steps are done.", state.lang);
      return true;
    }
    const updated = await api(`${base}/cooking-sessions/${session.id}/${action}`, { method: "POST" }).catch((error) => {
      toast(error.message);
      return null;
    });
    if (updated) {
      if (action === "next") speak(updated.step?.instruction ?? "That was the last step. Enjoy.", state.lang);
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
      if (state.handsFree || mic.getAttribute("aria-pressed") === "true") speak(spoken(reply.answer), state.lang);
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
      lang: state.lang,
      continuous,
      onText: (text) => ask(text.trim()),
      onState: (now) => {
        mic.setAttribute("aria-pressed", String(now === "listening"));
        mic.textContent = now === "listening" ? "Listening…" : "Speak";
        if (now === "blocked") toast("Allow the microphone to talk to the kitchen");
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
      toast("Nothing was changed");
      renderChef(main, household);
    }),
  );

  if (state.handsFree && !state.recognition) startListening(true);
  if (!speechSupported) state.handsFree = false;
}
