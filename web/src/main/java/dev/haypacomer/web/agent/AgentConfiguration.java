package dev.haypacomer.web.agent;

import dev.haypacomer.agent.confirm.ApproveConfirmation;
import dev.haypacomer.agent.confirm.StartAgentRun;
import dev.haypacomer.agent.memory.RecallMemoryTool;
import dev.haypacomer.agent.memory.RememberTool;
import dev.haypacomer.agent.runtime.AgentRuntime;
import dev.haypacomer.agent.runtime.RuleBasedPlanner;
import dev.haypacomer.agent.tools.GuardrailChain;
import dev.haypacomer.agent.tools.PermissionGuardrail;
import dev.haypacomer.agent.tools.SchemaGuardrail;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.agent.ConfirmationDesk;
import dev.haypacomer.application.agent.ListPendingConfirmations;
import dev.haypacomer.application.agent.RejectConfirmation;
import dev.haypacomer.application.agent.RememberForHousehold;
import dev.haypacomer.application.agent.ViewAgentRun;
import dev.haypacomer.application.agent.ViewHouseholdMemory;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.HouseholdRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfiguration {

  @Bean
  ToolRegistry agentTools(ViewHouseholdMemory viewMemory, RememberForHousehold remember) {
    return new ToolRegistry(List.of(new RecallMemoryTool(viewMemory), new RememberTool(remember)));
  }

  @Bean
  GuardrailChain agentGuardrails(HouseholdRepository households) {
    return new GuardrailChain(List.of(new SchemaGuardrail(), new PermissionGuardrail(households)));
  }

  @Bean
  AgentRuntime agentRuntime(
      ToolRegistry tools,
      GuardrailChain guardrails,
      AgentRunStore runs,
      ConfirmationStore confirmations,
      AiAuditLog audit,
      Clock clock) {
    RuleBasedPlanner offline = new RuleBasedPlanner(List.of(RecallMemoryTool.SPEC.name()));
    return new AgentRuntime(tools, guardrails, offline, offline, runs, confirmations, audit, clock);
  }

  @Bean
  StartAgentRun startAgentRun(HouseholdRepository households, AgentRuntime runtime) {
    return new StartAgentRun(households, runtime);
  }

  @Bean
  ConfirmationDesk confirmationDesk(
      ConfirmationStore confirmations, AgentRunStore runs, Clock clock) {
    return new ConfirmationDesk(confirmations, runs, clock);
  }

  @Bean
  ApproveConfirmation approveConfirmation(
      ConfirmationDesk desk, ToolRegistry tools, GuardrailChain guardrails) {
    return new ApproveConfirmation(desk, tools, guardrails);
  }

  @Bean
  RejectConfirmation rejectConfirmation(ConfirmationDesk desk) {
    return new RejectConfirmation(desk);
  }

  @Bean
  ListPendingConfirmations listPendingConfirmations(ConfirmationStore confirmations, Clock clock) {
    return new ListPendingConfirmations(confirmations, clock);
  }

  @Bean
  ViewAgentRun viewAgentRun(HouseholdRepository households, AgentRunStore runs) {
    return new ViewAgentRun(households, runs);
  }
}
