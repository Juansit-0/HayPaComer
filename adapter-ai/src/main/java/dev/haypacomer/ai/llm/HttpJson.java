package dev.haypacomer.ai.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class HttpJson {

  static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient http;
  private final Duration timeout;

  HttpJson(Duration timeout) {
    this.timeout = timeout;
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  JsonNode post(URI uri, Map<String, String> headers, Object body) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    headers.forEach(request::header);
    HttpResponse<String> response;
    try {
      response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (HttpTimeoutException timedOut) {
      throw new AiUnavailableException("The AI provider timed out", timedOut);
    } catch (IOException failure) {
      throw new AiUnavailableException("The AI provider is unreachable", failure);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AiUnavailableException("Interrupted while calling the AI provider", interrupted);
    }
    if (response.statusCode() == 429 || response.statusCode() >= 500) {
      throw new AiUnavailableException("The AI provider answered " + response.statusCode());
    }
    if (response.statusCode() >= 400) {
      throw new AiUnavailableException(
          "The AI provider rejected the call: " + response.statusCode());
    }
    try {
      return JSON.readTree(response.body());
    } catch (JacksonException malformed) {
      throw new InvalidAiResponseException("The AI provider did not answer JSON");
    }
  }
}
