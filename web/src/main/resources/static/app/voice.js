const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;

export const voiceSupported = Boolean(Recognition);
export const speechSupported = "speechSynthesis" in window;

const COMMANDS = [
  ["next", /^(next|next step|done|siguiente|siguiente paso|listo|ya)$/],
  ["pause", /^(pause|wait|pausa|espera|para)$/],
  ["resume", /^(resume|continue|go on|sigue|continua|reanuda)$/],
  ["repeat", /^(repeat|again|read it|repite|otra vez|lee el paso)$/],
];

export function normalize(text) {
  return text
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[.,!?¿¡]/g, "")
    .trim();
}

export function commandOf(text) {
  const said = normalize(text);
  const match = COMMANDS.find(([, pattern]) => pattern.test(said));
  return match ? match[0] : null;
}

export function defaultLanguage() {
  return (navigator.language || "en").toLowerCase().startsWith("es") ? "es-CO" : "en-US";
}

export function speak(text, lang) {
  if (!speechSupported) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text.length > 400 ? `${text.slice(0, 400)}…` : text);
  utterance.lang = lang;
  window.speechSynthesis.speak(utterance);
}

export function listen({ lang, continuous, onText, onState }) {
  if (!voiceSupported) return null;
  const recognition = new Recognition();
  recognition.lang = lang;
  recognition.continuous = continuous;
  recognition.interimResults = false;
  recognition.onresult = (event) => {
    const result = event.results[event.results.length - 1];
    if (result.isFinal) onText(result[0].transcript);
  };
  recognition.onstart = () => onState("listening");
  recognition.onend = () => onState("idle");
  recognition.onerror = (event) => onState(event.error === "not-allowed" ? "blocked" : "idle");
  recognition.start();
  return recognition;
}
