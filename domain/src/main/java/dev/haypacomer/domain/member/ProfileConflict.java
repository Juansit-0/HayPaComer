package dev.haypacomer.domain.member;

import dev.haypacomer.domain.food.FoodMetadata;
import java.util.Objects;

public record ProfileConflict(
    MemberId member, FoodMetadata food, ConflictReason reason, String detail) {

  public ProfileConflict {
    Objects.requireNonNull(member, "member");
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(detail, "detail");
  }
}
