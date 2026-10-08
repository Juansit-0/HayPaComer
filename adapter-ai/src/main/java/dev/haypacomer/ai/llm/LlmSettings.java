package dev.haypacomer.ai.llm;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public record LlmSettings(URI baseUrl, String apiKey, String model, Duration timeout) {

  public LlmSettings {
    Objects.requireNonNull(baseUrl, "baseUrl");
    Objects.requireNonNull(apiKey, "apiKey");
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(timeout, "timeout");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("The AI provider needs an API key");
    }
    if (model.isBlank()) {
      throw new IllegalArgumentException("The AI provider needs a model");
    }
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Timeout must be positive");
    }
  }

  @Override
  public String toString() {
    return "LlmSettings[baseUrl=" + baseUrl + ", model=" + model + ", timeout=" + timeout + "]";
  }
}
