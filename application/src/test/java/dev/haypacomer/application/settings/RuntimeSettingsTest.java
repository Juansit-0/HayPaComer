package dev.haypacomer.application.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.auth.AuthSettings;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemorySettingsRepository;
import dev.haypacomer.domain.coldchain.investigation.ColdRule;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuntimeSettingsTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemorySettingsRepository settings = new InMemorySettingsRepository();
  private final StoredPolicies policies = new StoredPolicies(settings);
  private final UserId owner = UserId.newId();
  private final UserId member = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneId.of("UTC"), owner, NOW);
  private final ChangeHouseholdSetting change = new ChangeHouseholdSetting(households, settings);
  private final ViewHouseholdSettings view = new ViewHouseholdSettings(households, settings);

  RuntimeSettingsTest() {
    home.join(member, Role.MEMBER, NOW);
    households.save(home);
  }

  @Test
  void storedDefaultsMatchTheFixedDefaults() {
    FixedPolicies fixed = FixedPolicies.DEFAULT;
    assertEquals(fixed.freshness(home.id()), policies.freshness(home.id()));
    assertEquals(fixed.thresholds(home.id()), policies.thresholds(home.id()));
    assertEquals(fixed.briefings(home.id()), policies.briefings(home.id()));
    assertEquals(fixed.coldRule(), policies.coldRule());
    assertEquals(fixed.circuit(), policies.circuit());
    assertEquals(fixed.auth(), policies.auth());
    assertEquals(fixed.aiCallsPerMinute(), policies.aiCallsPerMinute());
    assertEquals(fixed.expiryRules(home.id()), policies.expiryRules(home.id()));
    assertEquals(FridgeThresholds.DEFAULT, policies.thresholdsOf(FridgeId.newId()));
  }

  @Test
  void householdOverridesChangeOnlyThatHousehold() {
    HouseholdId other = HouseholdId.newId();
    FridgeId fridge = FridgeId.newId();
    settings.place(fridge, home.id());

    change.change(owner, home.id(), "food.at-risk-days", Optional.of("4"));
    change.change(owner, home.id(), "fridge.max-celsius", Optional.of("4.50"));
    change.change(owner, home.id(), "fridge.door-alert-seconds", Optional.of("60"));
    change.change(owner, home.id(), "briefing.morning-hour", Optional.of("6"));

    assertEquals(new FreshnessPolicy(4), policies.freshness(home.id()));
    assertEquals(FreshnessPolicy.DEFAULT, policies.freshness(other));
    assertEquals(new BigDecimal("4.5"), policies.thresholdsOf(fridge).maxCelsius());
    assertEquals(Duration.ofSeconds(60), policies.thresholdsOf(fridge).doorAlertAfter());
    assertEquals(6, policies.briefings(home.id()).morningHour());
    assertEquals("4.5", settings.overrides(home.id()).get("fridge.max-celsius"));
  }

  @Test
  void viewShowsHouseholdSettingsWithTheirOverrides() {
    change.change(owner, home.id(), "food.at-risk-days", Optional.of("3"));

    List<SettingView> shown = view.view(member, home.id());

    assertEquals(9, shown.size());
    SettingView atRisk =
        shown.stream()
            .filter(setting -> setting.definition().key().equals("food.at-risk-days"))
            .findFirst()
            .orElseThrow();
    assertEquals("3", atRisk.value());
    assertTrue(atRisk.overridden());
    assertTrue(
        shown.stream().allMatch(setting -> setting.definition().scope() == SettingScope.HOUSEHOLD));
  }

  @Test
  void resetGoesBackToTheDefault() {
    change.change(owner, home.id(), "food.at-risk-days", Optional.of("5"));

    SettingView reset = change.change(owner, home.id(), "food.at-risk-days", Optional.empty());

    assertEquals("2", reset.value());
    assertFalse(reset.overridden());
    assertEquals(FreshnessPolicy.DEFAULT, policies.freshness(home.id()));
  }

  @Test
  void onlyOwnersChangeAndOnlyMembersSee() {
    assertThrows(
        AccessDeniedException.class,
        () -> change.change(member, home.id(), "food.at-risk-days", Optional.of("3")));
    assertThrows(HouseholdNotFoundException.class, () -> view.view(UserId.newId(), home.id()));
  }

  @Test
  void valuesAreCheckedAgainstTheirRangeKindAndScope() {
    assertEquals(
        "food.at-risk-days must be between 0 and 14",
        assertThrows(
                IllegalArgumentException.class,
                () -> change.change(owner, home.id(), "food.at-risk-days", Optional.of("40")))
            .getMessage());
    assertEquals(
        "food.at-risk-days must be a whole number",
        assertThrows(
                IllegalArgumentException.class,
                () -> change.change(owner, home.id(), "food.at-risk-days", Optional.of("2.5")))
            .getMessage());
    assertEquals(
        "fridge.max-celsius must be a number",
        assertThrows(
                IllegalArgumentException.class,
                () -> change.change(owner, home.id(), "fridge.max-celsius", Optional.of("cold")))
            .getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () -> change.change(owner, home.id(), "auth.refresh-days", Optional.of("10")));
    assertThrows(
        IllegalArgumentException.class,
        () -> change.change(owner, home.id(), "nothing", Optional.of("1")));
    assertEquals(
        "2", change.change(owner, home.id(), "food.at-risk-days", Optional.of(" 2.0 ")).value());
  }

  @Test
  void globalSettingsIgnoreHouseholdOverrides() {
    settings.override(home.id(), "ai.calls-per-minute", "99");

    assertEquals(new BigDecimal("20"), policies.value("ai.calls-per-minute", home.id()));
    assertThrows(IllegalStateException.class, () -> policies.value("missing", home.id()));
  }

  @Test
  void definitionsListEverySetting() {
    assertEquals(18, new ListSettingDefinitions(settings).list().size());
  }

  @Test
  void fixedPoliciesKeepTheirValues() {
    FixedPolicies fixed =
        FixedPolicies.DEFAULT
            .withFreshness(new FreshnessPolicy(5))
            .withThresholds(FridgeThresholds.DEFAULT.withMinimumWeightChange(BigDecimal.TEN));

    assertEquals(new FreshnessPolicy(5), fixed.freshness(HouseholdId.newId()));
    assertEquals(BigDecimal.TEN, fixed.thresholdsOf(FridgeId.newId()).minimumWeightChange());
    assertEquals(BriefingSchedule.DEFAULT, fixed.briefings(HouseholdId.newId()));
    assertEquals(CircuitPolicy.DEFAULT, fixed.circuit());
    assertEquals(AuthSettings.DEFAULT, fixed.auth());
    assertEquals(ColdRule.DEFAULT, fixed.coldRule());
  }

  @Test
  void schedulesAndRulesRejectImpossibleValues() {
    assertThrows(IllegalArgumentException.class, () -> new BriefingSchedule(24, 8, 3));
    assertThrows(IllegalArgumentException.class, () -> new BriefingSchedule(7, -1, 3));
    assertThrows(IllegalArgumentException.class, () -> new BriefingSchedule(7, 8, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ColdRule(Duration.ofMinutes(30), Duration.ofMinutes(30)));
  }
}
