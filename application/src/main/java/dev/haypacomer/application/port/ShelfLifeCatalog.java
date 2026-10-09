package dev.haypacomer.application.port;

import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodMetadata;

public interface ShelfLifeCatalog {

  ShelfLife shelfLife(FoodMetadata food);
}
