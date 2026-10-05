package dev.haypacomer.sensors.esp32;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Esp32Envelope(
    UUID eventId,
    String device,
    String type,
    String mode,
    String door,
    BigDecimal tempC,
    BigDecimal grams,
    Boolean stable,
    String ingredient,
    Long raw,
    Instant at) {}
