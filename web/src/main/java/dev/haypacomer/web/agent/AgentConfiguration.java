package dev.haypacomer.web.agent;

import dev.haypacomer.agent.chat.ChefChat;
import dev.haypacomer.agent.chat.ReadConversation;
import dev.haypacomer.agent.confirm.ApproveConfirmation;
import dev.haypacomer.agent.kitchen.AddToMarketTool;
import dev.haypacomer.agent.kitchen.EstimateExpiryTool;
import dev.haypacomer.agent.kitchen.InvestigateColdTool;
import dev.haypacomer.agent.kitchen.KitchenToday;
import dev.haypacomer.agent.kitchen.QueryInventoryTool;
import dev.haypacomer.agent.kitchen.ReviewBudgetTool;
import dev.haypacomer.agent.kitchen.ViewColdChainTool;
import dev.haypacomer.agent.kitchen.ViewExpiriesTool;
import dev.haypacomer.agent.kitchen.ViewMarketListTool;
import dev.haypacomer.agent.kitchen.ViewWeeklyPlanTool;
import dev.haypacomer.agent.kitchen.WastePatternsTool;
import dev.haypacomer.agent.memory.RecallMemoryTool;
import dev.haypacomer.agent.memory.RememberTool;
import dev.haypacomer.agent.supervisor.KeywordRouter;
import dev.haypacomer.agent.supervisor.PlannerFactory;
import dev.haypacomer.agent.supervisor.Supervisor;
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
import dev.haypacomer.application.analytics.FindWastePatterns;
import dev.haypacomer.application.coldchain.InvestigateColdIncidents;
import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.application.inventory.EstimateExpiry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.ViewMarketBudget;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfiguration {

  @Bean
  ToolRegistry agentTools(
      ViewHouseholdMemory viewMemory,
      RememberForHousehold remember,
      ViewInventory inventory,
      ViewMarketList market,
      AddToMarketList addToMarket,
      FoodCatalogRepository catalog,
      ListColdChains coldChains,
      ViewCurrentPlan plans,
      ViewMarketBudget budget,
      InvestigateColdIncidents coldIncidents,
      FindWastePatterns wastePatterns,
      EstimateExpiry estimateExpiry,
      HouseholdRepository households,
      Clock clock) {
    KitchenToday today = new KitchenToday(households, clock);
    return new ToolRegistry(
        List.of(
            new RecallMemoryTool(viewMemory),
            new RememberTool(remember),
            new QueryInventoryTool(inventory, today),
            new ViewExpiriesTool(inventory, today),
            new EstimateExpiryTool(estimateExpiry),
            new ViewMarketListTool(market),
            new AddToMarketTool(addToMarket, catalog),
            new ViewColdChainTool(coldChains),
            new ViewWeeklyPlanTool(plans),
            new ReviewBudgetTool(budget),
            new InvestigateColdTool(coldIncidents),
            new WastePatternsTool(wastePatterns)));
  }

  @Bean
  GuardrailChain agentGuardrails(HouseholdRepository households) {
    return new GuardrailChain(List.of(new SchemaGuardrail(), new PermissionGuardrail(households)));
  }

  @Bean
  Supervisor supervisor(
      HouseholdRepository households,
      ToolRegistry tools,
      GuardrailChain guardrails,
      AgentRunStore runs,
      ConfirmationStore confirmations,
      AiAuditLog audit,
      PlannerFactory planners,
      Clock clock) {
    return new Supervisor(
        households,
        tools,
        guardrails,
        new KeywordRouter(),
        planners,
        runs,
        confirmations,
        audit,
        clock);
  }

  @Bean
  ChefChat chefChat(Supervisor supervisor, ConversationStore conversations, Clock clock) {
    return new ChefChat(supervisor, conversations, clock);
  }

  @Bean
  ReadConversation readConversation(ConversationStore conversations) {
    return new ReadConversation(conversations);
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
