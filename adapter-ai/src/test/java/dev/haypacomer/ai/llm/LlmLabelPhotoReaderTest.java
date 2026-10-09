package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.ai.offline.OfflineLabelPhotoReader;
import dev.haypacomer.ai.resilience.ProviderCircuit;
import dev.haypacomer.ai.resilience.ResilientLabelPhotoReader;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.ServiceHealth;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmLabelPhotoReaderTest {

  private static final RecipePhoto PHOTO =
      new RecipePhoto("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});

  private static LlmClient answering(String json) {
    return new LlmClient() {
      @Override
      public AdvisorSource source() {
        return AdvisorSource.GEMINI;
      }

      @Override
      public String completeJson(LlmPrompt prompt) {
        if (json == null) {
          throw new AiUnavailableException("down");
        }
        return json;
      }
    };
  }

  @Test
  void readsThePrintedDateStrictly() {
    LabelReading label =
        new LlmLabelPhotoReader(
                answering(
                    "{\"product\":\"Leche entera\",\"date\":\"2026-10-15\",\"confidence\":0.9}"))
            .read(PHOTO);

    assertEquals("Leche entera", label.product());
    assertEquals(LocalDate.of(2026, 10, 15), label.date().orElseThrow());
    assertEquals(0.9, label.confidence());
    assertEquals(AdvisorSource.GEMINI, label.source());
    assertEquals("gemini", new LlmLabelPhotoReader(answering("{}")).provider());
    assertNull(
        new LlmLabelPhotoReader(answering("{\"date\":null,\"confidence\":0.2}"))
            .read(PHOTO)
            .printedDate());
  }

  @Test
  void rejectsWhatIsNotADateAndReportsOutages() {
    PhotoReadingUnavailableException invalid =
        assertThrows(
            PhotoReadingUnavailableException.class,
            () ->
                new LlmLabelPhotoReader(answering("{\"date\":\"15/10/2026\",\"confidence\":0.9}"))
                    .read(PHOTO));
    assertFalse(invalid.isProviderDown());
    assertThrows(
        PhotoReadingUnavailableException.class,
        () -> new LlmLabelPhotoReader(answering("{\"date\":7,\"confidence\":0.9}")).read(PHOTO));
    assertThrows(
        PhotoReadingUnavailableException.class,
        () -> new LlmLabelPhotoReader(answering("{\"date\":null,\"confidence\":2}")).read(PHOTO));
    assertTrue(
        assertThrows(
                PhotoReadingUnavailableException.class,
                () -> new LlmLabelPhotoReader(answering(null)).read(PHOTO))
            .isProviderDown());
    assertTrue(
        assertThrows(
                PhotoReadingUnavailableException.class,
                () -> new OfflineLabelPhotoReader().read(PHOTO))
            .isProviderDown());
    assertEquals("offline", new OfflineLabelPhotoReader().provider());
  }

  private static ServiceHealth quiet() {
    return new ServiceHealth() {
      @Override
      public void degraded(String component, String reason, Instant at) {}

      @Override
      public void recovered(String component) {}

      @Override
      public List<dev.haypacomer.application.resilience.DegradedComponent> current() {
        return List.of();
      }
    };
  }

  @Test
  void theCircuitRestsAfterRepeatedOutages() {
    Map<AdvisorSource, CircuitState> states = new EnumMap<>(AdvisorSource.class);
    CircuitBreakerStore store =
        new CircuitBreakerStore() {
          @Override
          public CircuitState load(AdvisorSource source) {
            return states.getOrDefault(source, CircuitState.CLOSED);
          }

          @Override
          public void save(AdvisorSource source, CircuitState state) {
            states.put(source, state);
          }
        };
    ResilientLabelPhotoReader reader =
        new ResilientLabelPhotoReader(
            new LlmLabelPhotoReader(answering(null)),
            new ProviderCircuit(
                AdvisorSource.GEMINI, store, CircuitPolicy.DEFAULT, quiet(), Clock.systemUTC()));

    for (int attempt = 0; attempt < 4; attempt++) {
      assertThrows(PhotoReadingUnavailableException.class, () -> reader.read(PHOTO));
    }
    assertEquals("gemini", reader.provider());
    assertTrue(states.get(AdvisorSource.GEMINI).phase().name().equals("OPEN"));
  }
}
