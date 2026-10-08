package dev.haypacomer.application.ai;

import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record SuggestionQuery(
    List<Recipe> candidates,
    int servings,
    Integer maxMinutes,
    Set<MemberId> diners,
    int limit,
    boolean rescueOnly) {

  public static final int MAX_CANDIDATES = 50;

  public SuggestionQuery {
    candidates = List.copyOf(candidates);
    diners = Set.copyOf(diners);
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("Give at least one recipe to choose from");
    }
    if (candidates.size() > MAX_CANDIDATES) {
      throw new IllegalArgumentException("At most " + MAX_CANDIDATES + " recipes per request");
    }
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
    if (maxMinutes != null && maxMinutes < 1) {
      throw new IllegalArgumentException("Minutes must be at least 1: " + maxMinutes);
    }
    if (limit < 1 || limit > SuggestionRequest.MAX_SUGGESTIONS) {
      throw new IllegalArgumentException(
          "Limit must be between 1 and " + SuggestionRequest.MAX_SUGGESTIONS);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<Integer> minutesLimit() {
    return Optional.ofNullable(maxMinutes);
  }

  public static final class Builder {

    private final List<Recipe> candidates = new ArrayList<>();
    private final Set<MemberId> diners = new HashSet<>();
    private int servings = 2;
    private Integer maxMinutes;
    private int limit = SuggestionRequest.MAX_SUGGESTIONS;
    private boolean rescueOnly;

    private Builder() {}

    public Builder candidate(Recipe recipe) {
      candidates.add(Objects.requireNonNull(recipe, "recipe"));
      return this;
    }

    public Builder candidates(List<Recipe> recipes) {
      recipes.forEach(this::candidate);
      return this;
    }

    public Builder servings(int value) {
      servings = value;
      return this;
    }

    public Builder maxMinutes(Integer value) {
      maxMinutes = value;
      return this;
    }

    public Builder diner(MemberId member) {
      diners.add(Objects.requireNonNull(member, "member"));
      return this;
    }

    public Builder limit(int value) {
      limit = value;
      return this;
    }

    public Builder rescueOnly(boolean value) {
      rescueOnly = value;
      return this;
    }

    public SuggestionQuery build() {
      return new SuggestionQuery(candidates, servings, maxMinutes, diners, limit, rescueOnly);
    }
  }
}
