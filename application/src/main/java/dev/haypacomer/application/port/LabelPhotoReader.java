package dev.haypacomer.application.port;

import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.RecipePhoto;

public interface LabelPhotoReader {

  String provider();

  LabelReading read(RecipePhoto photo);
}
