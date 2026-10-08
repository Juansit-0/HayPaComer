package dev.haypacomer.web.live;

import dev.haypacomer.application.live.LiveUpdate;
import dev.haypacomer.application.port.LiveUpdateListener;
import dev.haypacomer.domain.household.HouseholdId;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class LiveStreamHub implements LiveUpdateListener {

  static final Duration TIMEOUT = Duration.ofMinutes(30);

  private final Map<HouseholdId, Set<SseEmitter>> streams = new ConcurrentHashMap<>();

  SseEmitter open(HouseholdId household) {
    SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
    Set<SseEmitter> listeners =
        streams.computeIfAbsent(household, id -> ConcurrentHashMap.newKeySet());
    listeners.add(emitter);
    Runnable remove = () -> listeners.remove(emitter);
    emitter.onCompletion(remove);
    emitter.onTimeout(remove);
    emitter.onError(error -> remove.run());
    send(emitter, SseEmitter.event().name("ready").data("{}", MediaType.APPLICATION_JSON));
    return emitter;
  }

  int listeners(HouseholdId household) {
    return streams.getOrDefault(household, Set.of()).size();
  }

  @Override
  public void onUpdate(LiveUpdate update) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("kind", update.kind().name());
    data.put("fridgeId", update.fridge());
    data.put("detail", update.detail());
    data.put("at", update.at().toString());
    streams
        .getOrDefault(update.household(), Set.of())
        .forEach(
            emitter ->
                send(
                    emitter,
                    SseEmitter.event()
                        .name(update.kind().name().toLowerCase())
                        .data(data, MediaType.APPLICATION_JSON)));
  }

  @Scheduled(fixedDelayString = "${haypacomer.live.heartbeat:PT25S}")
  void heartbeat() {
    streams
        .values()
        .forEach(set -> set.forEach(emitter -> send(emitter, SseEmitter.event().comment("ping"))));
  }

  private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
    try {
      emitter.send(event);
    } catch (IOException | IllegalStateException closed) {
      emitter.completeWithError(closed);
    }
  }
}
