package dev.haypacomer.web.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceConfiguration {

  @Bean
  RegisterDevice registerDevice(
      HouseholdRepository households,
      FridgeRepository fridges,
      DeviceRepository devices,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    return new RegisterDevice(households, fridges, devices, opaqueTokens, clock);
  }

  @Bean
  ListDevices listDevices(HouseholdRepository households, DeviceRepository devices) {
    return new ListDevices(households, devices);
  }

  @Bean
  RevokeDevice revokeDevice(HouseholdRepository households, DeviceRepository devices, Clock clock) {
    return new RevokeDevice(households, devices, clock);
  }

  @Bean
  AuthenticateDevice authenticateDevice(
      DeviceRepository devices, OpaqueTokens opaqueTokens, Clock clock) {
    return new AuthenticateDevice(devices, opaqueTokens, clock);
  }
}
