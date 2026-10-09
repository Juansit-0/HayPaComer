package dev.haypacomer.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.runtime.ToolKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

  private static AgentTool tool(ToolSpec spec) {
    return new AgentTool() {
      @Override
      public ToolSpec spec() {
        return spec;
      }

      @Override
      public String describe(ToolInvocation invocation) {
        return spec.name();
      }

      @Override
      public Observation invoke(ToolInvocation invocation) {
        return Observation.of(spec.name(), "ok");
      }
    };
  }

  private final ToolSpec inventory = ToolSpec.read("query_inventory", "Inventory", List.of());
  private final ToolSpec market =
      ToolSpec.write(
          "add_to_market",
          "Market",
          Permission.MANAGE_MARKET_LIST,
          List.of(
              ParameterSpec.required("food", ParameterType.TEXT, "Food"),
              ParameterSpec.optional("grams", ParameterType.GRAMS, "Grams"),
              ParameterSpec.optional("by", ParameterType.DATE, "Date"),
              ParameterSpec.optional("item", ParameterType.ID, "Item"),
              ParameterSpec.optional("servings", ParameterType.COUNT, "Servings")));
  private final ToolRegistry registry = new ToolRegistry(List.of(tool(inventory), tool(market)));

  @Test
  void findsToolsAndKeepsRegistrationOrder() {
    assertEquals(List.of(inventory, market), registry.specs());
    assertEquals(Set.of("query_inventory", "add_to_market"), registry.names());
    assertEquals(ToolKind.WRITE, registry.find("add_to_market").orElseThrow().kind());
    assertTrue(registry.find("drop_table").isEmpty());
  }

  @Test
  void allowlistsNarrowTheRegistryForASpecialist() {
    ToolRegistry chef = registry.allow(Set.of("query_inventory"));

    assertEquals(List.of(inventory), chef.specs());
    assertThrows(IllegalArgumentException.class, () -> registry.allow(Set.of("weather")));
  }

  @Test
  void rejectsDuplicatesAndMalformedSpecs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ToolRegistry(List.of(tool(inventory), tool(inventory))));
    assertThrows(IllegalArgumentException.class, () -> ToolSpec.read("Query", "x", List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> ToolSpec.write("add_to_market", "x", Permission.VIEW_HOUSEHOLD, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ToolSpec.read(
                "query_inventory",
                "x",
                List.of(
                    ParameterSpec.required("zone", ParameterType.TEXT, "Zone"),
                    ParameterSpec.optional("zone", ParameterType.TEXT, "Zone"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> ParameterSpec.required("Zone", ParameterType.TEXT, "Zone"));
  }

  @Test
  void schemaChecksEveryParameterType() {
    SchemaGuardrail schema = new SchemaGuardrail();
    assertEquals(Optional.empty(), schema.reject(market, call(Map.of("food", " rice "))));
    assertEquals(
        Optional.empty(),
        schema.reject(
            market,
            call(
                Map.of(
                    "food",
                    "rice",
                    "grams",
                    "500",
                    "by",
                    "2026-10-12",
                    "item",
                    "6f1c2a5e-8f7b-4c1e-9a0d-2b3c4d5e6f70",
                    "servings",
                    "4"))));
    assertEquals(
        Optional.of("Argument food must not be blank"),
        schema.reject(market, call(Map.of("food", " "))));
    assertEquals(
        Optional.of("Argument food must be at most 200 characters"),
        schema.reject(market, call(Map.of("food", "x".repeat(201)))));
    assertEquals(
        Optional.of("Argument grams must be whole grams between 1 and 100000"),
        schema.reject(market, call(Map.of("food", "rice", "grams", "lots"))));
    assertEquals(
        Optional.of("Argument by must be an ISO date"),
        schema.reject(market, call(Map.of("food", "rice", "by", "Friday"))));
    assertEquals(
        Optional.of("Argument item must be an id"),
        schema.reject(market, call(Map.of("food", "rice", "item", "42"))));
    assertEquals(
        Optional.of("Argument item must be an id"),
        schema.reject(market, call(Map.of("food", "rice", "item", "1-1-1-1-1"))));
    assertEquals(
        Optional.of("Argument servings must be a whole number between 1 and 50"),
        schema.reject(market, call(Map.of("food", "rice", "servings", "500"))));
  }

  @Test
  void theChainStopsAtTheFirstRejection() {
    Guardrail never = (spec, invocation) -> Optional.of("never");
    Guardrail unreachable =
        (spec, invocation) -> {
          throw new AssertionError("chain must stop");
        };

    assertEquals(
        Optional.of("never"),
        new GuardrailChain(List.of(new SchemaGuardrail(), never, unreachable))
            .reject(inventory, call(Map.of())));
    assertEquals(
        Optional.empty(),
        new GuardrailChain(List.of(new SchemaGuardrail())).reject(inventory, call(Map.of())));
  }

  private static ToolInvocation call(Map<String, String> arguments) {
    return new ToolInvocation(HouseholdId.newId(), UserId.newId(), arguments);
  }
}
