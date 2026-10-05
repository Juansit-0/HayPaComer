# Firmware

ESP32 sketches that speak the event protocol (`docs/event-protocol.md`) with the device key issued by the API.

## Door and temperature (`firmware/esp32/door_temp`)

| Part | Pin | Behavior |
|---|---|---|
| Reed switch (MC-38) | GPIO 4, `INPUT_PULLUP` | Debounced 50 ms; every change sends a `DOOR` event (`HIGH` = magnet away = open) |
| DS18B20 probe | GPIO 5, 4.7 k pull-up | Reading every 30 s sends a `TEMPERATURE` event; disconnected probes are skipped |
| Buzzer | GPIO 14 | Plays the commands pulled from the API |
| RGB LED | GPIO 25, 26, 27 | Red: door alert; red and blue: cold-chain alarm; green: weight confirmed |

- Events carry a random UUID v4 `eventId` and an NTP timestamp; nothing is sent until the clock is synchronized.
- Events wait in a 64-entry ring buffer and are posted in batches of up to 20 to `POST /api/v1/device/events` every 2 s; network failures back off exponentially up to 60 s and keep the buffer, so the server's idempotency makes retries safe. A `400` drops the batch so a bad event cannot block the queue.
- Every 3 s the device pulls `GET /api/v1/device/commands` and plays `DOOR_OPEN_BEEP`, `COLD_CHAIN_ALARM`, or `WEIGHT_CONFIRMED_BLINK`.
- HTTPS requires `ROOT_CA`; plain HTTP is meant for the local demo network only.

## Scale (`firmware/esp32/scale`)

| Part | Pin | Behavior |
|---|---|---|
| HX711 DOUT | GPIO 32 | Raw counts sampled at 10 Hz, median of the last 5 samples |
| HX711 SCK | GPIO 33 | Clock |
| Buzzer, RGB LED | GPIO 14, 25, 26, 27 | Short beep and green blink on `WEIGHT_CONFIRMED_BLINK` |

- The sketch sends `RAW_WEIGHT` envelopes with raw counts; tare, calibration, conversion to grams, and fridge or cooking mode live in the backend, so recalibrating never needs a reflash.
- A reading is stable after 1 s within 800 counts (about 2 g at 428 counts per gram); a new stable value that moved at least 2000 counts is sent at once, and a heartbeat every 5 s keeps the backend sample fresh for tare and calibration.
- Register the device with kind `ESP32_SCALE`, tare the empty scale from the app, place a known weight (for example 500 g), and calibrate.

## Flashing

1. Register the device in the API (`POST /api/v1/households/{h}/devices`, kind `ESP32_DOOR_TEMP`) and copy the `apiKey` shown once.
2. Copy `secrets.example.h` to `secrets.h` in the sketch folder and fill WiFi, `API_BASE_URL`, and `DEVICE_KEY`; `secrets.h` is git-ignored.
3. Install the ESP32 board package (`https://espressif.github.io/arduino-esp32/package_esp32_index.json`) and the libraries OneWire, DallasTemperature, ArduinoJson, and HX711 (bogde).
4. Flash with the Arduino IDE (board "ESP32 Dev Module") or:

```bash
arduino-cli compile --fqbn esp32:esp32:esp32 firmware/esp32/door_temp
```

```bash
arduino-cli upload -p /dev/cu.usbserial-0001 --fqbn esp32:esp32:esp32 firmware/esp32/door_temp
```

CI compiles every sketch on each pull request (job `firmware`).
