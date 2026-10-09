package dev.haypacomer.ai.llm;

import java.util.Base64;
import java.util.Objects;

public record LlmImage(String mimeType, String base64) {

  public LlmImage {
    Objects.requireNonNull(mimeType, "mimeType");
    Objects.requireNonNull(base64, "base64");
  }

  public static LlmImage of(String mimeType, byte[] bytes) {
    return new LlmImage(mimeType, Base64.getEncoder().encodeToString(bytes));
  }

  @Override
  public String toString() {
    return "LlmImage[" + mimeType + "]";
  }
}
