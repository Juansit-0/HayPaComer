package dev.haypacomer.application.agent;

import java.util.Arrays;
import java.util.Optional;

public enum MemoryTopic {
  PREFERENCE("preference"),
  USUAL_QUANTITY("usual"),
  ACCEPTED_DISH("dish"),
  DECISION("decision");

  private final String prefix;

  MemoryTopic(String prefix) {
    this.prefix = prefix;
  }

  public String prefix() {
    return prefix;
  }

  static Optional<MemoryTopic> ofPrefix(String prefix) {
    return Arrays.stream(values()).filter(topic -> topic.prefix.equals(prefix)).findFirst();
  }
}
