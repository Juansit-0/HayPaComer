package dev.haypacomer.ai.llm;

import dev.haypacomer.application.ai.AdvisorSource;

public interface LlmClient {

  AdvisorSource source();

  String completeJson(LlmPrompt prompt);
}
