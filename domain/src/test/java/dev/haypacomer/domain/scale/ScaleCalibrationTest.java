package dev.haypacomer.domain.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScaleCalibrationTest {

  private static final Instant T0 = Instant.parse("2026-10-05T12:00:00Z");

  @Test
  void tareThenCalibrateWithAKnownWeight() {
    ScaleCalibration tared = ScaleCalibration.taredAt(new RawSample(84_000, T0));

    assertFalse(tared.isCalibrated());
    assertTrue(tared.toGrams(90_000).isEmpty());

    ScaleCalibration calibrated =
        tared.calibrate(new RawSample(298_000, T0.plusSeconds(10)), Grams.of(500));

    assertTrue(calibrated.isCalibrated());
    assertEquals(Grams.of(500), calibrated.toGrams(298_000).orElseThrow());
    assertEquals(Grams.of(842), calibrated.toGrams(84_000 + 428 * 842).orElseThrow());
    assertEquals(Grams.ZERO, calibrated.toGrams(80_000).orElseThrow());
  }

  @Test
  void retareKeepsTheFactorAndDiscountsAContainer() {
    ScaleCalibration calibrated =
        ScaleCalibration.taredAt(new RawSample(0, T0))
            .calibrate(new RawSample(428_000, T0), Grams.of(1000));

    ScaleCalibration withBowl = calibrated.tare(new RawSample(42_800, T0.plusSeconds(60)));

    assertEquals(Grams.of(130), withBowl.toGrams(42_800 + 428 * 130).orElseThrow());
    assertEquals(T0.plusSeconds(60), withBowl.taredAt());
  }

  @Test
  void rejectsMeaninglessCalibrations() {
    ScaleCalibration tared = ScaleCalibration.taredAt(new RawSample(1_000, T0));

    assertThrows(
        IllegalArgumentException.class,
        () -> tared.calibrate(new RawSample(1_000, T0), Grams.of(500)));
    assertThrows(
        IllegalArgumentException.class,
        () -> tared.calibrate(new RawSample(5_000, T0), Grams.ZERO));
    assertThrows(
        IllegalArgumentException.class, () -> new ScaleCalibration(0, BigDecimal.ZERO, T0, T0));
  }

  @Test
  void stabilityNeedsOneSecondWithinTwoGrams() {
    StabilityDetector detector = StabilityDetector.standard();

    assertFalse(detector.offer(Grams.of(700), T0));
    assertFalse(detector.offer(Grams.of(651), T0.plusMillis(300)));
    assertFalse(detector.offer(Grams.of(650), T0.plusMillis(600)));
    assertFalse(detector.offer(Grams.of(651), T0.plusMillis(1200)));
    assertTrue(detector.offer(Grams.of(650), T0.plusMillis(1300)));
    assertFalse(detector.offer(Grams.of(640), T0.plusMillis(1500)));
    assertFalse(detector.offer(Grams.of(640), T0.plusMillis(1000)));
  }
}
