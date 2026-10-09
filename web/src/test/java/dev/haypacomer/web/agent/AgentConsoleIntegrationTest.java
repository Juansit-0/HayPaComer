package dev.haypacomer.web.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Map;
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
class AgentConsoleIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;
  @Autowired private AgentRunStore runs;
  @Autowired private ConfirmationStore confirmations;

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

  private String json(org.springframework.test.web.servlet.ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  @Test
  void runsLeaveATraceAndWritesWaitForTheirAuthor() throws Exception {
    String juan = signUp("console@haypacomer.dev");
    String stranger = signUp("outsider@haypacomer.dev");
    String householdId =
        JsonPath.read(
            json(
                mvc.perform(
                    post("/api/v1/households")
                        .header("Authorization", juan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"))),
            "$.id");
    String base = "/api/v1/households/" + householdId;
    mvc.perform(
            patch(base + "/agent/memory")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"remember\":[{\"topic\":\"USUAL_QUANTITY\",\"subject\":\"rice\",\"value\":\"150 g\"}]}"))
        .andExpect(status().isOk());

    String run =
        json(
            mvc.perform(
                    post(base + "/agent/runs")
                        .header("Authorization", juan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"How much rice do we cook?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runs.length()").value(1))
                .andExpect(jsonPath("$.runs[0].specialist").value("chef"))
                .andExpect(jsonPath("$.runs[0].status").value("DONE"))
                .andExpect(jsonPath("$.runs[0].stepsUsed").value(3)));
    String runId = JsonPath.read(run, "$.runs[0].id");
    String answer = JsonPath.read(run, "$.answer");
    org.junit.jupiter.api.Assertions.assertTrue(answer.contains("USUAL_QUANTITY rice: 150 g"));
    org.junit.jupiter.api.Assertions.assertTrue(answer.contains("query_inventory: No food found"));

    mvc.perform(get("/api/v1/agent/runs/" + runId + "/trace").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.steps.length()").value(10))
        .andExpect(jsonPath("$.steps[1].kind").value("TOOL_CALL"))
        .andExpect(jsonPath("$.steps[9].kind").value("ANSWER"));
    mvc.perform(
            post(base + "/agent/runs")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goal\":\"La nevera esta caliente, que compro?\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.runs[0].specialist").value("cold"))
        .andExpect(jsonPath("$.runs[1].specialist").value("market"));
    mvc.perform(get("/api/v1/agent/runs/" + runId + "/trace").header("Authorization", stranger))
        .andExpect(status().isNotFound());
    mvc.perform(
            post(base + "/agent/runs")
                .header("Authorization", stranger)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goal\":\"Peek\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post(base + "/agent/runs")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goal\":\"x\",\"specialist\":\"hacker\"}"))
        .andExpect(status().isBadRequest());

    String me = json(mvc.perform(get("/api/v1/me").header("Authorization", juan)));
    UserId user = new UserId(UUID.fromString(JsonPath.read(me, "$.id")));
    HouseholdId household = new HouseholdId(UUID.fromString(householdId));
    PendingConfirmation remember = propose(household, user, "dish", "arroz con pollo");
    PendingConfirmation other = propose(household, user, "dish", "lentejas");

    mvc.perform(get("/api/v1/agent/confirmations").header("Authorization", juan))
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/v1/agent/confirmations").header("Authorization", stranger))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            post("/api/v1/agent/confirmations/" + remember.id() + "/approve")
                .header("Authorization", stranger))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/agent/confirmations/" + remember.id() + "/approve")
                .header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("Remembered dish:arroz con pollo"));
    mvc.perform(
            post("/api/v1/agent/confirmations/" + other.id() + "/reject")
                .header("Authorization", juan))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/agent/confirmations/" + other.id() + "/approve")
                .header("Authorization", juan))
        .andExpect(status().isNotFound());

    mvc.perform(get(base + "/agent/memory").header("Authorization", juan))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[1].subject").value("arroz con pollo"));
    mvc.perform(
            get("/api/v1/agent/runs/" + remember.run().value() + "/trace")
                .header("Authorization", juan))
        .andExpect(jsonPath("$.run.status").value("DONE"))
        .andExpect(
            jsonPath("$.steps[0].detail").value("Approved: Remembered dish:arroz con pollo"));
  }

  private PendingConfirmation propose(
      HouseholdId household, UserId user, String topic, String subject) {
    AgentRun run =
        AgentRun.start(household, user, "chef", 8, Instant.now())
            .advance(RunStatus.WAITING_CONFIRMATION, 1, null);
    runs.save(run);
    PendingConfirmation pending =
        PendingConfirmation.propose(
            run.id(),
            household,
            user,
            "remember",
            Map.of("topic", "accepted_dish", "subject", subject, "value", "liked"),
            "Remember " + subject,
            Instant.now());
    confirmations.propose(pending);
    return pending;
  }
}
