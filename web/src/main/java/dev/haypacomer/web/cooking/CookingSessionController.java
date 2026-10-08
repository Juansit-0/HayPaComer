package dev.haypacomer.web.cooking;

import dev.haypacomer.application.session.AdvanceCookingSession;
import dev.haypacomer.application.session.ResumeCookingSession;
import dev.haypacomer.application.session.SessionAction;
import dev.haypacomer.application.session.StartCookingSession;
import dev.haypacomer.application.session.StepWeighingResult;
import dev.haypacomer.application.session.ViewCookingSession;
import dev.haypacomer.application.session.ViewStepTimer;
import dev.haypacomer.application.session.WeighStep;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.SessionPhase;
import dev.haypacomer.domain.session.StepCompletion;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/cooking-sessions")
public class CookingSessionController {

  private final StartCookingSession startCookingSession;
  private final AdvanceCookingSession advanceCookingSession;
  private final ViewCookingSession viewCookingSession;
  private final ResumeCookingSession resumeCookingSession;
  private final WeighStep weighStep;
  private final ViewStepTimer viewStepTimer;
  private final RecipeAssembler assembler;

  public CookingSessionController(
      StartCookingSession startCookingSession,
      AdvanceCookingSession advanceCookingSession,
      ViewCookingSession viewCookingSession,
      ResumeCookingSession resumeCookingSession,
      WeighStep weighStep,
      ViewStepTimer viewStepTimer,
      RecipeAssembler assembler) {
    this.startCookingSession = startCookingSession;
    this.advanceCookingSession = advanceCookingSession;
    this.viewCookingSession = viewCookingSession;
    this.resumeCookingSession = resumeCookingSession;
    this.weighStep = weighStep;
    this.viewStepTimer = viewStepTimer;
    this.assembler = assembler;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  SessionResponse start(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody StartRequest request) {
    return SessionResponse.from(
        startCookingSession.start(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            assembler.assemble(
                request.name(),
                request.servings(),
                request.minutes(),
                request.requirements(),
                request.steps()),
            request.targetServings(),
            Optional.ofNullable(request.scaleId()).map(DeviceId::new)));
  }

  @GetMapping("/active")
  ResponseEntity<SessionResponse> active(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return resumeCookingSession
        .active(CurrentUser.of(jwt), new HouseholdId(householdId))
        .map(SessionResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping("/{sessionId}")
  SessionResponse view(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return SessionResponse.from(
        viewCookingSession.view(
            CurrentUser.of(jwt), new HouseholdId(householdId), new CookingSessionId(sessionId)));
  }

  @PostMapping("/{sessionId}/next")
  SessionResponse next(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return act(jwt, householdId, sessionId, SessionAction.NEXT);
  }

  @PostMapping("/{sessionId}/pause")
  SessionResponse pause(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return act(jwt, householdId, sessionId, SessionAction.PAUSE);
  }

  @PostMapping("/{sessionId}/resume")
  SessionResponse resume(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return act(jwt, householdId, sessionId, SessionAction.RESUME);
  }

  @PostMapping("/{sessionId}/abandon")
  SessionResponse abandon(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return act(jwt, householdId, sessionId, SessionAction.ABANDON);
  }

  @PostMapping("/{sessionId}/steps/{position}/weigh")
  WeighingResponse weigh(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId,
      @PathVariable int position,
      @Valid @RequestBody(required = false) WeighRequest request) {
    StepWeighingResult result =
        weighStep.weigh(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new CookingSessionId(sessionId),
            position,
            Optional.ofNullable(request).map(WeighRequest::grams).map(Grams::of));
    WeighingProgress progress = result.progress();
    return new WeighingResponse(
        progress.food(),
        progress.measured().value(),
        progress.target().value(),
        progress.remaining().value(),
        progress.percent(),
        progress.status(),
        SessionResponse.from(result.session()));
  }

  @GetMapping("/{sessionId}/timer")
  ResponseEntity<TimerResponse> timer(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID sessionId) {
    return viewStepTimer
        .view(CurrentUser.of(jwt), new HouseholdId(householdId), new CookingSessionId(sessionId))
        .map(
            status ->
                new TimerResponse(
                    status.step(),
                    status.duration().toSeconds(),
                    status.remaining().toSeconds(),
                    status.paused(),
                    status.done()))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  private SessionResponse act(Jwt jwt, UUID householdId, UUID sessionId, SessionAction action) {
    return SessionResponse.from(
        advanceCookingSession.apply(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new CookingSessionId(sessionId),
            action));
  }

  record StartRequest(
      @NotBlank String name,
      @Min(1) int servings,
      @Min(1) int minutes,
      @NotEmpty List<@Valid RequirementRequest> requirements,
      @Size(max = 50) List<@Valid StepRequest> steps,
      @Min(1) int targetServings,
      UUID scaleId) {}

  record WeighRequest(@DecimalMin(value = "0") BigDecimal grams) {}

  record WeighingResponse(
      String food,
      BigDecimal measuredGrams,
      BigDecimal targetGrams,
      BigDecimal remainingGrams,
      int percent,
      WeighingStatus status,
      SessionResponse session) {}

  record TimerResponse(
      int step, long durationSeconds, long remainingSeconds, boolean paused, boolean done) {}

  record StepResponse(
      int position,
      String instruction,
      Long timerSeconds,
      String weighFood,
      BigDecimal weighGrams) {

    static StepResponse from(RecipeStep step) {
      return new StepResponse(
          step.position(),
          step.instruction(),
          step.timerDuration().map(Duration::toSeconds).orElse(null),
          step.weighingTarget().map(weighing -> weighing.food().name()).orElse(null),
          step.weighingTarget().map(weighing -> weighing.target().value()).orElse(null));
    }
  }

  record SessionResponse(
      UUID id,
      String recipe,
      int servings,
      UUID scaleId,
      SessionPhase phase,
      int currentStep,
      int totalSteps,
      StepResponse step,
      List<StepResponse> steps,
      List<Integer> completedSteps,
      BigDecimal measuredGrams,
      Instant startedAt,
      Instant updatedAt) {

    static SessionResponse from(CookingSession session) {
      return new SessionResponse(
          session.id().value(),
          session.recipe().name(),
          session.recipe().servings(),
          session.scale().map(DeviceId::value).orElse(null),
          session.phase(),
          session.currentStep(),
          session.recipe().steps().size(),
          session.step().map(StepResponse::from).orElse(null),
          session.recipe().steps().stream().map(StepResponse::from).toList(),
          session.completions().stream().map(StepCompletion::position).toList(),
          session.measured().map(Grams::value).orElse(null),
          session.startedAt(),
          session.updatedAt());
    }
  }
}
