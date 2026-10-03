package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;

public record StockCommand(
    FridgeId fridge,
    TrayId tray,
    String foodName,
    Grams grossWeight,
    Grams tare,
    LocalDate expiresOn,
    Visibility visibility) {}
