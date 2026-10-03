package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class Esp32EventAdapter implements SensorEventDecoder {

  private static final int MAX_BATCH = 500;

  private final JsonMapper json =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private final Map<String, Esp32EventFactory> factories =
      Map.of(
          "DOOR", new DoorEventFactory(),
          "TEMPERATURE", new TemperatureEventFactory(),
          "WEIGHT", new WeightEventFactory());

  @Override
  public List<SensorEvent> decode(Device device, String payload) {
    JsonNode root = parse(payload);
    List<JsonNode> nodes = new ArrayList<>();
    if (root.isArray()) {
      root.forEach(nodes::add);
    } else if (root.isObject()) {
      nodes.add(root);
    } else {
      throw new MalformedSensorPayloadException("Payload must be an event or an array of events");
    }
    if (nodes.isEmpty() || nodes.size() > MAX_BATCH) {
      throw new MalformedSensorPayloadException("A batch needs 1 to " + MAX_BATCH + " events");
    }
    return nodes.stream().map(node -> toEvent(device, node)).toList();
  }

  private JsonNode parse(String payload) {
    if (payload == null || payload.isBlank()) {
      throw new MalformedSensorPayloadException("Payload is empty");
    }
    try {
      return json.readTree(payload);
    } catch (JacksonException exception) {
      throw new MalformedSensorPayloadException("Payload is not valid JSON");
    }
  }

  private SensorEvent toEvent(Device device, JsonNode node) {
    Esp32Envelope envelope;
    try {
      envelope = json.treeToValue(node, Esp32Envelope.class);
    } catch (JacksonException exception) {
      throw new MalformedSensorPayloadException("Event has invalid field values");
    }
    String type = envelope.type() == null ? "" : envelope.type().strip().toUpperCase(Locale.ROOT);
    Esp32EventFactory factory = factories.get(type);
    if (factory == null) {
      throw new MalformedSensorPayloadException("Unknown event type: " + envelope.type());
    }
    return factory.create(envelope, device);
  }
}
