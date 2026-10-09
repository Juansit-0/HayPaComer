package dev.haypacomer.web.agent;

import dev.haypacomer.agent.chat.ChatReply;
import dev.haypacomer.agent.chat.ChefChat;
import dev.haypacomer.agent.chat.ReadConversation;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ChatRole;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.agent.ConversationSummary;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChefChatController {

  private final ChefChat chat;
  private final ReadConversation readConversation;
  private final ConversationStore conversations;

  public ChefChatController(
      ChefChat chat, ReadConversation readConversation, ConversationStore conversations) {
    this.chat = chat;
    this.readConversation = readConversation;
    this.conversations = conversations;
  }

  @PostMapping("/households/{householdId}/agent/chat")
  ChatResponse chat(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody ChatRequest request) {
    ChatReply reply =
        chat.chat(
            new HouseholdId(householdId),
            CurrentUser.of(jwt),
            Optional.ofNullable(request.conversationId()).map(ConversationId::new),
            request.message(),
            Optional.ofNullable(request.specialist()));
    return new ChatResponse(
        reply.conversation().value(),
        reply.answer().answer(),
        reply.answer().parts().stream().map(part -> RunBrief.from(part.run())).toList(),
        reply.evidence().stream().map(Evidence::from).toList(),
        reply.answer().pending().map(Pending::from).orElse(null));
  }

  @GetMapping("/agent/conversations")
  List<ConversationResponse> conversations(@AuthenticationPrincipal Jwt jwt) {
    return conversations.recent(CurrentUser.of(jwt), 20).stream()
        .map(ConversationResponse::from)
        .toList();
  }

  @GetMapping("/agent/conversations/{conversationId}")
  List<MessageResponse> messages(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
    return readConversation.read(CurrentUser.of(jwt), new ConversationId(conversationId)).stream()
        .map(MessageResponse::from)
        .toList();
  }

  record ChatRequest(
      @NotBlank @Size(max = 1000) String message,
      UUID conversationId,
      @Pattern(regexp = "chef|market|cold|coach") String specialist) {}

  record RunBrief(UUID id, String specialist, RunStatus status, int stepsUsed) {

    static RunBrief from(AgentRun run) {
      return new RunBrief(run.id().value(), run.specialist(), run.status(), run.stepsUsed());
    }
  }

  record Evidence(String tool, String content) {

    static Evidence from(Observation observation) {
      return new Evidence(observation.tool(), observation.content());
    }
  }

  record Pending(UUID id, String tool, String summary, Instant expiresAt) {

    static Pending from(PendingConfirmation pending) {
      return new Pending(pending.id(), pending.tool(), pending.summary(), pending.expiresAt());
    }
  }

  record ChatResponse(
      UUID conversationId,
      String answer,
      List<RunBrief> runs,
      List<Evidence> evidence,
      Pending confirmation) {}

  record ConversationResponse(UUID id, Instant lastActivity) {

    static ConversationResponse from(ConversationSummary summary) {
      return new ConversationResponse(summary.id().value(), summary.lastActivity());
    }
  }

  record MessageResponse(ChatRole role, String text, Instant at) {

    static MessageResponse from(ChatMessage message) {
      return new MessageResponse(message.role(), message.text(), message.at());
    }
  }
}
