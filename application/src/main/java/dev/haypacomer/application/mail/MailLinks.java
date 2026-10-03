package dev.haypacomer.application.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record MailLinks(String baseUrl) {

  public MailLinks {
    Objects.requireNonNull(baseUrl, "baseUrl");
    if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://localhost")) {
      throw new IllegalArgumentException("Mail links need an https base URL");
    }
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  public String to(String page, String token) {
    return baseUrl + "/" + page + "#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }
}
