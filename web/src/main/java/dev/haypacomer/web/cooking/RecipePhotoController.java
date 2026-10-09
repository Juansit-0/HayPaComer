package dev.haypacomer.web.cooking;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.DraftIngredient;
import dev.haypacomer.application.ai.ReadRecipePhoto;
import dev.haypacomer.application.ai.RecipeDraft;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/households/{householdId}/recipes")
public class RecipePhotoController {

  private final ReadRecipePhoto readRecipePhoto;

  public RecipePhotoController(ReadRecipePhoto readRecipePhoto) {
    this.readRecipePhoto = readRecipePhoto;
  }

  @PostMapping(path = "/from-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  DraftResponse read(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestPart("photo") MultipartFile photo)
      throws IOException {
    String type = photo.getContentType() == null ? "" : photo.getContentType();
    RecipeDraft draft =
        readRecipePhoto.read(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new RecipePhoto(type, photo.getBytes()));
    return DraftResponse.from(draft);
  }

  record IngredientResponse(
      String readFood,
      String readQuantity,
      String catalogFood,
      BigDecimal grams,
      boolean verified,
      String problem) {

    static IngredientResponse from(DraftIngredient ingredient) {
      return new IngredientResponse(
          ingredient.readFood(),
          ingredient.readQuantity(),
          ingredient.catalogFood(),
          ingredient.grams() == null ? null : ingredient.grams().value(),
          ingredient.verified(),
          ingredient.problem());
    }
  }

  record DraftResponse(
      String name,
      int servings,
      int minutes,
      List<IngredientResponse> ingredients,
      List<String> steps,
      double confidence,
      AdvisorSource source,
      boolean verified,
      boolean saved) {

    static DraftResponse from(RecipeDraft draft) {
      return new DraftResponse(
          draft.name(),
          draft.servings(),
          draft.minutes(),
          draft.ingredients().stream().map(IngredientResponse::from).toList(),
          draft.steps(),
          draft.confidence(),
          draft.source(),
          draft.verified(),
          false);
    }
  }
}
