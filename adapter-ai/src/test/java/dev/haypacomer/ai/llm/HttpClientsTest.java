package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import dev.haypacomer.application.ai.AdvisorSource;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpClientsTest {

  private HttpServer server;
  private final AtomicReference<String> path = new AtomicReference<>();
  private final AtomicReference<String> auth = new AtomicReference<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String responseBody = "{}";
  private volatile long delayMillis;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          auth.set(
              exchange.getRequestHeaders().getFirst("x-goog-api-key") != null
                  ? exchange.getRequestHeaders().getFirst("x-goog-api-key")
                  : exchange.getRequestHeaders().getFirst("Authorization"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          try {
            Thread.sleep(delayMillis);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private LlmSettings settings(Duration timeout) {
    return new LlmSettings(
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/"),
        "test-key",
        "test-model",
        timeout);
  }

  @Test
  void geminiSendsTheKeyInAHeaderAndReadsTheFirstPart() {
    responseBody =
        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"ok\\\":true}\"}]}}]}";
    GeminiClient gemini = new GeminiClient(settings(Duration.ofSeconds(2)));

    assertEquals("{\"ok\":true}", gemini.completeJson(new LlmPrompt("rules", "question")));
    assertEquals("/v1/models/test-model:generateContent", path.get());
    assertEquals("test-key", auth.get());
    assertTrue(requestBody.get().contains("\"responseMimeType\":\"application/json\""));
    assertFalse(requestBody.get().contains("test-key"));
    assertEquals(AdvisorSource.GEMINI, gemini.source());
  }

  @Test
  void openAiCompatibleUsesChatCompletionsWithJsonMode() {
    responseBody = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}";
    OpenAiCompatibleClient openAi = new OpenAiCompatibleClient(settings(Duration.ofSeconds(2)));

    assertEquals("{}", openAi.completeJson(new LlmPrompt("rules", "question")));
    assertEquals("/v1/chat/completions", path.get());
    assertEquals("Bearer test-key", auth.get());
    assertTrue(requestBody.get().contains("\"json_object\""));
    assertEquals(AdvisorSource.OPENAI_COMPATIBLE, openAi.source());
  }

  @Test
  void imagesTravelInlineForBothProviders() {
    LlmPrompt prompt = new LlmPrompt("rules", "read", LlmImage.of("image/png", new byte[] {1, 2}));
    responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}";
    new GeminiClient(settings(Duration.ofSeconds(2))).completeJson(prompt);
    assertTrue(
        requestBody
                .get()
                .contains("\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\"AQI=\"}")
            || requestBody
                .get()
                .contains("\"inline_data\":{\"data\":\"AQI=\",\"mime_type\":\"image/png\"}"));

    responseBody = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}";
    new OpenAiCompatibleClient(settings(Duration.ofSeconds(2))).completeJson(prompt);
    assertTrue(requestBody.get().contains("data:image/png;base64,AQI="));
    assertTrue(requestBody.get().contains("\"image_url\""));
    assertEquals("LlmImage[image/png]", prompt.image().toString());
  }

  @Test
  void providerFailuresAreReportedAsUnavailable() {
    GeminiClient gemini = new GeminiClient(settings(Duration.ofSeconds(2)));
    LlmPrompt prompt = new LlmPrompt("rules", "question");

    for (int failing : new int[] {429, 500, 503, 401}) {
      status = failing;
      assertThrows(AiUnavailableException.class, () -> gemini.completeJson(prompt));
    }
    status = 200;
    delayMillis = 1_500;
    GeminiClient impatient = new GeminiClient(settings(Duration.ofMillis(300)));
    assertThrows(AiUnavailableException.class, () -> impatient.completeJson(prompt));
    delayMillis = 0;
    server.stop(0);
    assertThrows(AiUnavailableException.class, () -> gemini.completeJson(prompt));
  }

  @Test
  void answersWithoutTheExpectedShapeAreInvalid() {
    LlmPrompt prompt = new LlmPrompt("rules", "question");
    GeminiClient gemini = new GeminiClient(settings(Duration.ofSeconds(2)));
    OpenAiCompatibleClient openAi = new OpenAiCompatibleClient(settings(Duration.ofSeconds(2)));

    responseBody = "<html>";
    assertThrows(InvalidAiResponseException.class, () -> gemini.completeJson(prompt));
    responseBody = "{\"candidates\":[]}";
    assertThrows(InvalidAiResponseException.class, () -> gemini.completeJson(prompt));
    assertThrows(InvalidAiResponseException.class, () -> openAi.completeJson(prompt));
  }

  @Test
  void settingsNeverPrintTheKey() {
    LlmSettings settings = settings(Duration.ofSeconds(1));

    assertFalse(settings.toString().contains("test-key"));
    URI url = URI.create("http://localhost/");
    assertThrows(
        IllegalArgumentException.class,
        () -> new LlmSettings(url, " ", "model", Duration.ofSeconds(1)));
    assertThrows(
        IllegalArgumentException.class, () -> new LlmSettings(url, "k", "", Duration.ofSeconds(1)));
    assertThrows(
        IllegalArgumentException.class, () -> new LlmSettings(url, "k", "model", Duration.ZERO));
  }
}
