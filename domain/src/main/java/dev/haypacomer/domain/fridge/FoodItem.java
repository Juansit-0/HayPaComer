package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FoodItem implements FridgeNode {

  private final FoodItemId id;
  private final FoodMetadata food;
  private final Grams tare;
  private LocalDate expiresOn;
  private ExpirySource expirySource;
  private LocalDate openedOn;
  private Grams quantity;

  public FoodItem(
      FoodItemId id, FoodMetadata food, Grams quantity, Grams tare, LocalDate expiresOn) {
    this(id, food, quantity, tare, expiresOn, null, null);
  }

  public FoodItem(
      FoodItemId id,
      FoodMetadata food,
      Grams quantity,
      Grams tare,
      LocalDate expiresOn,
      ExpirySource expirySource,
      LocalDate openedOn) {
    this.id = Objects.requireNonNull(id, "id");
    this.food = Objects.requireNonNull(food, "food");
    this.quantity = Objects.requireNonNull(quantity, "quantity");
    this.tare = Objects.requireNonNull(tare, "tare");
    this.expiresOn = expiresOn;
    this.expirySource =
        expiresOn == null ? null : expirySource == null ? ExpirySource.USER : expirySource;
    this.openedOn = openedOn;
  }

  public static FoodItem weighed(
      FoodMetadata food, Grams grossWeight, Grams tare, LocalDate expiresOn) {
    return new FoodItem(FoodItemId.newId(), food, grossWeight.minus(tare), tare, expiresOn);
  }

  public static FoodItem weighed(
      FoodMetadata food, Grams grossWeight, Grams tare, ExpiryEstimate expiry, LocalDate openedOn) {
    return new FoodItem(
        FoodItemId.newId(),
        food,
        grossWeight.minus(tare),
        tare,
        expiry.date(),
        expiry.source(),
        openedOn);
  }

  public FoodItemId id() {
    return id;
  }

  public FoodMetadata food() {
    return food;
  }

  public Grams quantity() {
    return quantity;
  }

  public Grams tare() {
    return tare;
  }

  public Optional<LocalDate> expiresOn() {
    return Optional.ofNullable(expiresOn);
  }

  public Optional<ExpirySource> expirySource() {
    return Optional.ofNullable(expirySource);
  }

  public Optional<LocalDate> openedOn() {
    return Optional.ofNullable(openedOn);
  }

  public boolean isOpened() {
    return openedOn != null;
  }

  public void open(LocalDate today, LocalDate expiry, ExpirySource source) {
    Objects.requireNonNull(today, "today");
    Objects.requireNonNull(expiry, "expiry");
    Objects.requireNonNull(source, "source");
    if (openedOn == null) {
      openedOn = today;
    }
    expiresOn = expiry;
    expirySource = source;
  }

  public Grams grossWeight() {
    return quantity.plus(tare);
  }

  public boolean isEmpty() {
    return quantity.isZero();
  }

  public Grams consume(Grams grams) {
    quantity = quantity.minus(grams);
    return quantity;
  }

  public Grams restock(Grams grams) {
    quantity = quantity.plus(grams);
    return quantity;
  }

  @Override
  public String name() {
    return food.name();
  }

  @Override
  public Grams totalGrams() {
    return quantity;
  }

  @Override
  public int itemCount() {
    return 1;
  }

  @Override
  public List<FridgeNode> children() {
    return List.of();
  }
}
