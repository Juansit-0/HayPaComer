package dev.haypacomer.application.agent;

import dev.haypacomer.domain.quantity.QuantityParser;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record MemoryNote(MemoryTopic topic, String subject, String value) {

  public static final int MAX_SUBJECT = 60;
  public static final int MAX_VALUE = 300;

  public MemoryNote {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(value, "value");
    subject = subject.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    value = value.strip();
    if (subject.isEmpty() || subject.length() > MAX_SUBJECT || subject.contains(":")) {
      throw new IllegalArgumentException(
          "A memory subject has 1 to " + MAX_SUBJECT + " characters and no colon");
    }
    if (value.isEmpty() || value.length() > MAX_VALUE) {
      throw new IllegalArgumentException("A memory value has 1 to " + MAX_VALUE + " characters");
    }
    if (topic == MemoryTopic.USUAL_QUANTITY) {
      QuantityParser.parse(value);
    }
  }

  public String key() {
    return topic.prefix() + ":" + subject;
  }

  public static Optional<MemoryNote> fromEntry(String key, String value) {
    int colon = key.indexOf(':');
    if (colon < 0) {
      return Optional.empty();
    }
    try {
      return MemoryTopic.ofPrefix(key.substring(0, colon))
          .map(topic -> new MemoryNote(topic, key.substring(colon + 1), value));
    } catch (RuntimeException unreadable) {
      return Optional.empty();
    }
  }
}
