# Hardware Bill of Materials (Colombia)

Prototype hardware for HayPaComer: ESP32 module, door contact, temperature probe, scale, signaling, and bench accessories. Prices are reference values in COP from September 2026 and change often; always confirm the current price, seller reputation, and shipping on the listing. MercadoLibre links point to search listings on `mercadolibre.com.co` because individual product IDs rotate; pick a seller with high reputation and buyer protection.

## Core electronics

| # | Item | Suggested spec | Qty | Ref price (COP) | MercadoLibre Colombia |
|---|---|---|---|---|---|
| 1 | ESP32 DevKit V1 | ESP-WROOM-32, 30 or 38 pins, CH340 or CP2102 USB | 1 | 25,000 - 40,000 | https://listado.mercadolibre.com.co/esp32-devkit-v1 |
| 2 | Door contact (reed switch + magnet) | MC-38 NO or NC, 15-25 mm gap, screw mount | 1 | 7,000 - 15,000 | https://listado.mercadolibre.com.co/sensor-magnetico-mc-38 |
| 3 | Temperature probe | DS18B20 waterproof, 1 m or 2 m cable, stainless tip | 1 | 5,000 - 15,000 | https://listado.mercadolibre.com.co/ds18b20-sonda-impermeable |
| 4 | Scale kit | Load cell 5 kg (aluminum, IP65) + HX711 24-bit module | 1 | 18,000 - 25,000 | https://listado.mercadolibre.com.co/hx711-celda-de-carga-5kg |
| 5 | Buzzer | Active 5 V, 12 x 9.5 mm, 85 dB | 1 | 1,000 - 3,000 | https://listado.mercadolibre.com.co/buzzer-activo-5v |
| 6 | RGB LED module | 10 mm full color, common cathode, 5 V, 3 channels | 1 | 3,000 - 6,000 | https://listado.mercadolibre.com.co/led-rgb-modulo |
| 7 | Power supply | 5 V 1 A minimum (2 A preferred) USB adapter + micro-USB or USB-C cable | 1 | 15,000 - 30,000 | https://listado.mercadolibre.com.co/fuente-5v-2a-usb |

## Bench and installation

| # | Item | Suggested spec | Qty | Ref price (COP) | MercadoLibre Colombia |
|---|---|---|---|---|---|
| 8 | Breadboard kit | MB-102 830 points + MB102 power supply + 65 jumpers | 1 | 13,000 - 25,000 | https://listado.mercadolibre.com.co/protoboard-830-jumpers |
| 9 | Resistor kit | 4.7 k for the DS18B20 pull-up, 220 ohm for the LED, 1 k for the buzzer | 1 | 5,000 - 12,000 | https://listado.mercadolibre.com.co/resistencias-surtidas-kit |
| 10 | Jumper wires | Dupont female-female and male-female, 20 cm | 1 set | 8,000 - 15,000 | https://listado.mercadolibre.com.co/cables-dupont-jumper |
| 11 | Multimeter (recommended) | Digital, continuity and DC voltage | 1 | 25,000 - 45,000 | https://listado.mercadolibre.com.co/multimetro-digital |
| 12 | Tray and mount | Acrylic or wood plate for the fridge-mode tray, M3/M4 screws, double-sided tape | 1 | 10,000 - 20,000 | https://listado.mercadolibre.com.co/base-acrilico-proyectos |

Reference total: 120,000 - 230,000 COP (around 30 - 55 USD), the multimeter being the most optional item. Verified local alternatives if MercadoLibre shipping fails: Electronilab (Bogota, same-day dispatch) and Mactronica (Medellin) stock the same parts, with load cell + HX711 around 17,500 - 22,500 COP and the MC-38 around 6,900 COP.

## Wiring plan (proposed, finalized in F3 and F4)

| Signal | ESP32 pin | Notes |
|---|---|---|
| Reed switch | GPIO 4 | `INPUT_PULLUP`; the magnet closes the contact when the door is shut |
| DS18B20 data | GPIO 5 | 1-Wire; 4.7 k pull-up to 3.3 V; probe inside the cold zone |
| HX711 DOUT | GPIO 32 | Input-only pin used as data |
| HX711 SCK | GPIO 33 | Clock output |
| Buzzer | GPIO 14 | Through a 1 k resistor |
| RGB LED | GPIO 25 (R), 26 (G), 27 (B) | PWM channels |

Power and safety rules:

- The ESP32 board stays outside the cold zone; only the DS18B20 probe and protected cables enter the fridge, to avoid condensation on the electronics.
- Power the HX711 at 3.3 V from the ESP32 so the data line is logic-safe without a level shifter, then calibrate the scale against known weights.
- Use the external 5 V supply, never a loose battery inside the fridge enclosure; add strain relief so door movement never pulls on the load cell wiring.
- Common ground between ESP32, HX711, and sensors; keep the load cell cable away from the door hinge to reduce mechanical noise.
- The load cell handles the tray, not the whole shelf; mount the cell between a fixed base and the tray with the arrow pointing in the direction of the applied weight.

## Buying notes

- Prefer sellers with MercadoLider reputation, at least a few hundred sales, and buyer protection; check that the listing is fulfilled and returns are accepted.
- The DS18B20 must be the waterproof probe version (stainless tip), not the bare TO-92 part.
- The load cell is the 5 kg bar type (80 x 12.7 x 12.7 mm), not a 1 kg or 20 kg variant; tare and calibration are done in firmware and verified with a kitchen scale.
- Buy two reed switches: they are cheap and the first mount often needs a spare after drilling.
- The demo can run entirely with the simulator before hardware arrives; hardware is required only for the firmware steps (F3 and F4).
