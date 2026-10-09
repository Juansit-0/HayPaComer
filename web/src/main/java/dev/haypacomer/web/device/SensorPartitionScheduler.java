package dev.haypacomer.web.device;

import dev.haypacomer.persistence.relational.PostgresSensorPartitions;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SensorPartitionScheduler {

  private final PostgresSensorPartitions partitions;
  private final Clock clock;

  public SensorPartitionScheduler(PostgresSensorPartitions partitions, Clock clock) {
    this.partitions = partitions;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(cron = "${haypacomer.sensors.partition-cron:0 30 3 * * *}")
  void ensureAhead() {
    partitions.ensureAhead(LocalDate.now(clock));
  }
}
