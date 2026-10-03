#include <Arduino.h>
#include <ArduinoJson.h>
#include <DallasTemperature.h>
#include <HTTPClient.h>
#include <OneWire.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <esp_random.h>
#include <time.h>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "secrets.example.h"
#endif

constexpr uint8_t REED_PIN = 4;
constexpr uint8_t ONE_WIRE_PIN = 5;
constexpr uint8_t BUZZER_PIN = 14;
constexpr uint8_t LED_RED_PIN = 25;
constexpr uint8_t LED_GREEN_PIN = 26;
constexpr uint8_t LED_BLUE_PIN = 27;

constexpr uint32_t DEBOUNCE_MS = 50;
constexpr uint32_t TEMPERATURE_EVERY_MS = 30000;
constexpr uint32_t FLUSH_EVERY_MS = 2000;
constexpr uint32_t COMMANDS_EVERY_MS = 3000;
constexpr uint32_t MAX_BACKOFF_MS = 60000;
constexpr size_t QUEUE_CAPACITY = 64;
constexpr size_t BATCH_SIZE = 20;

OneWire oneWire(ONE_WIRE_PIN);
DallasTemperature probe(&oneWire);

String queue[QUEUE_CAPACITY];
size_t queueHead = 0;
size_t queueCount = 0;

bool doorOpen = false;
bool lastRawOpen = false;
uint32_t lastRawChangeMs = 0;
uint32_t lastTemperatureMs = 0;
uint32_t lastFlushMs = 0;
uint32_t lastCommandsMs = 0;
uint32_t backoffMs = FLUSH_EVERY_MS;

bool readDoorOpen() {
  return digitalRead(REED_PIN) == HIGH;
}

bool clockReady() {
  return time(nullptr) > 1704067200;
}

String isoNow() {
  time_t now = time(nullptr);
  struct tm utc;
  gmtime_r(&now, &utc);
  char buffer[25];
  strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%SZ", &utc);
  return String(buffer);
}

String uuidV4() {
  uint8_t bytes[16];
  for (int index = 0; index < 16; index += 4) {
    uint32_t random = esp_random();
    memcpy(bytes + index, &random, 4);
  }
  bytes[6] = (bytes[6] & 0x0F) | 0x40;
  bytes[8] = (bytes[8] & 0x3F) | 0x80;
  char text[37];
  snprintf(
      text,
      sizeof(text),
      "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
      bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
      bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);
  return String(text);
}

void enqueue(const String& envelope) {
  if (queueCount == QUEUE_CAPACITY) {
    queueHead = (queueHead + 1) % QUEUE_CAPACITY;
    queueCount--;
  }
  queue[(queueHead + queueCount) % QUEUE_CAPACITY] = envelope;
  queueCount++;
}

void enqueueDoor(bool open) {
  JsonDocument event;
  event["eventId"] = uuidV4();
  event["device"] = WiFi.macAddress();
  event["type"] = "DOOR";
  event["door"] = open ? "OPEN" : "CLOSED";
  event["at"] = isoNow();
  String json;
  serializeJson(event, json);
  enqueue(json);
}

void enqueueTemperature(float celsius) {
  JsonDocument event;
  event["eventId"] = uuidV4();
  event["device"] = WiFi.macAddress();
  event["type"] = "TEMPERATURE";
  event["tempC"] = serialized(String(celsius, 2));
  event["at"] = isoNow();
  String json;
  serializeJson(event, json);
  enqueue(json);
}

bool beginRequest(HTTPClient& http, WiFiClient& plain, WiFiClientSecure& secure, const String& path) {
  String url = String(API_BASE_URL) + path;
  if (url.startsWith("https://")) {
    if (strlen(ROOT_CA) == 0) {
      Serial.println("HTTPS needs ROOT_CA in secrets.h");
      return false;
    }
    secure.setCACert(ROOT_CA);
    return http.begin(secure, url);
  }
  return http.begin(plain, url);
}

