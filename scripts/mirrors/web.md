# HayPaComer web

Presentation mirror of the web UI of **HayPaComer**, the smart fridge that answers "what can I cook right now with what is actually at home?". Development happens in the monorepo [Juansit-0/HayPaComer](https://github.com/Juansit-0/HayPaComer); this repository is a read-only view updated with `scripts/mirrors.sh` from that monorepo.

## What is here

The interface served by Spring Boot, written as vanilla ES modules with no build step:

- `index.html` and `app/`: the five screens (Now, Fridge, Market, Numbers, Chef) plus Settings, live updates over SSE, voice commands, and the language switch;
- `assets/`: brand tokens, logos, and the design notes used at build time.

In the monorepo this lives in `web/src/main/resources/static` and `brand/assets`.

## Run

The UI is served by the backend at `http://localhost:8080`; see the monorepo README for the full setup.
