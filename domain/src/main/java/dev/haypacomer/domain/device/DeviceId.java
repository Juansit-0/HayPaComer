package dev.haypacomer.domain.device;

import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) {

  public DeviceId {
    Objects.requireNonNull(value, "value");
  }

  public static DeviceId newId() {
    return new DeviceId(UUID.randomUUID());
  }
}
