package dev.haypacomer.ai.llm;

import dev.haypacomer.application.ai.AdvisorSource;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

public final class GeminiClient implements LlmClient {

  public static final URI DEFAULT_BASE_URL =
      URI.create("https://generativelanguage.googleapis.com/v1beta/");

  private final LlmSettings settings;
  private final HttpJson http;

  public GeminiClient(LlmSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.http = new HttpJson(settings.timeout());
  }

  @Override
  public AdvisorSource source() {
    return AdvisorSource.GEMINI;
  }

  @Override
  public String completeJson(LlmPrompt prompt) {
    JsonNode answer =
        http.post(
            settings.baseUrl().resolve("models/" + settings.model() + ":generateContent"),
            Map.of("x-goog-api-key", settings.apiKey()),
            Map.of(
                "systemInstruction",
                Map.of("parts", List.of(Map.of("text", prompt.system()))),
                "contents",
                List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt.user())))),
                "generationConfig",
                Map.of("responseMimeType", "application/json", "temperature", 0.2)));
    JsonNode text =
        answer.path("candidates").path(0).path("content").path("parts").path(0).path("text");
    if (!text.isString()) {
      throw new InvalidAiResponseException("Gemini answered without text");
    }
    return text.asString();
  }
}
