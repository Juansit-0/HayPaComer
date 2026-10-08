package dev.haypacomer.application.session;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.port.StepTimerStore;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class GuidedCookingMediator implements KitchenMediator {

  private final ScaleSessionStore scales;
  private final StepTimerStore timers;
  private final DeviceRepository devices;
  private final HardwareFactories hardware;
  private final Map<CookingSessionId, DeviceId> scaleOfSession = new ConcurrentHashMap<>();

  public GuidedCookingMediator(
      ScaleSessionStore scales,
      StepTimerStore timers,
      DeviceRepository devices,
      HardwareFactories hardware) {
    this.scales = Objects.requireNonNull(scales, "scales");
    this.timers = Objects.requireNonNull(timers, "timers");
    this.devices = Objects.requireNonNull(devices, "devices");
    this.hardware = Objects.requireNonNull(hardware, "hardware");
  }

  @Override
  public void notify(KitchenEvent event) {
    switch (event) {
      case KitchenEvent.SessionChanged changed -> sessionChanged(changed.session(), changed.at());
      case KitchenEvent.StepWeighed weighed -> stepWeighed(weighed.session(), weighed.progress());
      case KitchenEvent.ClockTicked ticked -> clockTicked(ticked.at());
    }
  }

  private void sessionChanged(CookingSession session, Instant at) {
    session.scale().ifPresent(scale -> scaleOfSession.put(session.id(), scale));
    switch (session.phase()) {
      case PREPARING -> {}
      case COOKING -> cookStep(session, session.step().orElseThrow(), at);
      case PAUSED -> timers.find(session.id()).ifPresent(timer -> timers.save(timer.pause(at)));
      case FINISHED, ABANDONED -> close(session);
    }
  }

  private void cookStep(CookingSession session, RecipeStep step, Instant at) {
    session
        .scale()
        .ifPresent(
            scale ->
                step.weighingTarget()
                    .ifPresent(
                        weighing ->
                            scales.cook(
                                scale,
                                WeighingTarget.of(weighing.food().name(), weighing.target()))));
    Optional<StepTimer> current = timers.find(session.id());
    if (current.isPresent() && current.get().step() == step.position()) {
      timers.save(current.get().resume(at));
      return;
    }
    current.ifPresent(timer -> timers.remove(session.id()));
    step.timerDuration()
        .ifPresent(
            duration -> timers.save(StepTimer.start(session.id(), step.position(), duration, at)));
  }

  private void stepWeighed(CookingSession session, WeighingProgress progress) {
    if (progress.status() == WeighingStatus.ON_TARGET) {
      session.scale().ifPresent(scale -> signal(scale, AlertPattern.WEIGHT_CONFIRMED_BLINK));
    }
  }

  private void signal(DeviceId scale, AlertPattern pattern) {
    devices
        .findById(scale)
        .ifPresent(device -> hardware.forDevice(device).alerts().signal(device.id(), pattern));
  }

  private void close(CookingSession session) {
    timers.remove(session.id());
    session.scale().ifPresent(scales::fridge);
    scaleOfSession.remove(session.id());
  }

  private void clockTicked(Instant at) {
    for (StepTimer timer : timers.all()) {
      if (timer.signaled() || !timer.due(at)) {
        continue;
      }
      timers.save(timer.markSignaled());
      Optional.ofNullable(scaleOfSession.get(timer.session()))
          .ifPresent(scale -> signal(scale, AlertPattern.TIMER_DONE_BEEP));
    }
  }
}
