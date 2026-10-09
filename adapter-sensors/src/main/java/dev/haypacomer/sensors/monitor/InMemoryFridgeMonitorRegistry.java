package dev.haypacomer.sensors.monitor;

import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FindingKind;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class InMemoryFridgeMonitorRegistry implements FridgeMonitorRegistry {

  private record Tuned(FridgeMonitor monitor, FridgeThresholds thresholds) {}

  private final Function<FridgeId, FridgeThresholds> thresholds;
  private final Map<FridgeId, Tuned> monitors = new ConcurrentHashMap<>();
  private final Map<FridgeId, DeviceId> devices = new ConcurrentHashMap<>();
  private final Set<Episode> reported = ConcurrentHashMap.newKeySet();

  public InMemoryFridgeMonitorRegistry(FridgeThresholds thresholds) {
    this(fridge -> thresholds);
  }

  public InMemoryFridgeMonitorRegistry(Function<FridgeId, FridgeThresholds> thresholds) {
    this.thresholds = thresholds;
  }

  @Override
  public FridgeMonitor monitor(FridgeId fridge) {
    FridgeThresholds current = thresholds.apply(fridge);
    return monitors
        .compute(
            fridge,
            (id, tuned) ->
                tuned != null && tuned.thresholds().equals(current)
                    ? tuned
                    : new Tuned(new FridgeMonitor(id, current), current))
        .monitor();
  }

  @Override
  public void rememberDevice(FridgeId fridge, DeviceId device) {
    devices.put(fridge, device);
  }

  @Override
  public Optional<DeviceId> lastDevice(FridgeId fridge) {
    return Optional.ofNullable(devices.get(fridge));
  }

  @Override
  public Collection<FridgeId> fridges() {
    return List.copyOf(monitors.keySet());
  }

  @Override
  public boolean firstReport(FridgeId fridge, Finding finding) {
    return reported.add(new Episode(fridge, finding.kind(), finding.since()));
  }

  private record Episode(FridgeId fridge, FindingKind kind, Instant since) {}
}
