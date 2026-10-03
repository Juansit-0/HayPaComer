package dev.haypacomer.application.port;

import java.util.function.Supplier;

public interface UnitOfWork {

  <T> T run(Supplier<T> work);
}
