package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;

record InventoryWorkspace(
    UserId actor,
    Household household,
    HouseholdInventory inventory,
    FoodAccessGuard guard,
    FoodCatalogRepository catalog,
    ExpiryDesk expiry,
    Instant now) {}
