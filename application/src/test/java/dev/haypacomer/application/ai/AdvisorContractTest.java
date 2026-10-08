package dev.haypacomer.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.QuantityParser;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdvisorContractTest {

  private static SuggestionRequest request(int servings, Integer minutes, int limit) {
    return new SuggestionRequest(
        List.of(),
        new Availability(),
        Set.of(),
        DiningGroup.of(),
        SubstitutionCatalog.EMPTY,
        servings,
        minutes,
        limit);
  }

  @Test
  void suggestionRequestsStayWithinTheContract() {
    assertTrue(request(2, null, 3).minutesLimit().isEmpty());
    assertEquals(20, request(2, 20, 1).minutesLimit().orElseThrow());
    assertThrows(IllegalArgumentException.class, () -> request(0, null, 3));
    assertThrows(IllegalArgumentException.class, () -> request(2, 0, 3));
    assertThrows(IllegalArgumentException.class, () -> request(2, null, 0));
    assertThrows(IllegalArgumentException.class, () -> request(2, null, 4));
    assertTrue(new Suggestions(List.of(), AdvisorSource.GEMINI).items().isEmpty());
  }

  @Test
  void parsedIntentsNeedAFoodAndAConfidenceBetweenZeroAndOne() {
    ParsedIntent intent =
        new ParsedIntent(
            IntentAction.STOCK,
            " soup ",
            QuantityParser.parse("300 g"),
            LocalDate.of(2026, 10, 9),
            0.9,
            AdvisorSource.OFFLINE_RULES);

    assertEquals("soup", intent.food());
    assertTrue(intent.amount().isPresent());
    assertTrue(intent.expiry().isPresent());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ParsedIntent(
                IntentAction.CONSUME, " ", null, null, 0.5, AdvisorSource.OFFLINE_RULES));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ParsedIntent(
                IntentAction.CONSUME, "rice", null, null, 1.5, AdvisorSource.OFFLINE_RULES));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ParsedIntent(
                IntentAction.CONSUME, "rice", null, null, -0.1, AdvisorSource.OFFLINE_RULES));
    assertEquals(
        Urgency.OK,
        new StatusExplanation("Rice", Urgency.OK, "Rice is fine.", AdvisorSource.OFFLINE_RULES)
            .urgency());
  }
}
