package dev.haypacomer.application.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class HouseholdMemoryTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

  private final Map<HouseholdId, Map<String, String>> stored = new HashMap<>();
  private final HouseholdMemory memory =
      new HouseholdMemory() {
        @Override
        public Map<String, String> read(HouseholdId household) {
          return new TreeMap<>(stored.getOrDefault(household, Map.of()));
        }

        @Override
        public void remember(HouseholdId household, String key, String value) {
          stored.computeIfAbsent(household, id -> new HashMap<>()).put(key, value);
        }

        @Override
        public void forget(HouseholdId household, String key) {
          stored.getOrDefault(household, new HashMap<>()).remove(key);
        }
      };
  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final UserId owner = UserId.newId();
  private final UserId guest = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  private final ViewHouseholdMemory view = new ViewHouseholdMemory(households, memory);
  private final RememberForHousehold remember = new RememberForHousehold(households, memory);
  private final ForgetForHousehold forget = new ForgetForHousehold(households, memory);
  private final ClearHouseholdMemory clear = new ClearHouseholdMemory(households, memory);

  {
    home.join(guest, Role.GUEST, NOW);
    households.save(home);
  }

  @Test
  void membersTeachAndEditWhatTheAgentRemembers() {
    remember.remember(
        owner, home.id(), new MemoryNote(MemoryTopic.USUAL_QUANTITY, "Rice", "150 g"));
    remember.remember(
        owner, home.id(), new MemoryNote(MemoryTopic.PREFERENCE, "  Spicy   food ", "No"));
    remember.remember(
        owner, home.id(), new MemoryNote(MemoryTopic.ACCEPTED_DISH, "arroz con pollo", "twice"));
    remember.remember(
        owner, home.id(), new MemoryNote(MemoryTopic.PREFERENCE, "spicy food", "Mild"));
    stored.get(home.id()).put("legacy", "ignored");
    stored.get(home.id()).put("unknown:topic", "ignored");

    assertEquals(
        List.of(
            new MemoryNote(MemoryTopic.PREFERENCE, "spicy food", "Mild"),
            new MemoryNote(MemoryTopic.USUAL_QUANTITY, "rice", "150 g"),
            new MemoryNote(MemoryTopic.ACCEPTED_DISH, "arroz con pollo", "twice")),
        view.view(guest, home.id()));
    assertEquals(
        "usual:rice",
        stored.get(home.id()).keySet().stream()
            .filter(k -> k.startsWith("usual"))
            .findFirst()
            .orElseThrow());

    forget.forget(owner, home.id(), MemoryTopic.PREFERENCE, " Spicy Food ");

    assertEquals(2, view.view(owner, home.id()).size());
  }

  @Test
  void guestsReadButCannotEditAndStrangersSeeNothing() {
    MemoryNote note = new MemoryNote(MemoryTopic.DECISION, "milk brand", "Alpina");

    assertThrows(AccessDeniedException.class, () -> remember.remember(guest, home.id(), note));
    assertThrows(
        AccessDeniedException.class,
        () -> forget.forget(guest, home.id(), MemoryTopic.DECISION, "milk brand"));
    assertThrows(HouseholdNotFoundException.class, () -> view.view(UserId.newId(), home.id()));
    assertTrue(view.view(guest, home.id()).isEmpty());
  }

  @Test
  void notesStayShortAndUsualQuantitiesMustBeReadable() {
    assertThrows(
        InvalidQuantityException.class,
        () -> new MemoryNote(MemoryTopic.USUAL_QUANTITY, "rice", "a lot"));
    assertThrows(
        IllegalArgumentException.class, () -> new MemoryNote(MemoryTopic.PREFERENCE, " ", "x"));
    assertThrows(
        IllegalArgumentException.class, () -> new MemoryNote(MemoryTopic.PREFERENCE, "a:b", "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MemoryNote(MemoryTopic.PREFERENCE, "x".repeat(61), "x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MemoryNote(MemoryTopic.DECISION, "x", "y".repeat(301)));
    assertThrows(
        IllegalArgumentException.class, () -> new MemoryNote(MemoryTopic.DECISION, "x", " "));
    assertEquals(Optional.empty(), MemoryNote.fromEntry("usual:rice", "plenty"));
  }

  @Test
  void onlyTheOwnerClearsEverything() {
    remember.remember(owner, home.id(), new MemoryNote(MemoryTopic.DECISION, "milk", "Alpina"));
    stored.get(home.id()).put("legacy", "x");

    assertThrows(AccessDeniedException.class, () -> clear.clear(guest, home.id()));
    assertEquals(2, clear.clear(owner, home.id()));
    assertTrue(stored.get(home.id()).isEmpty());
  }

  @Test
  void memoryHasACeiling() {
    for (int i = 0; i < RememberForHousehold.MAX_NOTES; i++) {
      memory.remember(home.id(), "decision:n" + i, "v");
    }

    assertThrows(
        IllegalStateException.class,
        () ->
            remember.remember(
                owner, home.id(), new MemoryNote(MemoryTopic.DECISION, "one more", "v")));
    remember.remember(owner, home.id(), new MemoryNote(MemoryTopic.DECISION, "n1", "updated"));
    assertEquals("updated", stored.get(home.id()).get("decision:n1"));
  }
}
