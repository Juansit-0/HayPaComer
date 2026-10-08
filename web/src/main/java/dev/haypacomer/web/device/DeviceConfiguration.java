package dev.haypacomer.web.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.scale.ApplyFridgeScaleReading;
import dev.haypacomer.application.scale.AssignScaleItem;
import dev.haypacomer.application.scale.CalibrateScale;
import dev.haypacomer.application.scale.ReadScale;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.application.scale.SetScaleMode;
import dev.haypacomer.application.scale.TareScale;
import dev.haypacomer.application.scale.UnassignScaleItem;
import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.application.sensor.ObserveSensorEvent;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.sensors.fridge.InMemoryFridgeSessionRegistry;
import dev.haypacomer.sensors.hardware.Esp32HardwareFactory;
import dev.haypacomer.sensors.hardware.SimulatedHardwareFactory;
import dev.haypacomer.sensors.monitor.InMemoryFridgeMonitorRegistry;
import dev.haypacomer.sensors.scale.InMemoryScaleSampleStore;
import dev.haypacomer.sensors.scale.InMemoryScaleSessionStore;
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
  ScaleSampleStore scaleSampleStore() {
    return new InMemoryScaleSampleStore();
  }

  @Bean
  ScaleSessionStore scaleSessionStore() {
    return new InMemoryScaleSessionStore();
  }

  @Bean
  SetScaleMode setScaleMode(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleSessionStore sessions,
      Clock clock) {
    return new SetScaleMode(households, devices, samples, sessions, clock);
  }

  @Bean
  ReadWeighingProgress readWeighingProgress(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      ScaleSessionStore sessions,
      Clock clock) {
    return new ReadWeighingProgress(households, devices, samples, calibrations, sessions, clock);
  }

  @Bean
  HardwareFactories hardwareFactories(
      Clock clock,
      ScaleCalibrationRepository calibrations,
      ScaleSampleStore samples,
      ScaleSessionStore sessions) {
    return new HardwareFactories(
        List.of(
            new Esp32HardwareFactory(calibrations, samples, sessions),
            new SimulatedHardwareFactory(clock, calibrations, samples, sessions)));
  }

  @Bean
  TareScale tareScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    return new TareScale(households, devices, samples, calibrations, clock);
  }

  @Bean
  CalibrateScale calibrateScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    return new CalibrateScale(households, devices, samples, calibrations, clock);
  }

  @Bean
  ReadScale readScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    return new ReadScale(households, devices, samples, calibrations, clock);
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
      NotifyHousehold notifications) {
    return new ObserveSensorEvent(registry, devices, hardware, notifications);
  }

  @Bean
  CheckFridgeAlerts checkFridgeAlerts(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      NotifyHousehold notifications,
      Clock clock) {
    return new CheckFridgeAlerts(registry, devices, hardware, notifications, clock);
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
      ApplyFridgeScaleReading fridgeScale,
      Clock clock) {
    return new IngestSensorEvents(
        hardware, validation, log, observe, coldChain, fridgeScale, clock);
  }

  @Bean
  FridgeSessionRegistry fridgeSessionRegistry() {
    return new InMemoryFridgeSessionRegistry();
  }

  @Bean
  ApplyFridgeScaleReading applyFridgeScaleReading(
      ScaleAssignmentRepository assignments,
      FridgeRepository fridges,
      ExecuteInventoryCommand commands,
      FridgeSessionRegistry sessions) {
    return new ApplyFridgeScaleReading(
        assignments,
        fridges,
        commands,
        sessions,
        Grams.of(FridgeThresholds.DEFAULT.minimumWeightChange()));
  }

  @Bean
  AssignScaleItem assignScaleItem(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      FoodAccessGuard guard,
      ScaleAssignmentRepository assignments,
      Clock clock) {
    return new AssignScaleItem(
        households, devices, samples, fridges, ownerships, guard, assignments, clock);
  }

  @Bean
  UnassignScaleItem unassignScaleItem(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleAssignmentRepository assignments,
      Clock clock) {
    return new UnassignScaleItem(households, devices, samples, assignments, clock);
  }

  @Bean
  AuthenticateDevice authenticateDevice(
      DeviceRepository devices, OpaqueTokens opaqueTokens, Clock clock) {
    return new AuthenticateDevice(devices, opaqueTokens, clock);
  }
}
