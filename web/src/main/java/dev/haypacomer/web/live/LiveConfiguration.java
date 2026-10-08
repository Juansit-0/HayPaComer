package dev.haypacomer.web.live;

import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.live.ViewFridgeTwin;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveConfiguration {

  @Bean
  BroadcastLiveUpdate broadcastLiveUpdate(LiveStreamHub hub) {
    return new BroadcastLiveUpdate(List.of(hub));
  }

  @Bean
  ViewFridgeTwin viewFridgeTwin(
      HouseholdRepository households, FridgeRepository fridges, FridgeMonitorRegistry monitors) {
    return new ViewFridgeTwin(households, fridges, monitors);
  }
}
