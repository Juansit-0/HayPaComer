package dev.haypacomer.web.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.PhotoIngredient;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.PhotoRecipe;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.RecipePhotoReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RecipePhotoIntegrationTest {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10};

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  static volatile boolean providerDown;

  @TestConfiguration
  static class ScriptedVision {

    @Bean
    @Primary
    RecipePhotoReader scriptedReader() {
      return new RecipePhotoReader() {
        @Override
        public String provider() {
          return "scripted";
        }

        @Override
        public PhotoRecipe read(RecipePhoto photo) {
          if (providerDown) {
            throw new PhotoReadingUnavailableException("The AI provider is not answering");
          }
          return new PhotoRecipe(
              "Arroz con pollo",
              4,
              45,
              List.of(
                  new PhotoIngredient("Rice", "300 g"),
                  new PhotoIngredient("Unicorn meat", "1 kg")),
              List.of("Cook"),
              0.8,
              AdvisorSource.GEMINI);
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  private String signUp(String email) throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}"))
        .andExpect(status().isCreated());
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  @Test
  void aPhotoBecomesADraftThatIsNeverSavedAndIsVerifiedItemByItem() throws Exception {
    String juan = signUp("photo@haypacomer.dev");
    String stranger = signUp("photo-stranger@haypacomer.dev");
    String household =
        mvc.perform(
                post("/api/v1/households")
                    .header("Authorization", juan)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String path = "/api/v1/households/" + JsonPath.read(household, "$.id") + "/recipes/from-photo";
    MockMultipartFile photo = new MockMultipartFile("photo", "card.jpg", "image/jpeg", JPEG);

    mvc.perform(multipart(path).file(photo).header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Arroz con pollo"))
        .andExpect(jsonPath("$.ingredients[0].catalogFood").value("Rice"))
        .andExpect(jsonPath("$.ingredients[0].grams").value(300))
        .andExpect(jsonPath("$.ingredients[0].verified").value(true))
        .andExpect(jsonPath("$.ingredients[1].problem").value("Not in the food catalog"))
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.saved").value(false));

    mvc.perform(
            multipart(path)
                .file(new MockMultipartFile("photo", "x.png", "image/png", JPEG))
                .header("Authorization", juan))
        .andExpect(status().isBadRequest());
    mvc.perform(multipart(path).file(photo).header("Authorization", stranger))
        .andExpect(status().isNotFound());
    mvc.perform(multipart(path).file(photo)).andExpect(status().isUnauthorized());

    providerDown = true;
    mvc.perform(multipart(path).file(photo).header("Authorization", juan))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.title").value("Photo reading unavailable"));
    providerDown = false;
  }
}
