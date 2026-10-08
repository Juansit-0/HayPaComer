package dev.haypacomer.web.security;

import java.net.URI;
import java.util.Locale;

public final class ProblemTypes {

  public static final String BASE = "https://haypacomer.dev/problems/";

  private ProblemTypes() {}

  public static URI of(String title) {
    return URI.create(
        BASE
            + title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
  }
}
