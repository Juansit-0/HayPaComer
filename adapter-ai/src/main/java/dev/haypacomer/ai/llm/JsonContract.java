package dev.haypacomer.ai.llm;

import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

final class JsonContract {

  private JsonContract() {}

  static JsonNode object(String json) {
    JsonNode node;
    try {
      node = HttpJson.JSON.readTree(unfence(json));
    } catch (JacksonException malformed) {
      throw new InvalidAiResponseException("The answer is not valid JSON");
    }
    if (node == null || !node.isObject()) {
      throw new InvalidAiResponseException("The answer must be a JSON object");
    }
    return node;
  }

  static String unfence(String answer) {
    String trimmed = answer.strip();
    if (!trimmed.startsWith("```") || !trimmed.endsWith("```") || trimmed.length() < 6) {
      return trimmed;
    }
    String inner = trimmed.substring(3, trimmed.length() - 3);
    int newline = inner.indexOf('\n');
    if (newline >= 0 && inner.substring(0, newline).strip().matches("[A-Za-z]*")) {
      inner = inner.substring(newline + 1);
    }
    return inner.strip();
  }

  static String text(JsonNode node, String field, int maxLength) {
    return optionalText(node, field, maxLength)
        .orElseThrow(() -> new InvalidAiResponseException("Missing text field: " + field));
  }

  static Optional<String> optionalText(JsonNode node, String field, int maxLength) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return Optional.empty();
    }
    if (!value.isString()) {
      throw new InvalidAiResponseException("Field must be text: " + field);
    }
    String text = value.asString().strip();
    if (text.isEmpty()) {
      return Optional.empty();
    }
    if (text.length() > maxLength) {
      throw new InvalidAiResponseException("Field is too long: " + field);
    }
    return Optional.of(text);
  }

  static double number(JsonNode node, String field, double min, double max) {
    JsonNode value = node.path(field);
    if (!value.isNumber()) {
      throw new InvalidAiResponseException("Field must be a number: " + field);
    }
    double number = value.asDouble();
    if (number < min || number > max) {
      throw new InvalidAiResponseException("Field is out of range: " + field);
    }
    return number;
  }
}
