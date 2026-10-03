package dev.haypacomer.application.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private final Map<HouseholdId, Household> householdStore = new HashMap<>();
  private final Map<HouseholdId, List<Fridge>> fridgeStore = new HashMap<>();
  private final Map<DeviceId, Device> deviceStore = new HashMap<>();
  private final OpaqueTokens opaqueTokens = new OpaqueTokens();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private Fridge fridge;
  private int deviceSaves;

  private final HouseholdRepository households =
      new HouseholdRepository() {
        @Override
        public void save(Household value) {
          householdStore.put(value.id(), value);
        }

        @Override
        public Optional<Household> findById(HouseholdId id) {
          return Optional.ofNullable(householdStore.get(id));
        }

        @Override
        public List<Household> findByUser(UserId user) {
          return List.of();
        }
      };

  private final FridgeRepository fridges =
      new FridgeRepository() {
        @Override
        public void save(HouseholdId owner, Fridge value) {
          fridgeStore.computeIfAbsent(owner, key -> new ArrayList<>()).add(value);
        }

        @Override
        public Optional<Fridge> findById(FridgeId id) {
          return Optional.empty();
        }

        @Override
        public List<Fridge> findByHousehold(HouseholdId owner) {
          return fridgeStore.getOrDefault(owner, List.of());
        }
      };

  private final DeviceRepository devices =
      new DeviceRepository() {
        @Override
        public void save(Device device) {
          deviceSaves++;
          deviceStore.put(device.id(), device);
        }

        @Override
        public Optional<Device> findById(DeviceId id) {
          return Optional.ofNullable(deviceStore.get(id));
        }

        @Override
        public Optional<Device> findByKeyHash(String apiKeyHash) {
          return deviceStore.values().stream()
              .filter(device -> device.apiKeyHash().equals(apiKeyHash))
              .findFirst();
        }

        @Override
        public List<Device> findByHousehold(HouseholdId owner) {
          return deviceStore.values().stream()
              .filter(device -> device.household().equals(owner))
              .toList();
        }
      };

  @BeforeEach
  void createHousehold() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    fridge = Fridge.named("Kitchen fridge");
    fridges.save(household.id(), fridge);
  }

  private RegisterDevice registerDevice() {
    return new RegisterDevice(households, fridges, devices, opaqueTokens, clock);
  }

  private AuthenticateDevice authenticate(Clock at) {
    return new AuthenticateDevice(devices, opaqueTokens, at);
  }

  @Test
  void ownerRegistersADeviceAndGetsTheKeyOnce() {
    RegisteredDevice registered =
        registerDevice()
            .register(juan, household.id(), fridge.id(), "Door", DeviceKind.ESP32_DOOR_TEMP);

    assertTrue(registered.apiKey().startsWith("hpc_dev_"));
    assertFalse(registered.device().apiKeyHash().contains(registered.apiKey()));
    assertFalse(registered.toString().contains(registered.apiKey()));
    assertEquals(
        List.of(registered.device()),
        new ListDevices(households, devices).list(ana, household.id()));
  }

  @Test
  void onlyDeviceManagersRegisterForTheirOwnFridges() {
    assertThrows(
        AccessDeniedException.class,
        () ->
            registerDevice()
                .register(ana, household.id(), fridge.id(), "Door", DeviceKind.ESP32_SCALE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            registerDevice()
                .register(juan, household.id(), FridgeId.newId(), "Door", DeviceKind.ESP32_SCALE));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> new ListDevices(households, devices).list(UserId.newId(), household.id()));
  }

  @Test
  void authenticatesByKeyAndThrottlesLastSeenWrites() {
    RegisteredDevice registered =
        registerDevice()
            .register(juan, household.id(), fridge.id(), "Scale", DeviceKind.ESP32_SCALE);
    int savesAfterRegister = deviceSaves;

    Device first = authenticate(clock).authenticate(registered.apiKey());
    authenticate(Clock.offset(clock, Duration.ofSeconds(30))).authenticate(registered.apiKey());
    authenticate(Clock.offset(clock, Duration.ofMinutes(2))).authenticate(registered.apiKey());

    assertEquals(NOW, first.lastSeen().orElseThrow());
    assertEquals(savesAfterRegister + 2, deviceSaves);
  }

  @Test
  void rejectsUnknownMalformedAndRevokedKeys() {
    RegisteredDevice registered =
        registerDevice()
            .register(juan, household.id(), fridge.id(), "Door", DeviceKind.ESP32_DOOR_TEMP);

    assertThrows(InvalidDeviceKeyException.class, () -> authenticate(clock).authenticate(null));
    assertThrows(InvalidDeviceKeyException.class, () -> authenticate(clock).authenticate("abc"));
    assertThrows(
        InvalidDeviceKeyException.class, () -> authenticate(clock).authenticate("hpc_dev_nope"));

    assertThrows(
        AccessDeniedException.class,
        () ->
            new RevokeDevice(households, devices, clock)
                .revoke(ana, household.id(), registered.device().id()));
    new RevokeDevice(households, devices, clock)
        .revoke(juan, household.id(), registered.device().id());

    assertThrows(
        InvalidDeviceKeyException.class,
        () -> authenticate(clock).authenticate(registered.apiKey()));
    assertThrows(
        DeviceNotFoundException.class,
        () ->
            new RevokeDevice(households, devices, clock)
                .revoke(juan, household.id(), DeviceId.newId()));
  }
}
