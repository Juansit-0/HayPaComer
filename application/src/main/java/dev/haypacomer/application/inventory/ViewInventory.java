package dev.haypacomer.application.inventory;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.OwnedFood;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.inventory.PrivateFoodProxy;
import dev.haypacomer.domain.inventory.StockedFood;
import dev.haypacomer.domain.inventory.UnderReviewFood;
import dev.haypacomer.domain.member.MemberId;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ViewInventory {

  private static final Comparator<InventoryEntry> RESCUE_ORDER =
      Comparator.comparing(InventoryEntry::usable)
          .reversed()
          .thenComparing(InventoryEntry::food, StockedFood.RESCUE_ORDER);

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final FoodOwnershipRepository ownerships;
  private final ColdChainRepository coldChains;
  private final PolicySource policies;

  public ViewInventory(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      ColdChainRepository coldChains,
      FreshnessPolicy freshness) {
    this(
        households,
        fridges,
        ownerships,
        coldChains,
        FixedPolicies.DEFAULT.withFreshness(freshness));
  }

  public ViewInventory(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      ColdChainRepository coldChains,
      PolicySource policies) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.ownerships = Objects.requireNonNull(ownerships, "ownerships");
    this.coldChains = Objects.requireNonNull(coldChains, "coldChains");
    this.policies = Objects.requireNonNull(policies, "policies");
  }

  public List<InventoryEntry> view(UserId actor, HouseholdId householdId, LocalDate today) {
    MemberId viewer = households.get(actor, householdId).membershipOf(actor).orElseThrow().member();
    FreshnessPolicy freshness = policies.freshness(householdId);
    return fridges.findByHousehold(householdId).stream()
        .flatMap(fridge -> entries(fridge, viewer, today, freshness).stream())
        .sorted(RESCUE_ORDER)
        .toList();
  }

  private List<InventoryEntry> entries(
      Fridge fridge, MemberId viewer, LocalDate today, FreshnessPolicy freshness) {
    boolean underReview = coldChains.find(fridge.id()).map(ColdChain::needsReview).orElse(false);
    return fridge
        .trays()
        .flatMap(
            tray ->
                tray.children().stream()
                    .map(item -> stocked(item, today, underReview, freshness))
                    .map(food -> PrivateFoodProxy.guard(food, viewer))
                    .map(
                        food ->
                            new InventoryEntry(
                                fridge.id(), tray.id(), food, food.isUsableBy(viewer))))
        .toList();
  }

  private StockedFood stocked(
      FoodItem item, LocalDate today, boolean underReview, FreshnessPolicy freshness) {
    StockedFood food =
        ownerships
            .find(item.id())
            .<StockedFood>map(ownership -> new OwnedFood(new PlainFood(item), ownership))
            .orElseGet(() -> new PlainFood(item));
    StockedFood fresh = freshness.apply(food, today);
    return underReview && item.food().perishable() ? new UnderReviewFood(fresh) : fresh;
  }
}
