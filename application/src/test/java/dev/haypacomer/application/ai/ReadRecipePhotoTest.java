package dev.haypacomer.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.RecipePhotoReader;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReadRecipePhotoTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D};
  private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final UserId owner = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  private final List<AiAuditEntry> audited = new ArrayList<>();
  private final AiAuditLog audit =
      new AiAuditLog() {
        @Override
        public void record(AiAuditEntry entry) {
          audited.add(entry);
        }

        @Override
        public List<AiAuditEntry> recent(int limit) {
          return audited;
        }
      };
  private boolean allowed = true;
  private final AiRateLimiter limiter = (subject, limit, window) -> allowed;

  {
    households.save(home);
    stores.catalog.save(
        new FoodMetadata(
            "Rice",
            FoodCategory.OTHER,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            false,
            365,
            Set.of()));
  }

  private ReadRecipePhoto reading(RecipePhotoReader reader) {
    return new ReadRecipePhoto(
        households, reader, stores.catalog, limiter, audit, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static RecipePhotoReader answering(PhotoRecipe recipe) {
    return new RecipePhotoReader() {
      @Override
      public String provider() {
        return "gemini";
      }

      @Override
      public PhotoRecipe read(RecipePhoto photo) {
        return recipe;
      }
    };
  }

  @Test
  void verifiesEveryIngredientAgainstTheCatalogAndTheScale() {
    PhotoRecipe read =
        new PhotoRecipe(
            "Arroz blanco",
            4,
            25,
            List.of(
                new PhotoIngredient("rice", "200 g + 200 g"),
                new PhotoIngredient("saffron", "1 pinch"),
                new PhotoIngredient("Rice", "two handfuls")),
            List.of("Rinse", "Boil"),
            0.8,
            AdvisorSource.GEMINI);

    RecipeDraft draft =
        reading(answering(read)).read(owner, home.id(), new RecipePhoto("image/jpeg", JPEG));

    assertEquals("Arroz blanco", draft.name());
    assertEquals(Grams.of(400), draft.ingredients().getFirst().grams());
    assertEquals("Rice", draft.ingredients().getFirst().catalogFood());
    assertTrue(draft.ingredients().getFirst().verified());
    assertEquals("Not in the food catalog", draft.ingredients().get(1).issue().orElseThrow());
    assertTrue(
        draft.ingredients().get(2).issue().orElseThrow().startsWith("Quantity cannot be weighed"));
    assertFalse(draft.verified());
    assertEquals(List.of("Rinse", "Boil"), draft.steps());
    assertEquals(AiOutcome.VALID, audited.getFirst().outcome());
    assertEquals("readRecipePhoto", audited.getFirst().operation());
  }

  @Test
  void aFullyVerifiedDraftIsMarkedSo() {
    PhotoRecipe read =
        new PhotoRecipe(
            "Rice",
            2,
            20,
            List.of(new PhotoIngredient("rice", "150 g")),
            List.of(),
            0.9,
            AdvisorSource.GEMINI);

    assertTrue(
        reading(answering(read))
            .read(owner, home.id(), new RecipePhoto("image/png", PNG))
            .verified());
    assertFalse(
        new RecipeDraft("x", 1, 1, List.of(), List.of(), 0.1, AdvisorSource.OFFLINE_RULES)
            .verified());
  }

  @Test
  void unavailableReadersAreAuditedAndStrangersAndFloodsRejected() {
    RecipePhotoReader offline =
        new RecipePhotoReader() {
          @Override
          public String provider() {
            return "offline";
          }

          @Override
          public PhotoRecipe read(RecipePhoto photo) {
            throw new PhotoReadingUnavailableException("Reading photos needs an AI provider");
          }
        };
    RecipePhoto photo = new RecipePhoto("image/webp", WEBP);

    assertThrows(
        PhotoReadingUnavailableException.class,
        () -> reading(offline).read(owner, home.id(), photo));
    assertEquals(AiOutcome.UNAVAILABLE, audited.getFirst().outcome());
    assertThrows(
        HouseholdNotFoundException.class,
        () -> reading(offline).read(UserId.newId(), home.id(), photo));
    allowed = false;
    assertThrows(
        AiRateLimitExceededException.class, () -> reading(offline).read(owner, home.id(), photo));
  }

  @Test
  void photosAreCheckedByTypeSizeAndContent() {
    assertThrows(IllegalArgumentException.class, () -> new RecipePhoto("image/gif", JPEG));
    assertThrows(IllegalArgumentException.class, () -> new RecipePhoto("image/png", JPEG));
    assertThrows(IllegalArgumentException.class, () -> new RecipePhoto("image/jpeg", new byte[0]));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RecipePhoto("image/jpeg", new byte[RecipePhoto.MAX_BYTES + 1]));
    assertThrows(
        IllegalArgumentException.class, () -> new RecipePhoto("image/webp", new byte[] {'R'}));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RecipePhoto(
                "image/webp", new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'A', 'V', 'I', 'F'}));
    RecipePhoto photo = new RecipePhoto("image/jpeg", JPEG);
    byte[] copy = photo.bytes();
    copy[3] = 9;
    assertEquals(0, photo.bytes()[3]);
    assertEquals(photo, new RecipePhoto("image/jpeg", JPEG.clone()));
    assertEquals(photo.hashCode(), new RecipePhoto("image/jpeg", JPEG.clone()).hashCode());
    assertNotEquals(photo, new RecipePhoto("image/png", PNG));
    assertNotEquals(photo, "photo");
    assertEquals("RecipePhoto[image/jpeg, 4 bytes]", photo.toString());
  }
}
