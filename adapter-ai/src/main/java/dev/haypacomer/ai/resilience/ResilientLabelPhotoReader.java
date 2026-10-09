package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.LabelPhotoReader;
import java.util.Objects;

public final class ResilientLabelPhotoReader implements LabelPhotoReader {

  private final LabelPhotoReader provider;
  private final ProviderCircuit circuit;

  public ResilientLabelPhotoReader(LabelPhotoReader provider, ProviderCircuit circuit) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.circuit = Objects.requireNonNull(circuit, "circuit");
  }

  @Override
  public String provider() {
    return provider.provider();
  }

  @Override
  public LabelReading read(RecipePhoto photo) {
    return circuit.call(
        () -> provider.read(photo),
        () -> {
          throw new PhotoReadingUnavailableException(ResilientRecipePhotoReader.RESTING);
        },
        failure ->
            failure instanceof PhotoReadingUnavailableException unavailable
                && unavailable.isProviderDown());
  }
}
