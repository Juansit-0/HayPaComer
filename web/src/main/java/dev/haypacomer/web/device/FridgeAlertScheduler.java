package dev.haypacomer.web.device;

import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FridgeAlertScheduler {

  private final CheckFridgeAlerts checkFridgeAlerts;

  public FridgeAlertScheduler(CheckFridgeAlerts checkFridgeAlerts) {
    this.checkFridgeAlerts = checkFridgeAlerts;
  }

  @Scheduled(fixedDelayString = "${haypacomer.sensors.alert-check:PT5S}")
  void check() {
    checkFridgeAlerts.check();
  }
}
