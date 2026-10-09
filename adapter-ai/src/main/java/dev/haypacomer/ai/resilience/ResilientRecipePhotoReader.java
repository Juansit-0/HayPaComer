package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.RecipePhotoReader;
import java.util.Objects;

public final class ResilientRecipePhotoReader implements RecipePhotoReader {

  static final String RESTING =
      "The AI provider is resting after repeated failures; try the photo again in a minute";

  private final RecipePhotoReader provider;
  private final ProviderCircuit circuit;

  public ResilientRecipePhotoReader(RecipePhotoReader provider, ProviderCircuit circuit) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.circuit = Objects.requireNonNull(circuit, "circuit");
  }

  @Override
  public String provider() {
    return provider.provider();
  }

  @Override
  public PhotoRecipe read(RecipePhoto photo) {
    return circuit.call(
        () -> provider.read(photo),
        () -> {
          throw new PhotoReadingUnavailableException(RESTING);
        },
        failure ->
            failure instanceof PhotoReadingUnavailableException unavailable
                && unavailable.isProviderDown());
  }
}
