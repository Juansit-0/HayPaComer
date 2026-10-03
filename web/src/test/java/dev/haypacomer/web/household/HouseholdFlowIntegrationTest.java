package dev.haypacomer.web.household;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class HouseholdFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private HouseholdRepository households;

  private String signUp(String email, String name) throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"fresh-milk-842\",\"displayName\":\""
                        + name
                        + "\"}"))
        .andExpect(status().isCreated());
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  private UUID idOf(String bearer) throws Exception {
    String me =
        mvc.perform(get("/api/v1/me").header("Authorization", bearer))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(me, "$.id"));
  }

  @Test
  void ownerManagesTheHouseholdAndOutsidersSeeNothing() throws Exception {
    String juan = signUp("juan@haypacomer.dev", "Juan");
    String ana = signUp("ana@haypacomer.dev", "Ana");
    String created =
        mvc.perform(
                post("/api/v1/households")
                    .header("Authorization", juan)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Apartment 402\",\"currency\":\"COP\",\"timezone\":\"America/Bogota\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String path = "/api/v1/households/" + JsonPath.read(created, "$.id");

    mvc.perform(get(path).header("Authorization", ana)).andExpect(status().isNotFound());

    UUID anaId = idOf(ana);
    Household household =
        households
            .findById(new HouseholdId(UUID.fromString(JsonPath.read(created, "$.id"))))
            .orElseThrow();
    household.join(new UserId(anaId), Role.MEMBER, Instant.now());
    households.save(household);

    mvc.perform(get(path).header("Authorization", ana))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRole").value("MEMBER"));
    mvc.perform(
            patch(path)
                .header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mine\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch(path)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Casa\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Casa"));
    mvc.perform(delete(path + "/members/" + idOf(juan)).header("Authorization", juan))
        .andExpect(status().isConflict());
    mvc.perform(
            put(path + "/owner")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + anaId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRole").value("MEMBER"));
    mvc.perform(get("/api/v1/households").header("Authorization", ana))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].myRole").value("OWNER"));
  }
}
