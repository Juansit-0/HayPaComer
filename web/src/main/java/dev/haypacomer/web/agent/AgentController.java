package dev.haypacomer.web.agent;

import dev.haypacomer.agent.confirm.ApproveConfirmation;
import dev.haypacomer.agent.supervisor.Supervisor;
import dev.haypacomer.agent.supervisor.SupervisorAnswer;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.ListPendingConfirmations;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RejectConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.RunTrace;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.agent.ViewAgentRun;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1")
public class AgentController {

  private final Supervisor supervisor;
  private final ViewAgentRun viewAgentRun;
  private final ListPendingConfirmations listPendingConfirmations;
  private final ApproveConfirmation approveConfirmation;
  private final RejectConfirmation rejectConfirmation;

  public AgentController(
      Supervisor supervisor,
      ViewAgentRun viewAgentRun,
      ListPendingConfirmations listPendingConfirmations,
      ApproveConfirmation approveConfirmation,
      RejectConfirmation rejectConfirmation) {
    this.supervisor = supervisor;
    this.viewAgentRun = viewAgentRun;
    this.listPendingConfirmations = listPendingConfirmations;
    this.approveConfirmation = approveConfirmation;
    this.rejectConfirmation = rejectConfirmation;
  }

  @PostMapping("/households/{householdId}/agent/runs")
  @ResponseStatus(HttpStatus.CREATED)
  RunResponse start(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody RunRequest request) {
    SupervisorAnswer answer =
        supervisor.handle(
            new HouseholdId(householdId),
            CurrentUser.of(jwt),
            request.goal(),
            Optional.ofNullable(request.specialist()));
    return new RunResponse(
        answer.parts().stream().map(part -> RunSummary.from(part.run())).toList(),
        answer.answer(),
        answer.pending().map(ConfirmationResponse::from).orElse(null));
  }

  @GetMapping("/agent/runs/{runId}/trace")
  TraceResponse trace(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID runId) {
    RunTrace trace = viewAgentRun.view(CurrentUser.of(jwt), new AgentRunId(runId));
    return new TraceResponse(
        RunSummary.from(trace.run()), trace.steps().stream().map(StepResponse::from).toList());
  }

  @GetMapping("/agent/confirmations")
  List<ConfirmationResponse> confirmations(@AuthenticationPrincipal Jwt jwt) {
    return listPendingConfirmations.list(CurrentUser.of(jwt)).stream()
        .map(ConfirmationResponse::from)
        .toList();
  }

  @PostMapping("/agent/confirmations/{confirmationId}/approve")
  ApprovalResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID confirmationId) {
    return new ApprovalResponse(
        approveConfirmation.approve(CurrentUser.of(jwt), confirmationId).content());
  }

  @PostMapping("/agent/confirmations/{confirmationId}/reject")
  ConfirmationResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID confirmationId) {
    return ConfirmationResponse.from(
        rejectConfirmation.reject(CurrentUser.of(jwt), confirmationId));
  }

  record RunRequest(
      @NotBlank @Size(max = 500) String goal,
      @Pattern(regexp = "chef|market|cold|coach") String specialist) {}

  record RunSummary(
      UUID id,
      UUID householdId,
      String specialist,
      RunStatus status,
      int stepsUsed,
      int stepBudget,
      Instant startedAt,
      Instant finishedAt) {

    static RunSummary from(AgentRun run) {
      return new RunSummary(
          run.id().value(),
          run.household().value(),
          run.specialist(),
          run.status(),
          run.stepsUsed(),
          run.stepBudget(),
          run.startedAt(),
          run.finishedAt());
    }
  }

  record RunResponse(List<RunSummary> runs, String answer, ConfirmationResponse confirmation) {}

  record StepResponse(TraceKind kind, String detail, Instant at) {

    static StepResponse from(TraceStep step) {
      return new StepResponse(step.kind(), step.detail(), step.at());
    }
  }

  record TraceResponse(RunSummary run, List<StepResponse> steps) {}

  record ConfirmationResponse(
      UUID id,
      UUID runId,
      UUID householdId,
      String tool,
      Map<String, String> arguments,
      String summary,
      Instant proposedAt,
      Instant expiresAt) {

    static ConfirmationResponse from(PendingConfirmation pending) {
      return new ConfirmationResponse(
          pending.id(),
          pending.run().value(),
          pending.household().value(),
          pending.tool(),
          pending.arguments(),
          pending.summary(),
          pending.proposedAt(),
          pending.expiresAt());
    }
  }

  record ApprovalResponse(String result) {}
}
