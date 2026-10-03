package dev.haypacomer.application.inventory;

public final class PermissionRequiredException extends RuntimeException {

  public PermissionRequiredException() {
    super("This food is ask-first; request permission from its owner");
  }
}
