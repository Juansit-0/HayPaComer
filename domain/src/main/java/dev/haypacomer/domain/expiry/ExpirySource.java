package dev.haypacomer.domain.expiry;

public enum ExpirySource {
  USER(1.0),
  LABEL(0.9),
  AI_SUGGESTED(0.7),
  ESTIMATED(0.6);

  private final double confidence;

  ExpirySource(double confidence) {
    this.confidence = confidence;
  }

  public double confidence() {
    return confidence;
  }
}
