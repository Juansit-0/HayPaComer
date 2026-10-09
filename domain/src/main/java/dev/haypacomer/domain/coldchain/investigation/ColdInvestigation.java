package dev.haypacomer.domain.coldchain.investigation;

import dev.haypacomer.domain.fridge.FridgeId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ColdInvestigation(
    FridgeId fridge,
    Instant from,
    Instant to,
    int readings,
    List<ColdEpisode> episodes,
    Duration totalAboveLimit,
    List<FoodAssessment> foods) {

  public ColdInvestigation {
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(totalAboveLimit, "totalAboveLimit");
    episodes = List.copyOf(episodes);
    foods = List.copyOf(foods);
  }
}
