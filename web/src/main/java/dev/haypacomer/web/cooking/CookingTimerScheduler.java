package dev.haypacomer.web.cooking;

import dev.haypacomer.application.session.CheckCookingTimers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CookingTimerScheduler {

  private final CheckCookingTimers checkCookingTimers;

  public CookingTimerScheduler(CheckCookingTimers checkCookingTimers) {
    this.checkCookingTimers = checkCookingTimers;
  }

  @Scheduled(fixedDelayString = "${haypacomer.cooking.timer-check:PT1S}")
  void check() {
    checkCookingTimers.check();
  }
}