void flushQueue() {
  if (queueCount == 0 || WiFi.status() != WL_CONNECTED) {
    return;
  }
  size_t batch = min(queueCount, BATCH_SIZE);
  String body = "[";
  for (size_t index = 0; index < batch; index++) {
    if (index > 0) {
      body += ",";
    }
    body += queue[(queueHead + index) % QUEUE_CAPACITY];
  }
  body += "]";
  WiFiClient plain;
  WiFiClientSecure secure;
  HTTPClient http;
  if (!beginRequest(http, plain, secure, "/api/v1/device/events")) {
    return;
  }
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_KEY);
  int status = http.POST(body);
  http.end();
  if (status == 200 || status == 202 || status == 400) {
    queueHead = (queueHead + batch) % QUEUE_CAPACITY;
    queueCount -= batch;
    backoffMs = FLUSH_EVERY_MS;
    if (status == 400) {
      Serial.println("Batch rejected by the server and dropped");
    }
  } else {
    backoffMs = min(backoffMs * 2, MAX_BACKOFF_MS);
    Serial.printf("Event upload failed with %d, retrying in %lu ms\n", status, (unsigned long) backoffMs);
  }
}

void setLed(bool red, bool green, bool blue) {
  digitalWrite(LED_RED_PIN, red ? HIGH : LOW);
  digitalWrite(LED_GREEN_PIN, green ? HIGH : LOW);
  digitalWrite(LED_BLUE_PIN, blue ? HIGH : LOW);
}

void beep(uint8_t times, uint16_t onMs, uint16_t offMs) {
  for (uint8_t index = 0; index < times; index++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(onMs);
    digitalWrite(BUZZER_PIN, LOW);
    delay(offMs);
  }
}

void play(const char* pattern) {
  if (strcmp(pattern, "DOOR_OPEN_BEEP") == 0) {
    setLed(true, false, false);
    beep(3, 120, 120);
  } else if (strcmp(pattern, "COLD_CHAIN_ALARM") == 0) {
    setLed(true, false, true);
    beep(2, 600, 300);
  } else if (strcmp(pattern, "WEIGHT_CONFIRMED_BLINK") == 0) {
    setLed(false, true, false);
    delay(400);
  }
  setLed(false, false, false);
}

void pollCommands() {
  if (WiFi.status() != WL_CONNECTED) {
    return;
  }
  WiFiClient plain;
  WiFiClientSecure secure;
  HTTPClient http;
  if (!beginRequest(http, plain, secure, "/api/v1/device/commands")) {
    return;
  }
  http.addHeader("X-Device-Key", DEVICE_KEY);
  int status = http.GET();
  if (status == 200) {
    JsonDocument commands;
    if (deserializeJson(commands, http.getString()) == DeserializationError::Ok) {
      for (JsonVariant command : commands.as<JsonArray>()) {
        play(command.as<const char*>());
      }
    }
  }
  http.end();
}

void connectWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  uint32_t started = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - started < 20000) {
    delay(250);
  }
  Serial.println(WiFi.status() == WL_CONNECTED ? "WiFi connected" : "WiFi not yet connected");
}

void setup() {
  Serial.begin(115200);
  pinMode(REED_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
  pinMode(LED_BLUE_PIN, OUTPUT);
  setLed(false, false, true);
  probe.begin();
  connectWifi();
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  doorOpen = readDoorOpen();
  lastRawOpen = doorOpen;
  setLed(false, false, false);
}

void loop() {
  uint32_t now = millis();
  if (WiFi.status() != WL_CONNECTED) {
    WiFi.reconnect();
  }
  bool rawOpen = readDoorOpen();
  if (rawOpen != lastRawOpen) {
    lastRawOpen = rawOpen;
    lastRawChangeMs = now;
  }
  if (rawOpen != doorOpen && now - lastRawChangeMs >= DEBOUNCE_MS && clockReady()) {
    doorOpen = rawOpen;
    enqueueDoor(doorOpen);
  }
  if (now - lastTemperatureMs >= TEMPERATURE_EVERY_MS && clockReady()) {
    lastTemperatureMs = now;
    probe.requestTemperatures();
    float celsius = probe.getTempCByIndex(0);
    if (celsius != DEVICE_DISCONNECTED_C) {
      enqueueTemperature(celsius);
    }
  }
  if (now - lastFlushMs >= backoffMs) {
    lastFlushMs = now;
    flushQueue();
  }
  if (now - lastCommandsMs >= COMMANDS_EVERY_MS) {
    lastCommandsMs = now;
    pollCommands();
  }
  delay(10);
}
