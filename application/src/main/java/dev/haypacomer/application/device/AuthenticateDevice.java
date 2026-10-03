package dev.haypacomer.application.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.domain.device.Device;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class AuthenticateDevice {

  private static final Duration LAST_SEEN_RESOLUTION = Duration.ofMinutes(1);

  private final DeviceRepository devices;
  private final DeviceKeys keys;
  private final Clock clock;

  public AuthenticateDevice(DeviceRepository devices, OpaqueTokens opaqueTokens, Clock clock) {
    this.devices = Objects.requireNonNull(devices, "devices");
    this.keys = new DeviceKeys(opaqueTokens);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Device authenticate(String rawKey) {
    if (rawKey == null || !rawKey.startsWith(DeviceKeys.PREFIX)) {
      throw new InvalidDeviceKeyException();
    }
    Device device =
        devices
            .findByKeyHash(keys.hash(rawKey))
            .filter(Device::isActive)
            .orElseThrow(InvalidDeviceKeyException::new);
    Instant now = clock.instant();
    boolean stale =
        device.lastSeen().map(seen -> seen.plus(LAST_SEEN_RESOLUTION).isBefore(now)).orElse(true);
    if (!stale) {
      return device;
    }
    Device seen = device.seenAt(now);
    devices.save(seen);
    return seen;
  }
}
