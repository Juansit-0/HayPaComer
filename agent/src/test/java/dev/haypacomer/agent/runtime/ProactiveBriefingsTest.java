package dev.haypacomer.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.kitchen.KitchenFixture;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ViewColdChainTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.proactive.AlertBriefings;
import dev.haypacomer.agent.proactive.Briefer;
import dev.haypacomer.agent.proactive.ScheduledBriefings;
import dev.haypacomer.agent.supervisor.KeywordRouter;
import dev.haypacomer.agent.supervisor.OfflinePlanners;
import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.port.BriefingLog;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProactiveBriefingsTest {

  private final KitchenFixture kitchen = new KitchenFixture();
  private final InMemoryAgentStores stores = new InMemoryAgentStores();
  private final List<Notification> delivered = new ArrayList<>();
  private final Set<String> claimed = new HashSet<>();
  private final BriefingLog log =
      (household, kind, day) -> claimed.add(household.value() + kind + day);
  private final List<Runnable> queued = new ArrayList<>();

  private Briefer briefer(Clock clock) {
    ToolRegistry tools =
        new ToolRegistry(
            List.of(
                new QueryInventoryTool(kitchen.inventory, kitchen.today),
                new ViewExpiriesTool(kitchen.inventory, kitchen.today),
                new ViewColdChainTool(kitchen.coldChains())));
    Supervisor supervisor =
        new Supervisor(
            kitchen.households,
            tools,
            new GuardrailChain(
                List.of(new SchemaGuardrail(), new PermissionGuardrail(kitchen.households))),
            new KeywordRouter(),
            new OfflinePlanners(),
            stores,
            stores,
            stores,
            clock);
    return new Briefer(
        kitchen.households, supervisor, new NotifyHousehold(List.of(delivered::add)), clock);
  }

  private ScheduledBriefings scheduled(Instant at) {
    Clock clock = Clock.fixed(at, ZoneOffset.UTC);
    return new ScheduledBriefings(
        () -> List.of(kitchen.home.id(), HouseholdId.newId()),
        kitchen.households,
        kitchen.inventory,
        log,
        briefer(clock),
        digest(clock),
        clock);
  }

  private dev.haypacomer.application.analytics.BuildWeeklyDigest digest(Clock clock) {
    dev.haypacomer.application.port.FoodPriceRepository prices =
        new dev.haypacomer.application.port.FoodPriceRepository() {
          @Override
          public java.util.Map<String, java.math.BigDecimal> pricesFor(
              HouseholdId household, java.util.Currency currency) {
            return java.util.Map.of();
          }

          @Override
          public void save(HouseholdId household, String foodKey, java.math.BigDecimal price) {}
        };
    return new dev.haypacomer.application.analytics.BuildWeeklyDigest(
        kitchen.households,
        new dev.haypacomer.application.analytics.ViewHouseholdMetrics(
            kitchen.households,
            kitchen.stores.history,
            prices,
            dev.haypacomer.domain.inventory.FreshnessPolicy.DEFAULT),
        kitchen.stores.history,
        new dev.haypacomer.application.analytics.HouseholdMemberNames(
            kitchen.households,
            new dev.haypacomer.application.port.UserRepository() {
              @Override
              public void save(dev.haypacomer.domain.identity.User user) {}

              @Override
              public java.util.Optional<dev.haypacomer.domain.identity.User> findById(
                  dev.haypacomer.domain.identity.UserId id) {
                return java.util.Optional.empty();
              }

              @Override
              public java.util.Optional<dev.haypacomer.domain.identity.User> findByEmail(
                  dev.haypacomer.domain.identity.EmailAddress email) {
                return java.util.Optional.empty();
              }
            }),
        clock);
  }

  @Test
  void mondayMorningBringsTheWeeklyDigestOnce() {
    kitchen.stores.movements.record(
        new dev.haypacomer.domain.inventory.InventoryMovement(
            java.util.UUID.randomUUID(),
            kitchen.home.id(),
            dev.haypacomer.domain.fridge.FoodItemId.newId(),
            kitchen.owner,
            dev.haypacomer.domain.inventory.MovementType.DISCARD,
            new java.math.BigDecimal("-250"),
            dev.haypacomer.domain.inventory.MovementSource.MANUAL,
            Instant.parse("2026-10-07T10:00:00Z"),
            "yogurt",
            null));
    Instant mondayEight = Instant.parse("2026-10-12T08:20:00Z");

    assertEquals(1, scheduled(mondayEight).run());
    assertEquals(0, scheduled(mondayEight.plusSeconds(60)).run());
    assertEquals(0, scheduled(Instant.parse("2026-10-13T08:20:00Z")).run());

    Notification weekly = delivered.getFirst();
    assertEquals("Your week in the kitchen", weekly.title());
    assertTrue(weekly.body().contains("You threw away 250 g (100% of what left the fridge"));
  }

  @Test
  void theMorningBriefingGoesOutOncePerDayAtSeven() {
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));
    Instant seven = KitchenFixture.TODAY.atTime(7, 15).toInstant(ZoneOffset.UTC);

    assertEquals(0, scheduled(seven.minusSeconds(3600)).run());
    assertEquals(1, scheduled(seven).run());
    assertEquals(0, scheduled(seven.plusSeconds(600)).run());

    Notification briefing = delivered.getFirst();
    assertEquals(NotificationType.BRIEFING, briefing.type());
    assertEquals("Today in your kitchen", briefing.title());
    assertTrue(briefing.body().contains("Chicken breast 650 g"));
    assertEquals("chef", stores.runs.values().iterator().next().specialist());
  }

  @Test
  void aClusterOfExpiringFoodBringsTheCoachOncePerDay() {
    kitchen.put("Milk", 900, KitchenFixture.TODAY.plusDays(1));
    kitchen.put("Yogurt", 250, KitchenFixture.TODAY.plusDays(2));
    kitchen.put("Ham", 200, KitchenFixture.TODAY);
    Instant noon = KitchenFixture.TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC);

    assertEquals(1, scheduled(noon).run());
    assertEquals(0, scheduled(noon.plusSeconds(3600)).run());
    assertEquals("3 foods expire soon", delivered.getFirst().title());
    assertTrue(delivered.getFirst().body().startsWith("Offline answer based on measured data"));
  }

  @Test
  void fridgeAlertsGetOneColdBriefingPerTypeAndDayInTheBackground() {
    AlertBriefings alerts =
        new AlertBriefings(kitchen.households, log, briefer(kitchen.clock), queued::add);
    Notification door =
        Notification.of(
            kitchen.home.id(),
            NotificationType.DOOR_LEFT_OPEN,
            "Door left open",
            "Open for 40 s",
            KitchenFixture.NOW);

    alerts.onNotification(door);
    alerts.onNotification(door);
    alerts.onNotification(
        Notification.of(
            kitchen.home.id(), NotificationType.BRIEFING, "x", "y", KitchenFixture.NOW));
    alerts.onNotification(
        Notification.of(
            HouseholdId.newId(), NotificationType.COLD_CHAIN_BREACH, "x", "y", KitchenFixture.NOW));

    assertEquals(3, queued.size());
    assertTrue(delivered.isEmpty());
    queued.forEach(Runnable::run);
    assertEquals(1, delivered.size());
    assertEquals("What to check after: Door left open", delivered.getFirst().title());
    assertTrue(delivered.getFirst().body().contains("view_cold_chain"));
  }

  @Test
  void oneFailingHouseholdDoesNotStopTheOthers() {
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));
    Clock seven =
        Clock.fixed(KitchenFixture.TODAY.atTime(7, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    HouseholdId broken = HouseholdId.newId();
    ScheduledBriefings briefings =
        new ScheduledBriefings(
            () -> List.of(broken, kitchen.home.id()),
            new dev.haypacomer.application.port.HouseholdRepository() {
              @Override
              public void save(dev.haypacomer.domain.household.Household household) {}

              @Override
              public java.util.Optional<dev.haypacomer.domain.household.Household> findById(
                  HouseholdId id) {
                if (id.equals(broken)) {
                  throw new IllegalStateException("database hiccup");
                }
                return kitchen.households.findById(id);
              }

              @Override
              public List<dev.haypacomer.domain.household.Household> findByUser(
                  dev.haypacomer.domain.identity.UserId user) {
                return List.of();
              }
            },
            kitchen.inventory,
            log,
            briefer(seven),
            digest(seven),
            seven);

    assertEquals(1, briefings.run());
    assertEquals(1, briefings.failures());
  }

  @Test
  void aBrokenBriefingNeverBreaksTheAlert() {
    BriefingLog down =
        (household, kind, day) -> {
          throw new IllegalStateException("Redis is down");
        };
    AlertBriefings alerts =
        new AlertBriefings(kitchen.households, down, briefer(kitchen.clock), Runnable::run);

    alerts.onNotification(
        Notification.of(
            kitchen.home.id(),
            NotificationType.COLD_CHAIN_BREACH,
            "Cold chain broken",
            "6 C for 20 min",
            KitchenFixture.NOW));

    assertEquals(1, alerts.failures());
    assertTrue(delivered.isEmpty());
  }
}
