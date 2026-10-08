package dev.haypacomer.application.ai;

import java.util.List;
import java.util.Objects;

public record Suggestions(List<Suggestion> items, AdvisorSource source) {

  public Suggestions {
    Objects.requireNonNull(source, "source");
    items = List.copyOf(items);
  }
}
