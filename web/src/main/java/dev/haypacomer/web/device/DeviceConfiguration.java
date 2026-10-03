package dev.haypacomer.web.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.sensors.hardware.Esp32HardwareFactory;
import dev.haypacomer.sensors.hardware.SimulatedHardwareFactory;
import java.time.Clock;
import java.util.List;
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
  HardwareFactories hardwareFactories(Clock clock) {
    return new HardwareFactories(
        List.of(new Esp32HardwareFactory(), new SimulatedHardwareFactory(clock)));
  }

  @Bean
  AuthenticateDevice authenticateDevice(
      DeviceRepository devices, OpaqueTokens opaqueTokens, Clock clock) {
    return new AuthenticateDevice(devices, opaqueTokens, clock);
  }
}
