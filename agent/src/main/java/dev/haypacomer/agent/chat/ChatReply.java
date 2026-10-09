package dev.haypacomer.agent.chat;

import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.supervisor.SupervisorAnswer;
import dev.haypacomer.application.agent.ConversationId;
import java.util.List;
import java.util.Objects;

public record ChatReply(ConversationId conversation, SupervisorAnswer answer) {

  public ChatReply {
    Objects.requireNonNull(conversation, "conversation");
    Objects.requireNonNull(answer, "answer");
  }

  public List<Observation> evidence() {
    return answer.parts().stream().flatMap(part -> part.evidence().stream()).toList();
  }
}
