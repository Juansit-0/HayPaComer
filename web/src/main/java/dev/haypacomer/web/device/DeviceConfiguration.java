package dev.haypacomer.web.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.application.sensor.ObserveSensorEvent;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.sensors.hardware.Esp32HardwareFactory;
import dev.haypacomer.sensors.hardware.SimulatedHardwareFactory;
import dev.haypacomer.sensors.monitor.InMemoryFridgeMonitorRegistry;
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
  FridgeMonitorRegistry fridgeMonitorRegistry() {
    return new InMemoryFridgeMonitorRegistry(FridgeThresholds.DEFAULT);
  }

  @Bean
  ObserveSensorEvent observeSensorEvent(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      Clock clock) {
    return new ObserveSensorEvent(registry, devices, hardware, clock);
  }

  @Bean
  CheckFridgeAlerts checkFridgeAlerts(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      Clock clock) {
    return new CheckFridgeAlerts(registry, devices, hardware, clock);
  }

  @Bean
  ValidateSensorEvent validateSensorEvent(SensorEventLog log, Clock clock) {
    return new ValidateSensorEvent(log, clock);
  }

  @Bean
  IngestSensorEvents ingestSensorEvents(
      HardwareFactories hardware,
      ValidateSensorEvent validation,
      SensorEventLog log,
      ObserveSensorEvent observe,
      TrackColdChain coldChain,
      Clock clock) {
    return new IngestSensorEvents(hardware, validation, log, observe, coldChain, clock);
  }

  @Bean
  AuthenticateDevice authenticateDevice(
      DeviceRepository devices, OpaqueTokens opaqueTokens, Clock clock) {
    return new AuthenticateDevice(devices, opaqueTokens, clock);
  }
}
