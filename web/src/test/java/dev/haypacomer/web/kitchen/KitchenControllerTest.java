package dev.haypacomer.web.kitchen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.HayPaComerFacade.KitchenSnapshot;
import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.AtRiskFood;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.web.error.ApiExceptionHandler;
import dev.haypacomer.web.security.JwtAccessTokenIssuer;
import dev.haypacomer.web.security.JwtProperties;
import dev.haypacomer.web.security.SecurityConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KitchenController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(
    properties = {
      "haypacomer.security.jwt.secret=test-secret-with-at-least-32-bytes!!",
      "haypacomer.security.jwt.issuer=https://api.haypacomer.dev",
      "haypacomer.security.jwt.access-token-time-to-live=15m"
    })
class KitchenControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private JwtEncoder encoder;
  @Autowired private JwtProperties properties;
  @MockitoBean private HayPaComerFacade facade;
  @MockitoBean private AuthenticateDevice authenticateDevice;

  private final UserId juan = UserId.newId();
  private final HouseholdId household = HouseholdId.newId();

  private String bearer() {
    return "Bearer "
        + new JwtAccessTokenIssuer(encoder, properties).issue(juan, Instant.now()).value();
  }

  private String path(String suffix) {
    return "/api/v1/households/" + household.value() + suffix;
  }

  private Fridge fridgeWithSoup() {
    Fridge fridge = Fridge.named("Kitchen");
    Zone shelves = Zone.named("Shelves", ZoneKind.SHELF);
    Tray top = Tray.named("Top", 0);
    shelves.add(top);
    fridge.add(shelves);
    fridge.place(
        new FoodItem(
            FoodItemId.newId(),
            new FoodMetadata(
                "Soup",
                FoodCategory.PREPARED,
                Unit.GRAM,
                ConversionFactors.MASS_ONLY,
                true,
                3,
                Set.of()),
            Grams.of(300),
            Grams.ZERO,
            LocalDate.of(2026, 10, 3)),
        top.id());
    return fridge;
  }

  @Test
  void setsUpAFridgeWithTheStandardLayoutByDefault() throws Exception {
    when(facade.setUpFridge(eq(juan), eq(household), eq("Kitchen"), eq(FridgeLayout.STANDARD)))
        .thenReturn(fridgeWithSoup());

    mvc.perform(
            post(path("/fridges"))
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kitchen\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("FRIDGE"))
        .andExpect(jsonPath("$.children[0].type").value("ZONE"))
        .andExpect(jsonPath("$.children[0].children[0].children[0].type").value("FOODITEM"))
        .andExpect(jsonPath("$.grams").value(300.0));
  }

  @Test
  void listsFridgesAsATwin() throws Exception {
    when(facade.fridges(juan, household)).thenReturn(List.of(fridgeWithSoup()));

    mvc.perform(get(path("/fridges")).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].items").value(1));
  }

  @Test
  void showsInventoryWithStatusesAndTheKitchenSnapshot() throws Exception {
    Fridge fridge = fridgeWithSoup();
    Tray tray = fridge.trays().findFirst().orElseThrow();
    FoodItem soup = tray.children().getFirst();
    InventoryEntry entry =
        new InventoryEntry(fridge.id(), tray.id(), new AtRiskFood(new PlainFood(soup)), true);
    when(facade.inventory(juan, household)).thenReturn(List.of(entry));
    when(facade.rescueFirst(juan, household)).thenReturn(List.of(entry));
    when(facade.snapshot(any(), any())).thenReturn(new KitchenSnapshot(1, Grams.of(300), 1, 0));

    mvc.perform(get(path("/inventory")).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Soup"))
        .andExpect(jsonPath("$[0].statuses[0]").value("AT_RISK"))
        .andExpect(jsonPath("$[0].rescuePriority").value(2))
        .andExpect(jsonPath("$[0].usable").value(true));
    mvc.perform(get(path("/inventory?rescueFirst=true")).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get(path("/kitchen")).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.atRisk").value(1));
  }
}
