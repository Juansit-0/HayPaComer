package dev.haypacomer.sensors.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class HardwareFamiliesTest {

  private static final Instant NOW = Instant.parse("2026-10-03T18:00:00Z");

  private final HardwareFactories factories =
      new HardwareFactories(
          List.of(
              new Esp32HardwareFactory(),
              new SimulatedHardwareFactory(Clock.fixed(NOW, ZoneOffset.UTC))));

  private static Device device(DeviceKind kind) {
    return Device.register(HouseholdId.newId(), FridgeId.newId(), "Device", kind, "ab", NOW);
  }

  @Test
  void eachDeviceKindGetsItsOwnHardwareFamily() {
    HardwareFactory real = factories.forDevice(device(DeviceKind.ESP32_DOOR_TEMP));
    HardwareFactory simulated = factories.forDevice(device(DeviceKind.SIMULATOR));

    assertInstanceOf(Esp32HardwareFactory.class, real);
    assertInstanceOf(SimulatedHardwareFactory.class, simulated);
    assertSame(real, factories.forDevice(device(DeviceKind.ESP32_SCALE)));
    assertNotSame(real.alerts(), simulated.alerts());
  }

  @Test
  void realHardwareRequiresCompleteEnvelopes() {
    Device esp32 = device(DeviceKind.ESP32_DOOR_TEMP);

    assertThrows(
        MalformedSensorPayloadException.class,
        () ->
            factories
                .forDevice(esp32)
                .decoder()
                .decode(esp32, "{\"type\":\"DOOR\",\"door\":\"OPEN\"}"));
  }

  @Test
  void simulatedHardwareFillsIdentityAndTime() {
    Device simulator = device(DeviceKind.SIMULATOR);

    List<SensorEvent> events =
        factories
            .forDevice(simulator)
            .decoder()
            .decode(
                simulator,
                "[{\"type\":\"DOOR\",\"door\":\"OPEN\"},{\"type\":\"DOOR\",\"door\":\"CLOSED\",\"at\":\"2026-10-03T18:00:40Z\"}]");

    assertEquals(NOW, events.getFirst().occurredAt());
    assertEquals(Instant.parse("2026-10-03T18:00:40Z"), events.get(1).occurredAt());
    assertInstanceOf(DoorEvent.class, events.getFirst());
    assertThrows(
        MalformedSensorPayloadException.class,
        () -> factories.forDevice(simulator).decoder().decode(simulator, "nope"));
    assertEquals(
        1,
        factories
            .forDevice(simulator)
            .decoder()
            .decode(simulator, "{\"type\":\"TEMPERATURE\",\"tempC\":4}")
            .size());
  }

  @Test
  void alertsQueueUntilTheDevicePullsThem() {
    QueuedAlertSignal queue = new QueuedAlertSignal();
    DeviceId door = DeviceId.newId();

    for (int index = 0; index < 25; index++) {
      queue.signal(door, AlertPattern.DOOR_OPEN_BEEP);
    }
    queue.signal(door, AlertPattern.COLD_CHAIN_ALARM);

    List<AlertPattern> drained = queue.drain(door);
    assertEquals(20, drained.size());
    assertEquals(AlertPattern.COLD_CHAIN_ALARM, drained.getLast());
    assertTrue(queue.drain(door).isEmpty());
    assertTrue(queue.drain(DeviceId.newId()).isEmpty());

    RecordingAlertSignal recording = new RecordingAlertSignal();
    recording.signal(door, AlertPattern.WEIGHT_CONFIRMED_BLINK);
    assertEquals(List.of(AlertPattern.WEIGHT_CONFIRMED_BLINK), recording.drain(door));
  }
}
