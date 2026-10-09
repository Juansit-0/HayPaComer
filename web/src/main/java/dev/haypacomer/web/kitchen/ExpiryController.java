package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.inventory.EstimateExpiry;
import dev.haypacomer.application.inventory.ExpiryProposal;
import dev.haypacomer.application.inventory.ReadExpiryFromLabel;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.i18n.Localizer;
import dev.haypacomer.web.security.CurrentUser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ExpiryController {

  private final EstimateExpiry estimateExpiry;
  private final ReadExpiryFromLabel readExpiryFromLabel;
  private final Localizer localizer;

  public ExpiryController(
      EstimateExpiry estimateExpiry, ReadExpiryFromLabel readExpiryFromLabel, Localizer localizer) {
    this.estimateExpiry = estimateExpiry;
    this.readExpiryFromLabel = readExpiryFromLabel;
    this.localizer = localizer;
  }

  @PostMapping(
      path = "/api/v1/households/{householdId}/items/expiry-from-photo",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ProposalResponse fromPhoto(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestPart("photo") MultipartFile photo,
      @RequestParam String food,
      @RequestParam(defaultValue = "SHELF") ZoneKind zone,
      @RequestParam(defaultValue = "false") boolean opened)
      throws IOException {
    String type = photo.getContentType() == null ? "" : photo.getContentType();
    ExpiryProposal proposal =
        readExpiryFromLabel.read(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            food,
            zone,
            opened,
            new RecipePhoto(type, photo.getBytes()));
    return new ProposalResponse(
        proposal.food(),
        proposal.productOnLabel(),
        proposal.printedDate(),
        proposal.expiresOn(),
        proposal.source(),
        proposal.confidence(),
        localizer.message(proposal.reason()),
        false);
  }

  @GetMapping("/api/v1/households/{householdId}/expiry-estimate")
  EstimateResponse estimate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam String food,
      @RequestParam(defaultValue = "SHELF") ZoneKind zone,
      @RequestParam(defaultValue = "false") boolean opened) {
    ExpiryEstimate estimate =
        estimateExpiry.estimate(
            CurrentUser.of(jwt), new HouseholdId(householdId), food, zone, opened);
    return new EstimateResponse(
        estimate.date(), estimate.source(), estimate.confidence(), estimate.shelfDays());
  }

  record ProposalResponse(
      String food,
      String productOnLabel,
      LocalDate printedDate,
      LocalDate expiresOn,
      ExpirySource source,
      double confidence,
      String reason,
      boolean saved) {}

  record EstimateResponse(
      LocalDate expiresOn, ExpirySource source, double confidence, int shelfDays) {}
}
