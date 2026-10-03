package dev.haypacomer.sensors.hardware;

import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class SimulatedEventDecoder implements SensorEventDecoder {

  private final JsonMapper json = JsonMapper.builder().build();
  private final SensorEventDecoder strict;
  private final Clock clock;

  public SimulatedEventDecoder(SensorEventDecoder strict, Clock clock) {
    this.strict = strict;
    this.clock = clock;
  }

  @Override
  public List<SensorEvent> decode(Device device, String payload) {
    JsonNode root;
    try {
      root = json.readTree(payload == null ? "" : payload);
    } catch (JacksonException exception) {
      throw new MalformedSensorPayloadException("Payload is not valid JSON");
    }
    if (root.isObject()) {
      complete((ObjectNode) root);
    } else if (root.isArray()) {
      root.forEach(
          node -> {
            if (node.isObject()) {
              complete((ObjectNode) node);
            }
          });
    }
    return strict.decode(device, json.writeValueAsString(root));
  }

  private void complete(ObjectNode node) {
    if (!node.hasNonNull("eventId")) {
      node.put("eventId", UUID.randomUUID().toString());
    }
    if (!node.hasNonNull("at")) {
      node.put("at", clock.instant().toString());
    }
  }
}
