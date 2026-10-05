package dev.haypacomer.web.device;

import dev.haypacomer.application.scale.CalibrateScale;
import dev.haypacomer.application.scale.ReadScale;
import dev.haypacomer.application.scale.ScaleReading;
import dev.haypacomer.application.scale.TareScale;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/devices/{deviceId}/scale")
public class ScaleController {

  private final TareScale tareScale;
  private final CalibrateScale calibrateScale;
  private final ReadScale readScale;

  public ScaleController(TareScale tareScale, CalibrateScale calibrateScale, ReadScale readScale) {
    this.tareScale = tareScale;
    this.calibrateScale = calibrateScale;
    this.readScale = readScale;
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
