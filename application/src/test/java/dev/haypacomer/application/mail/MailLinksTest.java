package dev.haypacomer.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MailLinksTest {

  @Test
  void buildsFragmentLinksSoTokensStayOutOfServerLogs() {
    assertEquals(
        "https://haypacomer.dev/join#token=a%2Bb",
        new MailLinks("https://haypacomer.dev/").to("join", "a+b"));
    assertEquals(
        "http://localhost:5173/join#token=x",
        new MailLinks("http://localhost:5173").to("join", "x"));
  }

  @Test
  void requiresHttpsOutsideLocalhost() {
    assertThrows(IllegalArgumentException.class, () -> new MailLinks("http://haypacomer.dev"));
  }
}
