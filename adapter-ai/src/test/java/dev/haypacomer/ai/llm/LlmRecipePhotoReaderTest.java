package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.ai.offline.OfflineRecipePhotoReader;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.PhotoIngredient;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmRecipePhotoReaderTest {

  private static final RecipePhoto PHOTO =
      new RecipePhoto("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});

  private static final class Answer implements LlmClient {

    private final String json;
    private final List<LlmPrompt> prompts = new ArrayList<>();

    Answer(String json) {
      this.json = json;
    }

    @Override
    public AdvisorSource source() {
      return AdvisorSource.GEMINI;
    }

    @Override
    public String completeJson(LlmPrompt prompt) {
      prompts.add(prompt);
      if (json == null) {
        throw new AiUnavailableException("down");
      }
      return json;
    }
  }

  @Test
  void readsAStrictRecipeContractAndSendsThePhoto() {
    Answer client =
        new Answer(
            """
            {"name":"Arroz con pollo","servings":4,"minutes":45,
             "ingredients":[{"food":"rice","quantity":"300 g"},{"food":"chicken","quantity":"1 kg"}],
             "steps":["Brown the chicken"," Add rice "],"confidence":0.82}
            """);

    PhotoRecipe recipe = new LlmRecipePhotoReader(client).read(PHOTO);

    assertEquals("Arroz con pollo", recipe.name());
    assertEquals(4, recipe.servings());
    assertEquals(new PhotoIngredient("chicken", "1 kg"), recipe.ingredients().get(1));
    assertEquals(List.of("Brown the chicken", "Add rice"), recipe.steps());
    assertEquals(AdvisorSource.GEMINI, recipe.source());
    assertEquals("image/jpeg", client.prompts.getFirst().image().mimeType());
    assertEquals("gemini", new LlmRecipePhotoReader(client).provider());
  }

  @Test
  void stepsAreOptional() {
    PhotoRecipe recipe =
        new LlmRecipePhotoReader(
                new Answer(
                    "{\"name\":\"Toast\",\"servings\":1,\"minutes\":5,\"ingredients\":"
                        + "[{\"food\":\"bread\",\"quantity\":\"2 units\"}],\"confidence\":0.5}"))
            .read(PHOTO);

    assertTrue(recipe.steps().isEmpty());
  }

  @Test
  void anythingOutsideTheContractIsRefused() {
    String valid =
        "\"servings\":2,\"minutes\":10,\"confidence\":0.5,"
            + "\"ingredients\":[{\"food\":\"rice\",\"quantity\":\"100 g\"}]";
    for (String answer :
        List.of(
            "not json",
            "{\"name\":\"\",\"ingredients\":[]}",
            "{\"name\":\"x\",\"servings\":2,\"minutes\":10,\"confidence\":0.5,\"ingredients\":[]}",
            "{\"name\":\"x\"," + valid.replace("\"servings\":2", "\"servings\":90") + "}",
            "{\"name\":\"x\"," + valid + ",\"steps\":\"stir\"}",
            "{\"name\":\"x\"," + valid + ",\"steps\":[\" \"]}",
            "{\"name\":\"x\"," + valid + ",\"steps\":[\"" + "a".repeat(301) + "\"]}",
            "{\"name\":\"x\"," + valid.replace("\"100 g\"", "5") + "}")) {
      assertThrows(
          PhotoReadingUnavailableException.class,
          () -> new LlmRecipePhotoReader(new Answer(answer)).read(PHOTO),
          answer);
    }
  }

  @Test
  void outagesAndOfflineModeExplainThemselves() {
    PhotoReadingUnavailableException outage =
        assertThrows(
            PhotoReadingUnavailableException.class,
            () -> new LlmRecipePhotoReader(new Answer(null)).read(PHOTO));
    assertEquals("The AI provider is not answering; try later", outage.getMessage());
    OfflineRecipePhotoReader offline = new OfflineRecipePhotoReader();
    assertEquals("offline", offline.provider());
    assertTrue(
        assertThrows(PhotoReadingUnavailableException.class, () -> offline.read(PHOTO))
            .getMessage()
            .contains("AI_PROVIDER"));
  }
}
