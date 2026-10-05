package dev.haypacomer.domain.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WeighingTargetTest {

  private final WeighingTarget chicken = WeighingTarget.of(" Chicken breast ", Grams.of(200));

  @Test
  void reportsShortOnTargetAndOver() {
    WeighingProgress short80 = chicken.evaluate(Grams.of(80));
    assertEquals(WeighingStatus.SHORT, short80.status());
    assertEquals(Grams.of(120), short80.remaining());
    assertEquals(40, short80.percent());
    assertEquals("Chicken breast", short80.food());

    assertEquals(WeighingStatus.ON_TARGET, chicken.evaluate(Grams.of(194)).status());
    assertEquals(WeighingStatus.ON_TARGET, chicken.evaluate(Grams.of(206)).status());
    assertEquals(WeighingStatus.SHORT, chicken.evaluate(Grams.of(193)).status());
    WeighingProgress over = chicken.evaluate(Grams.of(250));
    assertEquals(WeighingStatus.OVER, over.status());
    assertEquals(Grams.ZERO, over.remaining());
    assertEquals(125, over.percent());
  }

  @Test
  void rejectsMeaninglessTargets() {
    assertThrows(IllegalArgumentException.class, () -> WeighingTarget.of(" ", Grams.of(1)));
    assertThrows(IllegalArgumentException.class, () -> WeighingTarget.of("Rice", Grams.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeighingTarget("Rice", Grams.of(1), BigDecimal.ONE));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeighingTarget("Rice", Grams.of(1), new BigDecimal("-0.1")));
  }
}
