package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.RecipePhotoReader;

public final class OfflineRecipePhotoReader implements RecipePhotoReader {

  @Override
  public String provider() {
    return "offline";
  }

  @Override
  public PhotoRecipe read(RecipePhoto photo) {
    throw new PhotoReadingUnavailableException(
        "Reading recipe photos needs an AI provider; set AI_PROVIDER to gemini or"
            + " openai-compatible");
  }
}
