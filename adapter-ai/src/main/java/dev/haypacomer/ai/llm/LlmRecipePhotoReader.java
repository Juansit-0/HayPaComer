package dev.haypacomer.ai.llm;

import dev.haypacomer.application.ai.PhotoIngredient;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.RecipePhotoReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tools.jackson.databind.JsonNode;

public final class LlmRecipePhotoReader implements RecipePhotoReader {

  static final int MAX_INGREDIENTS = 30;
  static final int MAX_STEPS = 30;

  static final String SYSTEM =
      """
      You read a photo of a recipe (a cookbook page, a handwritten card, or a screenshot).
      Answer only JSON with this shape:
      {"name": text up to 80, "servings": integer 1-50, "minutes": integer 1-1440,
       "ingredients": [{"food": text up to 60, "quantity": text up to 40}] (1 to 30),
       "steps": [text up to 300] (0 to 30), "confidence": number 0-1}
      Copy foods and quantities as written, using grams, kilograms, milliliters, liters,
      cups, tablespoons, teaspoons, or units. Never invent ingredients that are not in the
      photo. If the photo is not a recipe, answer {"name": "", "ingredients": []}.
      """;

  private final LlmClient client;

  public LlmRecipePhotoReader(LlmClient client) {
    this.client = client;
  }

  @Override
  public String provider() {
    return client.source().name().toLowerCase(Locale.ROOT);
  }

  @Override
  public PhotoRecipe read(RecipePhoto photo) {
    String answer;
    try {
      answer =
          client.completeJson(
              new LlmPrompt(
                  SYSTEM, "Read this recipe photo.", LlmImage.of(photo.mimeType(), photo.bytes())));
    } catch (AiUnavailableException unavailable) {
      throw new PhotoReadingUnavailableException("The AI provider is not answering; try later");
    }
    try {
      return parse(answer);
    } catch (InvalidAiResponseException invalid) {
      throw new PhotoReadingUnavailableException(
          "The photo could not be read as a recipe: " + invalid.getMessage());
    }
  }

  private PhotoRecipe parse(String answer) {
    JsonNode root = JsonContract.object(answer);
    String name =
        JsonContract.optionalText(root, "name", 80)
            .orElseThrow(() -> new InvalidAiResponseException("No recipe in the photo"));
    JsonNode ingredients = root.path("ingredients");
    if (!ingredients.isArray() || ingredients.isEmpty() || ingredients.size() > MAX_INGREDIENTS) {
      throw new InvalidAiResponseException("A recipe needs 1 to 30 ingredients");
    }
    List<PhotoIngredient> read = new ArrayList<>();
    for (JsonNode ingredient : ingredients) {
      read.add(
          new PhotoIngredient(
              JsonContract.text(ingredient, "food", 60),
              JsonContract.text(ingredient, "quantity", 40)));
    }
    JsonNode steps = root.path("steps");
    List<String> instructions = new ArrayList<>();
    if (!steps.isMissingNode() && !steps.isNull()) {
      if (!steps.isArray() || steps.size() > MAX_STEPS) {
        throw new InvalidAiResponseException("Steps must be a list of at most 30");
      }
      for (JsonNode step : steps) {
        if (!step.isString() || step.asString().isBlank() || step.asString().length() > 300) {
          throw new InvalidAiResponseException("Each step is text up to 300 characters");
        }
        instructions.add(step.asString().strip());
      }
    }
    return new PhotoRecipe(
        name,
        (int) JsonContract.number(root, "servings", 1, 50),
        (int) JsonContract.number(root, "minutes", 1, 1440),
        read,
        instructions,
        JsonContract.number(root, "confidence", 0, 1),
        client.source());
  }
}
