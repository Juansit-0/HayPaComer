package dev.haypacomer.domain.scale;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class StabilityDetector {

  public static final Duration DEFAULT_WINDOW = Duration.ofSeconds(1);
  public static final Grams DEFAULT_TOLERANCE = Grams.of(2);

  private final Duration window;
  private final Grams tolerance;
  private final Deque<Point> points = new ArrayDeque<>();

  public StabilityDetector(Duration window, Grams tolerance) {
    this.window = Objects.requireNonNull(window, "window");
    this.tolerance = Objects.requireNonNull(tolerance, "tolerance");
  }

  public static StabilityDetector standard() {
    return new StabilityDetector(DEFAULT_WINDOW, DEFAULT_TOLERANCE);
  }

  public synchronized boolean offer(Grams grams, Instant at) {
    if (!points.isEmpty() && at.isBefore(points.peekLast().at())) {
      return false;
    }
    points.addLast(new Point(grams.value(), at));
    while (points.size() > 1
        && Duration.between(points.peekFirst().at(), at).compareTo(window.multipliedBy(3)) > 0) {
      points.removeFirst();
    }
    BigDecimal latest = grams.value();
    Instant stableSince = at;
    for (var iterator = points.descendingIterator(); iterator.hasNext(); ) {
      Point point = iterator.next();
      if (point.grams().subtract(latest).abs().compareTo(tolerance.value()) > 0) {
        break;
      }
      stableSince = point.at();
    }
    return Duration.between(stableSince, at).compareTo(window) >= 0;
  }

  private record Point(BigDecimal grams, Instant at) {}
}
