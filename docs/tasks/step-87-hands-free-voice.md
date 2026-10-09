# Step 87: Hands-free voice in the kitchen (Web Speech)

Commit and pull request title: `feat(agent): hands-free voice in the kitchen (web speech)`

## Goal

Hands covered in flour should not stop anyone from asking the kitchen or moving to the next step. A Chef screen lets people talk or type to the agent, hears the answer out loud, and drives a guided cooking session with a few spoken words, all in the browser with no audio sent to any server.

## Scope

- `app/voice.js`:
  - wraps the Web Speech API (`SpeechRecognition` and `speechSynthesis`) with honest support flags;
  - reads at most 400 characters aloud;
  - recognizes local commands in English and Spanish: next, pause, resume, and repeat ("siguiente", "pausa", "sigue", "repite"), after normalizing accents and punctuation;
  - picks Spanish (Colombia) or English from the browser language.
- `app/chef.js`, a new "Chef" tab:
  - questions by text or microphone go to `POST /households/{h}/agent/chat`, keeping the conversation;
  - answers show the specialists, the text, and "What this is based on" (the evidence);
  - hands-free mode keeps listening and reads answers aloud, with a language switch;
  - when a cooking session is active, a step card offers next, pause or resume, and read aloud, and the spoken commands control it;
  - pending confirmations show "Do it" and "No";
  - without speech support the screen says so and typing still works.
- Mobile: five tabs share the bottom bar.
- Speech recognition runs in the browser; only the recognized text reaches the backend, and every write still needs a confirmation.

## Tests (definition of done)

- `StaticUiIntegrationTest` serves `chef.js`, `voice.js`, and the Chef tab.
- Checked in the browser against the real app:
  - a Spanish question answered from measured stock;
  - the command parser ("Siguiente." is next, "¡Repite!" is repeat, a question is not a command);
  - speech support detected;
  - five tabs fit on a phone.
