# HayPaComer firmware

Presentation mirror of the firmware of **HayPaComer**, the smart fridge that answers "what can I cook right now with what is actually at home?". Development happens in the monorepo [Juansit-0/HayPaComer](https://github.com/Juansit-0/HayPaComer); this repository is a read-only view updated with `scripts/mirrors.sh` from that monorepo.

## What is here

- `esp32/door_temp/`: debounced reed switch, DS18B20 temperature every 30 s, NTP-stamped envelopes, batched upload with backoff, buzzer and RGB commands;
- `esp32/scale/`: HX711 load cell at 10 Hz, median and local stability, `RAW_WEIGHT` on stable change plus heartbeat, cooking and fridge modes;
- `docs/firmware.md`: wiring, libraries, and flashing notes;
- `docs/hardware.md`: bill of materials with links.

Secrets live in a git-ignored `secrets.h`; `secrets.example.h` shows the shape. Without hardware, the monorepo scripts under `scripts/demo` simulate the same events against the API.
