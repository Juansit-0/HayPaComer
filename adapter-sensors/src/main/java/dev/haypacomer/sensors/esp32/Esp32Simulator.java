package dev.haypacomer.sensors.esp32;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class Esp32Simulator {

  private final JsonMapper json = JsonMapper.builder().build();
  private final String deviceName;

  public Esp32Simulator(String deviceName) {
    this.deviceName = deviceName;
  }

  public Esp32Envelope door(boolean open, Instant at) {
    return envelope("DOOR", null, open ? "OPEN" : "CLOSED", null, null, null, null, at);
  }

  public Esp32Envelope temperature(String celsius, Instant at) {
    return envelope("TEMPERATURE", null, null, new BigDecimal(celsius), null, null, null, at);
  }

  public Esp32Envelope weight(String grams, boolean stable, String mode, Instant at) {
    return envelope("WEIGHT", mode, null, null, new BigDecimal(grams), stable, null, at);
  }

  public List<Esp32Envelope> doorLeftOpen(Instant openedAt, Duration openFor) {
    return List.of(door(true, openedAt), door(false, openedAt.plus(openFor)));
  }

  public List<Esp32Envelope> coldChainBreak(Instant start, Duration every, String... celsius) {
    List<Esp32Envelope> readings = new ArrayList<>();
    for (int index = 0; index < celsius.length; index++) {
      readings.add(temperature(celsius[index], start.plus(every.multipliedBy(index))));
    }
    return List.copyOf(readings);
  }

  public List<Esp32Envelope> productRemoval(String gramsBefore, String gramsAfter, Instant at) {
    return List.of(
        weight(gramsBefore, true, "FRIDGE", at),
        door(true, at.plusSeconds(2)),
        weight(gramsAfter, false, "FRIDGE", at.plusSeconds(5)),
        weight(gramsAfter, true, "FRIDGE", at.plusSeconds(6)),
        door(false, at.plusSeconds(8)));
  }

  public String toJson(Esp32Envelope envelope) {
    return json.writeValueAsString(envelope);
  }

  public String toJson(List<Esp32Envelope> envelopes) {
    return json.writeValueAsString(envelopes);
  }

  private Esp32Envelope envelope(
      String type,
      String mode,
      String door,
      BigDecimal tempC,
      BigDecimal grams,
      Boolean stable,
      String ingredient,
      Instant at) {
    return new Esp32Envelope(
        UUID.randomUUID(), deviceName, type, mode, door, tempC, grams, stable, ingredient, at);
  }
}
