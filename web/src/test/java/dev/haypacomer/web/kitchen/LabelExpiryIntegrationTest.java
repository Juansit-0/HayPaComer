package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.port.LabelPhotoReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class LabelExpiryIntegrationTest {

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10};

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  static volatile int daysAhead = 5;
  static volatile boolean providerDown;

  @TestConfiguration
  static class ScriptedLabels {

    @Bean
    @Primary
    LabelPhotoReader scriptedLabelReader() {
      return new LabelPhotoReader() {
        @Override
        public String provider() {
          return "scripted";
        }

        @Override
        public LabelReading read(RecipePhoto photo) {
          if (providerDown) {
            throw PhotoReadingUnavailableException.providerDown("down");
          }
          return new LabelReading(
              "Leche entera",
              LocalDate.now(ZoneOffset.UTC).plusDays(daysAhead),
              0.95,
              AdvisorSource.GEMINI);
        }
      };
    }
  }

  @Autowired private MockMvc mvc;

  @Test
  void aLabelPhotoProposesADateThatJavaChecksAndNeverSaves() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"label@haypacomer.dev\",\"password\":\"fresh-milk-842\","
                        + "\"displayName\":\"J\"}"))
        .andExpect(status().isCreated());
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"label@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String juan = "Bearer " + JsonPath.read(login, "$.accessToken");
    String household =
        mvc.perform(
                post("/api/v1/households")
                    .header("Authorization", juan)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String path =
        "/api/v1/households/" + JsonPath.read(household, "$.id") + "/items/expiry-from-photo";
    MockMultipartFile photo = new MockMultipartFile("photo", "label.jpg", "image/jpeg", JPEG);
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    mvc.perform(multipart(path).file(photo).param("food", "Milk").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("LABEL"))
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(5).toString()))
        .andExpect(jsonPath("$.productOnLabel").value("Leche entera"))
        .andExpect(jsonPath("$.saved").value(false));

    daysAhead = 40;
    mvc.perform(
            multipart(path)
                .file(photo)
                .param("food", "Milk")
                .header("Authorization", juan)
                .header("Accept-Language", "es-CO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("ESTIMATED"))
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(7).toString()))
        .andExpect(jsonPath("$.printedDate").value(today.plusDays(40).toString()));

    providerDown = true;
    mvc.perform(
            multipart(path)
                .file(photo)
                .param("food", "Milk")
                .param("zone", "DOOR")
                .header("Authorization", juan)
                .header("Accept-Language", "es-CO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(5).toString()))
        .andExpect(
            jsonPath("$.reason").value("La IA no está disponible; esta es la vida útil habitual"));
    providerDown = false;
    daysAhead = 5;

    mvc.perform(multipart(path).file(photo).param("food", "Milk"))
        .andExpect(status().isUnauthorized());
    mvc.perform(multipart(path).file(photo).param("food", "Unicorn").header("Authorization", juan))
        .andExpect(status().isUnprocessableContent());
  }
}
