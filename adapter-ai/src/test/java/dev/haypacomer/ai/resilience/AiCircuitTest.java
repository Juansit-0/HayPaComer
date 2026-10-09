package dev.haypacomer.ai.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPhase;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.ai.PhotoIngredient;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.ChatModel;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.RecipePhotoReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AiCircuitTest {

  private static final CircuitPolicy POLICY = new CircuitPolicy(2, Duration.ofMinutes(1));
  private static final RecipePhoto PHOTO =
      new RecipePhoto("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-10-09T18:00:00Z"));
  private final Clock clock =
      new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now.get();
        }
      };
  private final Map<AdvisorSource, CircuitState> states = new EnumMap<>(AdvisorSource.class);
  private final CircuitBreakerStore store =
      new CircuitBreakerStore() {
        @Override
        public CircuitState load(AdvisorSource provider) {
          return states.getOrDefault(provider, CircuitState.CLOSED);
        }

        @Override
        public void save(AdvisorSource provider, CircuitState state) {
          states.put(provider, state);
        }
      };
  private final MapHealth health = new MapHealth();
  private final ProviderCircuit circuit =
      new ProviderCircuit(AdvisorSource.GEMINI, store, POLICY, health, clock);
  private final AtomicInteger calls = new AtomicInteger();
  private volatile boolean down;

  private final ChatModel gemini =
      new ChatModel() {
        @Override
        public String name() {
          return "gemini";
        }

        @Override
        public String completeJson(String system, String user) {
          calls.incrementAndGet();
          if (down) {
            throw new ChatModelUnavailableException("timeout");
          }
          return "{\"action\":\"answer\",\"text\":\"ok\"}";
        }
      };

  @Test
  void theChatModelOpensTheCircuitAndTheNullModelDefersInstantly() {
    ResilientChatModel chat = new ResilientChatModel(gemini, new NullChatModel(), circuit);
    down = true;

    assertEquals(NullChatModel.DEFER, chat.completeJson("s", "u"));
    assertEquals(NullChatModel.DEFER, chat.completeJson("s", "u"));
    assertEquals(CircuitPhase.OPEN, states.get(AdvisorSource.GEMINI).phase());
    assertEquals("ai provider gemini", health.current().getFirst().component());
    assertEquals(NullChatModel.DEFER, chat.completeJson("s", "u"));
    assertEquals(2, calls.get());

    down = false;
    now.set(now.get().plus(Duration.ofMinutes(2)));
    assertEquals("{\"action\":\"answer\",\"text\":\"ok\"}", chat.completeJson("s", "u"));
    assertEquals(CircuitState.CLOSED, states.get(AdvisorSource.GEMINI));
    assertTrue(health.current().isEmpty());
    assertEquals("gemini", chat.name());
    assertEquals("none", new NullChatModel().name());
  }

  @Test
  void otherErrorsAreNotCountedAsOutages() {
    ResilientChatModel chat =
        new ResilientChatModel(
            new ChatModel() {
              @Override
              public String name() {
                return "gemini";
              }

              @Override
              public String completeJson(String system, String user) {
                throw new IllegalStateException("bug");
              }
            },
            new NullChatModel(),
            circuit);

    assertThrows(IllegalStateException.class, () -> chat.completeJson("s", "u"));
    assertTrue(states.isEmpty());
  }

  @Test
  void photoReadingShortCircuitsWhileTheProviderRests() {
    AtomicInteger reads = new AtomicInteger();
    PhotoRecipe recipe =
        new PhotoRecipe(
            "Toast",
            1,
            5,
            List.of(new PhotoIngredient("bread", "2 units")),
            List.of(),
            0.9,
            AdvisorSource.GEMINI);
    RecipePhotoReader flaky =
        new RecipePhotoReader() {
          @Override
          public String provider() {
            return "gemini";
          }

          @Override
          public PhotoRecipe read(RecipePhoto photo) {
            reads.incrementAndGet();
            if (down) {
              throw PhotoReadingUnavailableException.providerDown(
                  "The AI provider is not answering");
            }
            if (photo.bytes()[3] == 2) {
              throw new PhotoReadingUnavailableException("The photo could not be read as a recipe");
            }
            return recipe;
          }
        };
    ResilientRecipePhotoReader reader = new ResilientRecipePhotoReader(flaky, circuit);

    assertEquals(recipe, reader.read(PHOTO));
    assertThrows(
        PhotoReadingUnavailableException.class,
        () ->
            reader.read(
                new RecipePhoto(
                    "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 2})));
    assertTrue(states.getOrDefault(AdvisorSource.GEMINI, CircuitState.CLOSED).failures() == 0);
    down = true;
    assertThrows(PhotoReadingUnavailableException.class, () -> reader.read(PHOTO));
    assertThrows(PhotoReadingUnavailableException.class, () -> reader.read(PHOTO));
    int before = reads.get();
    PhotoReadingUnavailableException resting =
        assertThrows(PhotoReadingUnavailableException.class, () -> reader.read(PHOTO));
    assertEquals(ResilientRecipePhotoReader.RESTING, resting.getMessage());
    assertEquals(before, reads.get());
    assertEquals("gemini", reader.provider());
    assertTrue(!resting.isProviderDown());
  }
}
