package dev.haypacomer.ai.llm;

import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.LabelPhotoReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import tools.jackson.databind.JsonNode;

public final class LlmLabelPhotoReader implements LabelPhotoReader {

  static final String SYSTEM =
      """
      You read the label of a food package in a photo.
      Answer only JSON with this shape:
      {"product": text up to 80, "date": "YYYY-MM-DD" or null, "confidence": number 0-1}
      "date" is the printed expiry or best before date, copied as printed and converted to
      ISO. Labels in Colombia usually print day, month, year (for example 09/10/2026 is
      2026-10-09). Never invent a date: if no date is readable, answer null. "confidence"
      says how sure you are of the date.
      """;

  private final LlmClient client;

  public LlmLabelPhotoReader(LlmClient client) {
    this.client = client;
  }

  @Override
  public String provider() {
    return client.source().name().toLowerCase(Locale.ROOT);
  }

  @Override
  public LabelReading read(RecipePhoto photo) {
    String answer;
    try {
      answer =
          client.completeJson(
              new LlmPrompt(
                  SYSTEM, "Read this food label.", LlmImage.of(photo.mimeType(), photo.bytes())));
    } catch (AiUnavailableException unavailable) {
      throw PhotoReadingUnavailableException.providerDown(
          "The AI provider is not answering; try later");
    }
    try {
      return parse(answer);
    } catch (InvalidAiResponseException invalid) {
      throw new PhotoReadingUnavailableException(
          "The photo could not be read as a label: " + invalid.getMessage());
    }
  }

  private LabelReading parse(String answer) {
    JsonNode root = JsonContract.object(answer);
    String product = JsonContract.optionalText(root, "product", 80).orElse("");
    JsonNode date = root.path("date");
    LocalDate printed = null;
    if (!date.isMissingNode() && !date.isNull()) {
      if (!date.isString()) {
        throw new InvalidAiResponseException("The date must be text or null");
      }
      try {
        printed = LocalDate.parse(date.asString().strip());
      } catch (DateTimeParseException notIso) {
        throw new InvalidAiResponseException("The date is not an ISO date");
      }
    }
    return new LabelReading(
        product, printed, JsonContract.number(root, "confidence", 0, 1), client.source());
  }
}
