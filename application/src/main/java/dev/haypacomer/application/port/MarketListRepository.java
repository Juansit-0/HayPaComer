package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.market.MarketList;
import java.util.Optional;

public interface MarketListRepository {

  void save(MarketList list);

  Optional<MarketList> findByHousehold(HouseholdId household);
}
