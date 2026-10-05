package dev.haypacomer.application.sensor;

import dev.haypacomer.application.sensor.validation.Verdict;
import dev.haypacomer.domain.sensor.Finding;
import java.util.List;

public record IngestionReport(List<EventResult> events, List<Finding> findings) {

  public IngestionReport {
    events = List.copyOf(events);
    findings = List.copyOf(findings);
  }

  public long count(Verdict verdict) {
    return events.stream().filter(event -> event.verdict() == verdict).count();
  }

  public boolean allRejected() {
    return !events.isEmpty() && count(Verdict.REJECTED) == events.size();
  }

  public boolean allDuplicates() {
    return !events.isEmpty() && count(Verdict.DUPLICATE) == events.size();
  }
}
