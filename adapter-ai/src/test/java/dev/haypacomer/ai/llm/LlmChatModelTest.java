package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.ai.AdvisorSource;
import org.junit.jupiter.api.Test;

class LlmChatModelTest {

  private static LlmClient client(RuntimeException failure) {
    return new LlmClient() {
      @Override
      public AdvisorSource source() {
        return AdvisorSource.OPENAI_COMPATIBLE;
      }

      @Override
      public String completeJson(LlmPrompt prompt) {
        if (failure != null) {
          throw failure;
        }
        return prompt.system() + "|" + prompt.user();
      }
    };
  }

  @Test
  void passesPromptsAndReportsFailuresAsUnavailable() {
    assertEquals("s|u", new LlmChatModel(client(null)).completeJson("s", "u"));
    assertEquals("openai_compatible", new LlmChatModel(client(null)).name());
    assertThrows(
        ChatModelUnavailableException.class,
        () -> new LlmChatModel(client(new AiUnavailableException("down"))).completeJson("s", "u"));
    assertThrows(
        ChatModelUnavailableException.class,
        () ->
            new LlmChatModel(client(new InvalidAiResponseException("bad"))).completeJson("s", "u"));
  }
}
