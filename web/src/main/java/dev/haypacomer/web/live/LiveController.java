package dev.haypacomer.web.live;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.live.FridgeTwin;
import dev.haypacomer.application.live.ViewFridgeTwin;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class LiveController {

  private final GetHousehold getHousehold;
  private final ViewFridgeTwin viewFridgeTwin;
  private final LiveStreamHub hub;

  public LiveController(
      GetHousehold getHousehold, ViewFridgeTwin viewFridgeTwin, LiveStreamHub hub) {
    this.getHousehold = getHousehold;
    this.viewFridgeTwin = viewFridgeTwin;
    this.hub = hub;
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter stream(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    HouseholdId household = new HouseholdId(householdId);
    getHousehold.get(CurrentUser.of(jwt), household);
    return hub.open(household);
  }

  @GetMapping("/fridges/{fridgeId}/twin")
  TwinResponse twin(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID fridgeId) {
    return TwinResponse.from(
        viewFridgeTwin.view(
            CurrentUser.of(jwt), new HouseholdId(householdId), new FridgeId(fridgeId)));
  }

  record TwinResponse(
      UUID fridgeId, Boolean doorOpen, Instant doorSince, BigDecimal celsius, Instant measuredAt) {

    static TwinResponse from(FridgeTwin twin) {
      return new TwinResponse(
          twin.fridge().value(),
          twin.doorOpen(),
          twin.doorSince(),
          twin.celsius(),
          twin.measuredAt());
    }
  }
}
