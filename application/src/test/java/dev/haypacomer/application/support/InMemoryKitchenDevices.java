package dev.haypacomer.application.support;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.application.port.StepTimerStore;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryKitchenDevices {

  public final Map<DeviceId, Device> deviceMap = new HashMap<>();
  public final Map<DeviceId, WeighingTarget> cookingTargets = new HashMap<>();
  public final Map<CookingSessionId, StepTimer> timerMap = new LinkedHashMap<>();
  public final List<String> signals = new ArrayList<>();

  public final DeviceRepository devices =
      new DeviceRepository() {
        @Override
        public void save(Device device) {
          deviceMap.put(device.id(), device);
        }

        @Override
        public Optional<Device> findById(DeviceId id) {
          return Optional.ofNullable(deviceMap.get(id));
        }

        @Override
        public Optional<Device> findByKeyHash(String apiKeyHash) {
          return Optional.empty();
        }

        @Override
        public List<Device> findByHousehold(HouseholdId household) {
          return deviceMap.values().stream()
              .filter(device -> device.household().equals(household))
              .toList();
        }
      };

  public final ScaleSessionStore scales =
      new ScaleSessionStore() {
        @Override
        public ScaleMode mode(DeviceId scale) {
          return cookingTargets.containsKey(scale) ? ScaleMode.COOKING : ScaleMode.FRIDGE;
        }

        @Override
        public Optional<WeighingTarget> target(DeviceId scale) {
          return Optional.ofNullable(cookingTargets.get(scale));
        }

        @Override
        public void cook(DeviceId scale, WeighingTarget target) {
          cookingTargets.put(scale, target);
        }

        @Override
        public void fridge(DeviceId scale) {
          cookingTargets.remove(scale);
        }
      };

  public final StepTimerStore timers =
      new StepTimerStore() {
        @Override
        public void save(StepTimer timer) {
          timerMap.put(timer.session(), timer);
        }

        @Override
        public Optional<StepTimer> find(CookingSessionId session) {
          return Optional.ofNullable(timerMap.get(session));
        }

        @Override
        public void remove(CookingSessionId session) {
          timerMap.remove(session);
        }

        @Override
        public List<StepTimer> all() {
          return List.copyOf(timerMap.values());
        }
      };

  private final AlertSignal alerts =
      new AlertSignal() {
        @Override
        public void signal(DeviceId device, AlertPattern pattern) {
          signals.add(deviceMap.get(device).name() + ":" + pattern);
        }

        @Override
        public List<AlertPattern> drain(DeviceId device) {
          return List.of();
        }
      };

  public final HardwareFactories hardware =
      new HardwareFactories(
          List.of(
              new HardwareFactory() {
                @Override
                public boolean supports(DeviceKind kind) {
                  return true;
                }

                @Override
                public SensorEventDecoder decoder() {
                  throw new UnsupportedOperationException();
                }

                @Override
                public AlertSignal alerts() {
                  return alerts;
                }
              }));
}
