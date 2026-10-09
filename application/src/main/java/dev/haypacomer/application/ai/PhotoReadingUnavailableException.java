package dev.haypacomer.application.ai;

public final class PhotoReadingUnavailableException extends RuntimeException {

  private final boolean providerDown;

  public PhotoReadingUnavailableException(String message) {
    this(message, false);
  }

  private PhotoReadingUnavailableException(String message, boolean providerDown) {
    super(message);
    this.providerDown = providerDown;
  }

  public static PhotoReadingUnavailableException providerDown(String message) {
    return new PhotoReadingUnavailableException(message, true);
  }

  public boolean isProviderDown() {
    return providerDown;
  }
}
