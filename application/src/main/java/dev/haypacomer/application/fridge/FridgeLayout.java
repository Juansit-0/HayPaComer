package dev.haypacomer.application.fridge;

import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;

public enum FridgeLayout {
  EMPTY,
  STANDARD;

  Fridge build(String name) {
    Fridge fridge = Fridge.named(name);
    if (this == STANDARD) {
      Zone shelves = Zone.named("Shelves", ZoneKind.SHELF);
      shelves.add(Tray.named("Top", 0));
      shelves.add(Tray.named("Middle", 1));
      shelves.add(Tray.named("Bottom", 2));
      Zone door = Zone.named("Door", ZoneKind.DOOR);
      door.add(Tray.named("Rack", 0));
      Zone drawer = Zone.named("Drawer", ZoneKind.DRAWER);
      drawer.add(Tray.named("Vegetables", 0));
      fridge.add(shelves);
      fridge.add(door);
      fridge.add(drawer);
    }
    return fridge;
  }
}
