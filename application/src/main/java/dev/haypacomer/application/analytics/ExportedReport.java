package dev.haypacomer.application.analytics;

import java.util.Objects;

public record ExportedReport(String fileName, String mediaType, String content) {

  public ExportedReport {
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(content, "content");
  }
}
