package dev.haypacomer.application.coldchain;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListColdChains {

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final ColdChainRepository chains;

  public ListColdChains(
      HouseholdRepository households, FridgeRepository fridges, ColdChainRepository chains) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.chains = Objects.requireNonNull(chains, "chains");
  }

  public List<ColdChain> list(UserId actor, HouseholdId household) {
    households.get(actor, household);
    return fridges.findByHousehold(household).stream()
        .map(fridge -> chains.find(fridge.id()).orElseGet(() -> ColdChain.start(fridge.id())))
        .toList();
  }
}
