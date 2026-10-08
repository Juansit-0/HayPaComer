import { session } from "./api.js";

const recent = [];
let controller = null;
let householdId = null;

export function liveFeed() {
  return recent;
}

function dispatch(kind, data) {
  const update = { ...data, kind };
  recent.unshift(update);
  recent.length = Math.min(recent.length, 8);
  window.dispatchEvent(new CustomEvent("hpc:live", { detail: update }));
}

function parse(block) {
  let event = "message";
  const data = [];
  block.split("\n").forEach((line) => {
    if (line.startsWith("event:")) event = line.slice(6).trim();
    if (line.startsWith("data:")) data.push(line.slice(5).trim());
  });
  if (event === "ready" || data.length === 0) return;
  dispatch(event, JSON.parse(data.join("\n")));
}

async function listen(id, signal) {
  const response = await fetch(`/api/v1/households/${id}/stream`, {
    headers: { Accept: "text/event-stream", Authorization: `Bearer ${session.access}` },
    signal,
  });
  if (!response.ok || !response.body) throw new Error(`stream ${response.status}`);
  window.dispatchEvent(new CustomEvent("hpc:live-state", { detail: "on" }));
  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = "";
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += value;
    let split;
    while ((split = buffer.indexOf("\n\n")) >= 0) {
      parse(buffer.slice(0, split));
      buffer = buffer.slice(split + 2);
    }
  }
}

export function connectLive(id) {
  if (householdId === id && controller) return;
  disconnectLive();
  householdId = id;
  controller = new AbortController();
  const signal = controller.signal;
  const loop = async (delay) => {
    if (signal.aborted || !session.signedIn) return;
    try {
      await listen(id, signal);
      delay = 1000;
    } catch {
      delay = Math.min(delay * 2, 30000);
    }
    if (signal.aborted) return;
    window.dispatchEvent(new CustomEvent("hpc:live-state", { detail: "off" }));
    setTimeout(() => loop(delay), delay);
  };
  loop(1000);
}

export function disconnectLive() {
  controller?.abort();
  controller = null;
  householdId = null;
  recent.length = 0;
}
