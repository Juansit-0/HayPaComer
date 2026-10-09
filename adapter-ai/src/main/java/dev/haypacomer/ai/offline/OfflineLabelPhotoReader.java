package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.LabelPhotoReader;

public final class OfflineLabelPhotoReader implements LabelPhotoReader {

  @Override
  public String provider() {
    return "offline";
  }

  @Override
  public LabelReading read(RecipePhoto photo) {
    throw PhotoReadingUnavailableException.providerDown("Reading labels needs an AI provider");
  }
}
