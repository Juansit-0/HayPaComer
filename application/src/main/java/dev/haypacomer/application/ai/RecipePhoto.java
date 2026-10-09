package dev.haypacomer.application.ai;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public record RecipePhoto(String mimeType, byte[] bytes) {

  public static final int MAX_BYTES = 4 * 1024 * 1024;
  public static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");

  public RecipePhoto {
    Objects.requireNonNull(mimeType, "mimeType");
    Objects.requireNonNull(bytes, "bytes");
    if (!TYPES.contains(mimeType)) {
      throw new IllegalArgumentException("Send a JPEG, PNG, or WebP photo");
    }
    if (bytes.length == 0 || bytes.length > MAX_BYTES) {
      throw new IllegalArgumentException("The photo must be between 1 byte and 4 MB");
    }
    if (!matches(mimeType, bytes)) {
      throw new IllegalArgumentException("The file content is not a " + mimeType + " image");
    }
    bytes = bytes.clone();
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  private static boolean matches(String mimeType, byte[] bytes) {
    return switch (mimeType) {
      case "image/jpeg" -> starts(bytes, 0xFF, 0xD8, 0xFF);
      case "image/png" -> starts(bytes, 0x89, 'P', 'N', 'G');
      default ->
          starts(bytes, 'R', 'I', 'F', 'F')
              && bytes.length >= 12
              && new String(
                      Arrays.copyOfRange(bytes, 8, 12), java.nio.charset.StandardCharsets.US_ASCII)
                  .equals("WEBP");
    };
  }

  private static boolean starts(byte[] bytes, int... prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if ((bytes[i] & 0xFF) != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RecipePhoto photo
        && photo.mimeType.equals(mimeType)
        && Arrays.equals(photo.bytes, bytes);
  }

  @Override
  public int hashCode() {
    return 31 * mimeType.hashCode() + Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return "RecipePhoto[" + mimeType + ", " + bytes.length + " bytes]";
  }
}
