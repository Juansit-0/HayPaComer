package dev.haypacomer.web.agent;

import dev.haypacomer.agent.proactive.AlertBriefings;
import dev.haypacomer.agent.proactive.Briefer;
import dev.haypacomer.agent.proactive.ScheduledBriefings;
import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.application.analytics.BuildWeeklyDigest;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.port.BriefingLog;
import dev.haypacomer.application.port.HouseholdDirectory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.persistence.redis.RedisBriefingLog;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ProactiveConfiguration {

  @Bean
  BriefingLog briefingLog(StringRedisTemplate redis) {
    return new RedisBriefingLog(redis);
  }

  @Bean(destroyMethod = "shutdown")
  ExecutorService briefingExecutor() {
    return Executors.newSingleThreadExecutor(
        runnable -> Thread.ofPlatform().name("briefings").daemon(true).unstarted(runnable));
  }

  @Bean
  Briefer briefer(
      HouseholdRepository households,
      Supervisor supervisor,
      @Qualifier("briefingDelivery") NotifyHousehold delivery,
      Clock clock) {
    return new Briefer(households, supervisor, delivery, clock);
  }

  @Bean
  AlertBriefings alertBriefings(
      HouseholdRepository households,
      BriefingLog log,
      Briefer briefer,
      @Qualifier("briefingExecutor") ExecutorService executor) {
    return new AlertBriefings(households, log, briefer, executor);
  }

  @Bean
  ScheduledBriefings scheduledBriefings(
      HouseholdDirectory directory,
      HouseholdRepository households,
      ViewInventory inventory,
      BriefingLog log,
      Briefer briefer,
      BuildWeeklyDigest digest,
      Clock clock) {
    return new ScheduledBriefings(directory, households, inventory, log, briefer, digest, clock);
  }
}
