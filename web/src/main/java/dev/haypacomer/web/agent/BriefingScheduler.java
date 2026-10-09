package dev.haypacomer.web.agent;

import dev.haypacomer.agent.proactive.ScheduledBriefings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BriefingScheduler {

  private final ScheduledBriefings briefings;

  public BriefingScheduler(ScheduledBriefings briefings) {
    this.briefings = briefings;
  }

  @Scheduled(cron = "${haypacomer.agent.briefings-cron:0 0 * * * *}")
  void run() {
    briefings.run();
  }
}
