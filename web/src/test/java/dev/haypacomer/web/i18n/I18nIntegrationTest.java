package dev.haypacomer.web.i18n;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class I18nIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

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
  void bundlesArePublicCacheableAndFollowTheBrowser() throws Exception {
    mvc.perform(get("/api/v1/i18n"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.current").value("es-CO"))
        .andExpect(jsonPath("$.available[*].code", hasItem("en")));
    mvc.perform(get("/api/v1/i18n").header("Accept-Language", "en-US,en;q=0.9"))
        .andExpect(jsonPath("$.current").value("en"));
    String tag =
        mvc.perform(get("/api/v1/i18n/es-CO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['food.chicken breast']").value("Pechuga de pollo"))
            .andExpect(header().exists("ETag"))
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    mvc.perform(get("/api/v1/i18n/es-CO").header("If-None-Match", tag))
        .andExpect(status().isNotModified());
    mvc.perform(get("/api/v1/i18n/fr").header("Accept-Language", "es-CO"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("No encontrado"))
        .andExpect(jsonPath("$.detail").value("Idioma desconocido: fr"));
    mvc.perform(get("/api/v1/i18n/fr")).andExpect(jsonPath("$.title").value("Not found"));
  }

  @Test
  void errorsAndSettingsSpeakTheLanguageOfThePerson() throws Exception {
    String juan = signUp("i18n@haypacomer.dev");
    String missing = "/api/v1/households/" + UUID.randomUUID();

    mvc.perform(get(missing).header("Authorization", juan).header("Accept-Language", "es"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Hogar no encontrado"));
    mvc.perform(
            get("/api/v1/settings/defaults")
                .header("Authorization", juan)
                .header("Accept-Language", "es"))
        .andExpect(
            jsonPath("$[?(@.key == 'auth.min-password-length')].description")
                .value("Longitud mínima de la contraseña"));
    mvc.perform(get("/api/v1/foods").param("q", "pechu").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Chicken breast"));

    mvc.perform(
            put("/api/v1/me/locale")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locale\":\"en\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locale").value("en"));
    mvc.perform(get(missing).header("Authorization", juan).header("Accept-Language", "es"))
        .andExpect(jsonPath("$.detail").value("Household not found"));
    mvc.perform(get("/api/v1/i18n").header("Authorization", juan))
        .andExpect(jsonPath("$.current").value("en"));
    mvc.perform(
            put("/api/v1/me/locale")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locale\":\"es-CO\"}"))
        .andExpect(status().isOk());
    mvc.perform(get(missing).header("Authorization", juan))
        .andExpect(jsonPath("$.detail").value("Hogar no encontrado"));
    mvc.perform(
            put("/api/v1/me/locale")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locale\":\"fr\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/v1/me/locale")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locale\":\"en\"}"))
        .andExpect(status().isUnauthorized());
  }
}
