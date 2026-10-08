package dev.haypacomer.ai.llm;

import dev.haypacomer.application.ai.AdvisorSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

public final class OpenAiCompatibleClient implements LlmClient {

  private final LlmSettings settings;
  private final HttpJson http;

  public OpenAiCompatibleClient(LlmSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.http = new HttpJson(settings.timeout());
  }

  @Override
  public AdvisorSource source() {
    return AdvisorSource.OPENAI_COMPATIBLE;
  }

  @Override
  public String completeJson(LlmPrompt prompt) {
    JsonNode answer =
        http.post(
            settings.baseUrl().resolve("chat/completions"),
            Map.of("Authorization", "Bearer " + settings.apiKey()),
            Map.of(
                "model",
                settings.model(),
                "temperature",
                0.2,
                "response_format",
                Map.of("type", "json_object"),
                "messages",
                List.of(
                    Map.of("role", "system", "content", prompt.system()),
                    Map.of("role", "user", "content", prompt.user()))));
    JsonNode content = answer.path("choices").path(0).path("message").path("content");
    if (!content.isString()) {
      throw new InvalidAiResponseException("The provider answered without content");
    }
    return content.asString();
  }
}
