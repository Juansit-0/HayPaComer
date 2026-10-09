package dev.haypacomer.agent.confirm;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.ConfirmationDesk;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.domain.identity.UserId;
import java.util.Optional;
import java.util.UUID;

public final class ApproveConfirmation {

  private final ConfirmationDesk desk;
  private final ToolRegistry tools;
  private final GuardrailChain guardrails;

  public ApproveConfirmation(ConfirmationDesk desk, ToolRegistry tools, GuardrailChain guardrails) {
    this.desk = desk;
    this.tools = tools;
    this.guardrails = guardrails;
  }

  public Observation approve(UserId actor, UUID id) {
    PendingConfirmation pending = desk.take(actor, id);
    ToolInvocation invocation =
        new ToolInvocation(pending.household(), pending.user(), pending.arguments());
    AgentTool tool =
        tools
            .find(pending.tool())
            .orElseThrow(() -> refuse(pending, "The tool is no longer available"));
    Optional<String> rejection =
        guardrails.reject(tool.spec(), invocation).or(() -> tool.problem(invocation));
    if (rejection.isPresent()) {
      throw refuse(pending, rejection.get());
    }
    try {
      Observation observation = tool.invoke(invocation);
      desk.close(pending, RunStatus.DONE, "Approved: " + observation.content());
      return observation;
    } catch (RuntimeException failure) {
      desk.close(pending, RunStatus.FAILED, "Approved but failed: " + failure.getMessage());
      throw failure;
    }
  }

  private ConfirmationRefusedException refuse(PendingConfirmation pending, String reason) {
    desk.close(pending, RunStatus.FAILED, "Approved but refused: " + reason);
    return new ConfirmationRefusedException(reason);
  }
}
