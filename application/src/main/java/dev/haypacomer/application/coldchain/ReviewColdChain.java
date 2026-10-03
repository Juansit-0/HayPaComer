package dev.haypacomer.application.coldchain;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdIncident;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class ReviewColdChain {

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final ColdChainRepository chains;
  private final Clock clock;

  public ReviewColdChain(
      HouseholdRepository households,
      FridgeRepository fridges,
      ColdChainRepository chains,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.chains = Objects.requireNonNull(chains, "chains");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ColdIncident review(UserId actor, HouseholdId household, FridgeId fridge) {
    households.get(actor, household).requirePermission(actor, Permission.EDIT_INVENTORY);
    boolean ownFridge =
        fridges.findByHousehold(household).stream().map(Fridge::id).anyMatch(fridge::equals);
    if (!ownFridge) {
      throw new FridgeNotFoundException();
    }
    ColdChain chain = chains.find(fridge).orElseGet(() -> ColdChain.start(fridge));
    ColdIncident incident = chain.review(actor, clock.instant());
    chains.save(chain);
    return incident;
  }
}
