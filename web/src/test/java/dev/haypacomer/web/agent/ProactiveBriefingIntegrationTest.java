package dev.haypacomer.web.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.agent.proactive.ScheduledBriefings;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes",
      "haypacomer.agent.briefings-cron=-"
    })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ProactiveBriefingIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;
  @Autowired private ScheduledBriefings briefings;

  private ResultActions send(String path, String bearer, String body) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request);
  }

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  @Test
  void expiringFoodTriggersOneCoachBriefingInTheInbox() throws Exception {
    send(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"brief@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}")
        .andExpect(status().isCreated());
    String juan =
        "Bearer "
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/auth/login",
                        null,
                        "{\"email\":\"brief@haypacomer.dev\",\"password\":\"fresh-milk-842\"}")),
                "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/households",
                        juan,
                        "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
                "$.id");
    String fridge = body(send(base + "/fridges", juan, "{\"name\":\"Kitchen\"}"));
    String place =
        "{\"fridgeId\":\""
            + JsonPath.read(fridge, "$.id")
            + "\",\"trayId\":\""
            + JsonPath.read(fridge, "$.children[0].children[0].id")
            + "\",";
    String quietHour = String.valueOf((LocalTime.now(ZoneOffset.UTC).getHour() + 12) % 24);
    for (String key : new String[] {"briefing.morning-hour", "briefing.digest-hour"}) {
      mvc.perform(
              put(base + "/settings")
                  .header("Authorization", juan)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"key\":\"" + key + "\",\"value\":\"" + quietHour + "\"}"))
          .andExpect(status().isOk());
    }
    String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();
    for (String food : new String[] {"Milk", "Yogurt", "Chicken breast"}) {
      send(
              base + "/items",
              juan,
              place + "\"food\":\"" + food + "\",\"grams\":200,\"expiresOn\":\"" + tomorrow + "\"}")
          .andExpect(status().isCreated());
    }

    briefings.run();
    briefings.run();

    mvc.perform(get("/api/v1/notifications").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].type").value("BRIEFING"))
        .andExpect(jsonPath("$[0].title").value("3 foods expire soon"));
  }
}
