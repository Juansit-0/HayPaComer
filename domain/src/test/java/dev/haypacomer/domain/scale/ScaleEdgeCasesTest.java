package dev.haypacomer.domain.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.Grams;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScaleEdgeCasesTest {

  private static final Instant T0 = Instant.parse("2026-10-05T12:00:00Z");

  @Test
  void aReversedLoadCellStillReadsPositiveGrams() {
    ScaleCalibration reversed =
        ScaleCalibration.taredAt(new RawSample(500_000, T0))
            .calibrate(new RawSample(286_000, T0), Grams.of(500));

    assertEquals(Grams.of(842), reversed.toGrams(500_000 - 428L * 842).orElseThrow());
    assertEquals(Grams.ZERO, reversed.toGrams(510_000).orElseThrow());
  }

  @Test
  void nonIntegerFactorsRoundToHundredthsOfAGram() {
    ScaleCalibration calibration =
        ScaleCalibration.taredAt(new RawSample(0, T0))
            .calibrate(new RawSample(213_975, T0), Grams.of(500));

    assertEquals(Grams.of("427.95"), Grams.of(calibration.countsPerGram()));
    assertEquals(Grams.of(650), calibration.toGrams(Math.round(427.95 * 650)).orElseThrow());
  }

  @Test
  void recalibrationKeepsTheTareAndReplacesTheFactor() {
    ScaleCalibration first =
        ScaleCalibration.taredAt(new RawSample(1_000, T0))
            .calibrate(new RawSample(429_000, T0), Grams.of(1000));
    ScaleCalibration second =
        first.calibrate(new RawSample(215_000, T0.plusSeconds(60)), Grams.of(500));

    assertEquals(first.offsetCounts(), second.offsetCounts());
    assertEquals(Grams.of(500), second.toGrams(215_000).orElseThrow());
    assertEquals(T0.plusSeconds(60), second.calibratedAt());
  }

  @Test
  void smallNoiseIsStableButVibrationIsNot() {
    StabilityDetector detector = StabilityDetector.standard();
    long[] noisy = {650, 651, 649, 650, 651, 650, 649, 650, 651, 650, 650};
    boolean stable = false;
    for (int index = 0; index < noisy.length; index++) {
      stable = detector.offer(Grams.of(noisy[index]), T0.plusMillis(index * 100L));
    }
    assertTrue(stable);

    StabilityDetector shaken = StabilityDetector.standard();
    boolean everStable = false;
    for (int index = 0; index < 30; index++) {
      long grams = index % 3 == 0 ? 662 : 650;
      everStable |= shaken.offer(Grams.of(grams), T0.plusMillis(index * 100L));
    }
    assertFalse(everStable);
  }

  @Test
  void stabilityIsLostAndRegainedAfterARemoval() {
    StabilityDetector detector = StabilityDetector.standard();
    for (int index = 0; index <= 12; index++) {
      detector.offer(Grams.of(842), T0.plusMillis(index * 100L));
    }

    assertFalse(detector.offer(Grams.of(650), T0.plusMillis(1300)));
    assertFalse(detector.offer(Grams.of(650), T0.plusMillis(2200)));
    assertTrue(detector.offer(Grams.of(650), T0.plusMillis(2300)));
    assertTrue(detector.offer(Grams.of(650), T0.plus(Duration.ofSeconds(10))));
  }
}
