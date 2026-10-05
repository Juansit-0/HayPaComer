package dev.haypacomer.web.device;

import dev.haypacomer.application.scale.AssignScaleItem;
import dev.haypacomer.application.scale.CalibrateScale;
import dev.haypacomer.application.scale.ReadScale;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.application.scale.ScaleAssignment;
import dev.haypacomer.application.scale.ScaleReading;
import dev.haypacomer.application.scale.SetScaleMode;
import dev.haypacomer.application.scale.TareScale;
import dev.haypacomer.application.scale.UnassignScaleItem;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/devices/{deviceId}/scale")
public class ScaleController {

  private final TareScale tareScale;
  private final CalibrateScale calibrateScale;
  private final ReadScale readScale;
  private final AssignScaleItem assignScaleItem;
  private final UnassignScaleItem unassignScaleItem;
  private final SetScaleMode setScaleMode;
  private final ReadWeighingProgress readWeighingProgress;

  public ScaleController(
      TareScale tareScale,
      CalibrateScale calibrateScale,
      ReadScale readScale,
      AssignScaleItem assignScaleItem,
      UnassignScaleItem unassignScaleItem,
      SetScaleMode setScaleMode,
      ReadWeighingProgress readWeighingProgress) {
    this.tareScale = tareScale;
    this.calibrateScale = calibrateScale;
    this.readScale = readScale;
    this.assignScaleItem = assignScaleItem;
    this.unassignScaleItem = unassignScaleItem;
    this.setScaleMode = setScaleMode;
    this.readWeighingProgress = readWeighingProgress;
  }

  @PutMapping("/mode")
  ModeResponse mode(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId,
      @Valid @RequestBody ModeRequest request) {
    Optional<WeighingTarget> target =
        request.food() == null || request.targetGrams() == null
            ? Optional.empty()
            : Optional.of(WeighingTarget.of(request.food(), Grams.of(request.targetGrams())));
    return new ModeResponse(
        setScaleMode.set(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new DeviceId(deviceId),
            request.mode(),
            target));
  }

  @GetMapping("/progress")
  WeighingProgress progress(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId) {
    return readWeighingProgress.read(
        CurrentUser.of(jwt), new HouseholdId(householdId), new DeviceId(deviceId));
  }

  @PutMapping("/item")
  AssignmentResponse assign(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId,
      @Valid @RequestBody AssignRequest request) {
    ScaleAssignment assignment =
        assignScaleItem.assign(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new DeviceId(deviceId),
            new FoodItemId(request.itemId()));
    return new AssignmentResponse(assignment.item().value(), assignment.assignedAt());
  }

  @DeleteMapping("/item")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unassign(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId) {
    unassignScaleItem.unassign(
        CurrentUser.of(jwt), new HouseholdId(householdId), new DeviceId(deviceId));
  }

  @PostMapping("/tare")
  CalibrationResponse tare(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId) {
    return CalibrationResponse.from(
        tareScale.tare(CurrentUser.of(jwt), new HouseholdId(householdId), new DeviceId(deviceId)));
  }

  @PostMapping("/calibrate")
  CalibrationResponse calibrate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId,
      @Valid @RequestBody CalibrateRequest request) {
    return CalibrationResponse.from(
        calibrateScale.calibrate(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new DeviceId(deviceId),
            Grams.of(request.knownGrams())));
  }

  @GetMapping("/reading")
  ReadingResponse reading(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId) {
    return ReadingResponse.from(
        readScale.read(CurrentUser.of(jwt), new HouseholdId(householdId), new DeviceId(deviceId)));
  }

  record ModeRequest(
      @NotNull ScaleMode mode,
      String food,
      @DecimalMin(value = "0", inclusive = false) BigDecimal targetGrams) {}

  record ModeResponse(ScaleMode mode) {}

  record AssignRequest(@NotNull UUID itemId) {}

  record AssignmentResponse(UUID itemId, Instant assignedAt) {}

  record CalibrateRequest(
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal knownGrams) {}

  record CalibrationResponse(boolean calibrated, Instant taredAt, Instant calibratedAt) {

    static CalibrationResponse from(ScaleCalibration calibration) {
      return new CalibrationResponse(
          calibration.isCalibrated(), calibration.taredAt(), calibration.calibratedAt());
    }
  }

  record ReadingResponse(long rawCounts, BigDecimal grams, boolean calibrated, Instant at) {

    static ReadingResponse from(ScaleReading reading) {
      return new ReadingResponse(
          reading.rawCounts(),
          reading.weight().map(Grams::value).orElse(null),
          reading.calibrated(),
          reading.at());
    }
  }
}
