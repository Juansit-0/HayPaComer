package dev.haypacomer.application.inventory;

public final class SnapshotNotFoundException extends RuntimeException {

  public SnapshotNotFoundException() {
    super("Snapshot not found");
  }
}
