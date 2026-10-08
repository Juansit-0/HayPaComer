package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CookingSessionIntegrationTest {

  private static final String RICE_BOWL =
      "{\"name\":\"Rice bowl\",\"servings\":2,\"minutes\":25,\"targetServings\":4,"
          + "\"requirements\":[{\"food\":\"Rice\",\"quantity\":\"150 g\"},"
          + "{\"food\":\"Egg\",\"quantity\":\"2 huevos\"}],"
          + "\"steps\":[{\"instruction\":\"Weigh the rice\",\"weigh\":{\"food\":\"Rice\",\"grams\":150}},"
          + "{\"instruction\":\"Boil\",\"timerSeconds\":900},{\"instruction\":\"Serve\"}]}";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  private ResultActions send(String path, String bearer, String body) throws Exception {
    return mvc.perform(
        post(path)
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  @Test
  void cooksStepByStepAndResumesTheActiveSession() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}"))
        .andExpect(status().isCreated());
    String juan =
        "Bearer "
            + JsonPath.read(
                body(
                    mvc.perform(
                        post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"))),
                "$.accessToken");
    String sessions =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/households",
                        juan,
                        "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
                "$.id")
            + "/cooking-sessions";

    mvc.perform(get(sessions + "/active").header("Authorization", juan))
        .andExpect(status().isNoContent());
    send(
            sessions,
            juan,
            RICE_BOWL.replace(
                "\"targetServings\":4",
                "\"targetServings\":4,\"scaleId\":\"" + UUID.randomUUID() + "\""))
        .andExpect(status().isNotFound());
    String created =
        body(
            send(sessions, juan, RICE_BOWL)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phase").value("PREPARING"))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.totalSteps").value(3))
                .andExpect(jsonPath("$.steps[0].weighGrams").value(300.0))
                .andExpect(jsonPath("$.steps[1].timerSeconds").value(900)));
    String session = sessions + "/" + JsonPath.read(created, "$.id");

    send(sessions, juan, RICE_BOWL)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.activeSessionId").value(JsonPath.<String>read(created, "$.id")));
    send(session + "/pause", juan, "")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Invalid session step"));
    send(session + "/next", juan, "")
        .andExpect(jsonPath("$.phase").value("COOKING"))
        .andExpect(jsonPath("$.step.instruction").value("Weigh the rice"));
    send(session + "/next", juan, "").andExpect(jsonPath("$.currentStep").value(2));
    send(session + "/pause", juan, "").andExpect(jsonPath("$.phase").value("PAUSED"));
    mvc.perform(get(sessions + "/active").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phase").value("PAUSED"))
        .andExpect(jsonPath("$.step.instruction").value("Boil"))
        .andExpect(jsonPath("$.completedSteps[0]").value(1));
    send(session + "/resume", juan, "").andExpect(jsonPath("$.currentStep").value(2));
    send(session + "/next", juan, "");
    send(session + "/next", juan, "")
        .andExpect(jsonPath("$.phase").value("FINISHED"))
        .andExpect(jsonPath("$.completedSteps.length()").value(3));
    mvc.perform(get(sessions + "/active").header("Authorization", juan))
        .andExpect(status().isNoContent());
    mvc.perform(get(sessions + "/" + UUID.randomUUID()).header("Authorization", juan))
        .andExpect(status().isNotFound());
    send(sessions, juan, RICE_BOWL.replace("\"Rice\",\"grams\"", "\"Tuna\",\"grams\""))
        .andExpect(status().isBadRequest());
  }
}
