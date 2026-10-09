package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.IntentAction;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ResponseContractFixturesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
  private static final RecipePhoto PHOTO =
      new RecipePhoto("image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1});

  private static LlmClient answering(String json) {
    return new LlmClient() {
      @Override
      public AdvisorSource source() {
        return AdvisorSource.OPENAI_COMPATIBLE;
      }

      @Override
      public String completeJson(LlmPrompt prompt) {
        return json;
      }
    };
  }

  @Test
  void fencedJsonFromRealProvidersIsAccepted() {
    ParsedIntent intent =
        new LlmKitchenAdvisor(
                answering(
                    "```json\n{\"action\":\"STOCK\",\"food\":\"sopa de pollo\","
                        + "\"quantity\":\"300 g\",\"expiresOn\":\"2026-10-12\",\"confidence\":0.9}\n```"))
            .parseIntent("guarda 300 g de sopa que vence el lunes", TODAY)
            .orElseThrow();

    assertEquals(IntentAction.STOCK, intent.action());
    assertEquals("sopa de pollo", intent.food());
    assertEquals(LocalDate.of(2026, 10, 12), intent.expiresOn());

    PhotoRecipe recipe =
        new LlmRecipePhotoReader(
                answering(
                    "```\n{\"name\":\"Sancocho\",\"servings\":6,\"minutes\":90,"
                        + "\"ingredients\":[{\"food\":\"yuca\",\"quantity\":\"500 g\"}],"
                        + "\"confidence\":0.7}\n```"))
            .read(PHOTO);
    assertEquals("Sancocho", recipe.name());
  }

  @Test
  void proseAroundJsonIsStillRejected() {
    LlmKitchenAdvisor chatty =
        new LlmKitchenAdvisor(
            answering("Here you go: {\"action\":\"STOCK\",\"food\":\"rice\",\"confidence\":1}"));

    assertThrows(InvalidAiResponseException.class, () -> chatty.parseIntent("save rice", TODAY));
    assertThrows(
        PhotoReadingUnavailableException.class,
        () -> new LlmRecipePhotoReader(answering("```json\nnot json\n```")).read(PHOTO));
    assertEquals("{}", JsonContract.unfence("```JSON\n{}\n```"));
    assertEquals("{\"a\":1}", JsonContract.unfence("```{\"a\":1}```"));
    assertEquals("``", JsonContract.unfence("``"));
  }
}
