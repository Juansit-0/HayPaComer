package dev.haypacomer.domain.market;

import java.util.Objects;
import java.util.UUID;

public record MarketItemId(UUID value) {

  public MarketItemId {
    Objects.requireNonNull(value, "value");
  }

  public static MarketItemId newId() {
    return new MarketItemId(UUID.randomUUID());
  }
}
