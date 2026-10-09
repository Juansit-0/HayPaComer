package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.util.function.Supplier;

public interface UnitOfWork {

  <T> T run(Supplier<T> work);

  default <T> T runFor(HouseholdId household, Supplier<T> work) {
    return run(work);
  }
}
