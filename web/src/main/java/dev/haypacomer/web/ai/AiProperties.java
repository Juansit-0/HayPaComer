package dev.haypacomer.web.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("haypacomer.ai")
public record AiProperties(
    Provider provider,
    Duration timeout,
    Duration cacheTimeToLive,
    Gemini gemini,
    OpenAiCompatible openaiCompatible) {

  public enum Provider {
    OFFLINE,
    GEMINI,
    OPENAI_COMPATIBLE
  }

  public record Gemini(String apiKey, String model) {}

  public record OpenAiCompatible(String baseUrl, String apiKey, String model) {}
}
