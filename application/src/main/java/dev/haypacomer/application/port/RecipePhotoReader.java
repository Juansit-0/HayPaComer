package dev.haypacomer.application.port;

import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;

public interface RecipePhotoReader {

  String provider();

  PhotoRecipe read(RecipePhoto photo);
}
