package dev.haypacomer.agent.runtime;

import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AgentRuntime {

  private final ToolRegistry tools;
  private final GuardrailChain guardrails;
  private final Planner planner;
  private final Planner fallback;
  private final AgentRunStore runs;
  private final ConfirmationStore confirmations;
  private final AiAuditLog audit;
  private final Clock clock;

  public AgentRuntime(
      ToolRegistry tools,
      GuardrailChain guardrails,
      Planner planner,
      Planner fallback,
      AgentRunStore runs,
      ConfirmationStore confirmations,
      AiAuditLog audit,
      Clock clock) {
    this.tools = tools;
    this.guardrails = guardrails;
    this.planner = planner;
    this.fallback = fallback;
    this.runs = runs;
    this.confirmations = confirmations;
    this.audit = audit;
    this.clock = clock;
  }

  public AgentResult run(AgentTask task, AgentBudget budget) {
    Instant started = clock.instant();
    Instant deadline = started.plus(budget.timeout());
    AgentRun run =
        AgentRun.start(
            task.household(), task.user(), task.specialist(), budget.maxSteps(), started);
    runs.save(run);
    List<Exchange> history = new ArrayList<>();
    Planner active = planner;
    while (true) {
      if (clock.instant().isAfter(deadline)) {
        trace(run, TraceKind.ANSWER, "Stopped: the run took longer than " + budget.timeout());
        return new AgentResult(finish(run, RunStatus.OUT_OF_BUDGET), null, null);
      }
      AgentContext context =
          new AgentContext(task, tools.names(), history, run.stepBudget() - run.stepsUsed());
      Decision decision;
      try {
        decision = decide(active, context);
      } catch (PlannerUnavailableException unavailable) {
        if (active == fallback) {
          trace(run, TraceKind.ANSWER, "Stopped: no planner is available");
          return new AgentResult(finish(run, RunStatus.FAILED), null, null);
        }
        trace(
            run, TraceKind.PLAN, "Planner " + active.name() + " unavailable, using offline rules");
        record(active, Duration.ZERO, AiOutcome.FALLBACK);
        active = fallback;
        continue;
      }
      switch (decision) {
        case Decision.FinalAnswer answer -> {
          trace(run, TraceKind.ANSWER, answer.text());
          return new AgentResult(finish(run, RunStatus.DONE), answer.text(), null);
        }
        case Decision.CallTool call -> {
          if (run.stepsUsed() >= run.stepBudget()) {
            trace(run, TraceKind.ANSWER, "Stopped: step budget of " + run.stepBudget() + " used");
            return new AgentResult(finish(run, RunStatus.OUT_OF_BUDGET), null, null);
          }
          trace(run, TraceKind.PLAN, call.reason());
          run = step(run);
          trace(run, TraceKind.TOOL_CALL, call.tool() + " " + call.arguments());
          AgentTool tool = tools.find(call.tool()).orElse(null);
          if (tool == null) {
            record(active, Duration.ZERO, AiOutcome.REJECTED);
            observe(run, history, call, Observation.failure(call.tool(), "Unknown tool"));
            continue;
          }
          ToolInvocation invocation =
              new ToolInvocation(task.household(), task.user(), call.arguments());
          Optional<String> rejection =
              guardrails.reject(tool.spec(), invocation).or(() -> tool.problem(invocation));
          if (rejection.isPresent()) {
            record(active, Duration.ZERO, AiOutcome.REJECTED);
            observe(run, history, call, Observation.failure(call.tool(), rejection.get()));
            continue;
          }
          if (tool.kind() == ToolKind.WRITE) {
            PendingConfirmation pending =
                PendingConfirmation.propose(
                    task.household(),
                    task.user(),
                    tool.name(),
                    call.arguments(),
                    tool.describe(invocation),
                    clock.instant());
            confirmations.propose(pending);
            trace(run, TraceKind.OBSERVATION, "Waiting for confirmation " + pending.id());
            run = save(run, RunStatus.WAITING_CONFIRMATION, null);
            return new AgentResult(run, null, pending);
          }
          observe(run, history, call, invoke(tool, invocation));
        }
      }
    }
  }

  private Decision decide(Planner active, AgentContext context) {
    Instant before = clock.instant();
    Decision decision = active.next(context);
    record(active, Duration.between(before, clock.instant()), AiOutcome.VALID);
    return decision;
  }

  private Observation invoke(AgentTool tool, ToolInvocation invocation) {
    try {
      return tool.invoke(invocation);
    } catch (RuntimeException failure) {
      return Observation.failure(tool.name(), "Tool failed: " + failure.getMessage());
    }
  }

  private void observe(
      AgentRun run, List<Exchange> history, Decision.CallTool call, Observation observation) {
    history.add(new Exchange(call, observation));
    trace(run, TraceKind.OBSERVATION, observation.content());
  }

  private void record(Planner active, Duration latency, AiOutcome outcome) {
    Duration measured = latency.isNegative() ? Duration.ZERO : latency;
    audit.record(new AiAuditEntry(active.name(), "plan", measured, outcome, clock.instant()));
  }

  private void trace(AgentRun run, TraceKind kind, String detail) {
    runs.trace(run.id(), new TraceStep(kind, detail, clock.instant()));
  }

  private AgentRun step(AgentRun run) {
    return save(run, RunStatus.RUNNING, null, run.stepsUsed() + 1);
  }

  private AgentRun finish(AgentRun run, RunStatus status) {
    return save(run, status, clock.instant());
  }

  private AgentRun save(AgentRun run, RunStatus status, Instant finishedAt) {
    return save(run, status, finishedAt, run.stepsUsed());
  }

  private AgentRun save(AgentRun run, RunStatus status, Instant finishedAt, int stepsUsed) {
    AgentRun updated =
        new AgentRun(
            run.id(),
            run.household(),
            run.user(),
            run.specialist(),
            status,
            stepsUsed,
            run.stepBudget(),
            run.startedAt(),
            finishedAt);
    runs.save(updated);
    return updated;
  }
}
