package dev.haypacomer.application.session;

import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.session.CookingSession;
import java.util.Objects;

public record StepWeighingResult(CookingSession session, WeighingProgress progress) {

  public StepWeighingResult {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(progress, "progress");
  }
}
