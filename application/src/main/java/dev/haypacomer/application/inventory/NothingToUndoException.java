package dev.haypacomer.application.inventory;

public final class NothingToUndoException extends RuntimeException {

  public NothingToUndoException() {
    super("There is no change to undo");
  }
}
