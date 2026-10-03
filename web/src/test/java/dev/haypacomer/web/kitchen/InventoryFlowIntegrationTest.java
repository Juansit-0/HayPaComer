package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InventoryFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private HouseholdRepository households;

  private String send(HttpMethod method, String path, String bearer, String body, int expected)
      throws Exception {
    var builder = request(method, path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      builder.header("Authorization", bearer);
    }
    return mvc.perform(builder)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String signUp(String email) throws Exception {
    send(
        HttpMethod.POST,
        "/api/v1/auth/register",
        null,
        "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\",\"displayName\":\"P\"}",
        201);
    String login =
        send(
            HttpMethod.POST,
            "/api/v1/auth/login",
            null,
            "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}",
            200);
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  private UUID idOf(String bearer) throws Exception {
    return UUID.fromString(
        JsonPath.read(send(HttpMethod.GET, "/api/v1/me", bearer, "", 200), "$.id"));
  }

  @Test
  void stockConsumeShareAndDiscardWithOwnership() throws Exception {
    String juan = signUp("juan@haypacomer.dev");
    String ana = signUp("ana@haypacomer.dev");
    String household =
        send(
            HttpMethod.POST,
            "/api/v1/households",
            juan,
            "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"America/Bogota\"}",
            201);
    UUID householdId = UUID.fromString(JsonPath.read(household, "$.id"));
    Household stored = households.findById(new HouseholdId(householdId)).orElseThrow();
    stored.join(new UserId(idOf(ana)), Role.MEMBER, Instant.now());
    households.save(stored);
    String base = "/api/v1/households/" + householdId;

    String fridge = send(HttpMethod.POST, base + "/fridges", juan, "{\"name\":\"Kitchen\"}", 201);
    String fridgeId = JsonPath.read(fridge, "$.id");
    String rack = JsonPath.read(fridge, "$.children[1].children[0].id");

    mvc.perform(get("/api/v1/foods?q=mil").header("Authorization", ana))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Milk"))
        .andExpect(jsonPath("$[0].allergens[0]").value("MILK"));

    String milk =
        send(
            HttpMethod.POST,
            base + "/items",
            juan,
            "{\"fridgeId\":\""
                + fridgeId
                + "\",\"trayId\":\""
                + rack
                + "\",\"food\":\"Milk\","
                + "\"grams\":892,\"tareGrams\":50,\"expiresOn\":\""
                + LocalDate.now().plusDays(6)
                + "\"}",
            201);
    String milkId = JsonPath.read(milk, "$.itemId");
    mvc.perform(
            request(HttpMethod.POST, base + "/items/" + milkId + "/consume")
                .header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grams\":192}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingGrams").value(650.0));

    String yogurt =
        send(
            HttpMethod.POST,
            base + "/items",
            juan,
            "{\"fridgeId\":\""
                + fridgeId
                + "\",\"trayId\":\""
                + rack
                + "\",\"food\":\"Yogurt\","
                + "\"grams\":125,\"visibility\":\"PRIVATE\"}",
            201);
    String yogurtId = JsonPath.read(yogurt, "$.itemId");
    send(HttpMethod.POST, base + "/items/" + yogurtId + "/consume", ana, "{\"grams\":50}", 403);
    mvc.perform(get(base + "/inventory").header("Authorization", ana))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Milk"))
        .andExpect(jsonPath("$[1].usable").value(false))
        .andExpect(jsonPath("$[1].statuses[0]").value("PRIVATE"));

    send(
        HttpMethod.POST,
        base + "/items/" + yogurtId + "/grants",
        ana,
        "{\"userId\":\"" + idOf(ana) + "\"}",
        403);
    send(
        HttpMethod.POST,
        base + "/items/" + yogurtId + "/grants",
        juan,
        "{\"userId\":\"" + idOf(ana) + "\"}",
        200);
    send(HttpMethod.POST, base + "/items/" + yogurtId + "/consume", ana, "{\"grams\":50}", 200);
    send(
        HttpMethod.PUT,
        base + "/items/" + yogurtId + "/visibility",
        juan,
        "{\"visibility\":\"ASK_FIRST\"}",
        200);
    send(HttpMethod.DELETE, base + "/items/" + yogurtId + "/grants/" + idOf(ana), juan, "", 200);
    send(HttpMethod.POST, base + "/items/" + yogurtId + "/consume", ana, "{\"grams\":10}", 403);

    mvc.perform(
            request(HttpMethod.POST, base + "/items/" + milkId + "/consume")
                .header("Authorization", ana)
                .header("Idempotency-Key", "11111111-1111-1111-1111-111111111111")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grams\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingGrams").value(600.0))
        .andExpect(jsonPath("$.replayed").value(false));
    mvc.perform(
            request(HttpMethod.POST, base + "/items/" + milkId + "/consume")
                .header("Authorization", ana)
                .header("Idempotency-Key", "11111111-1111-1111-1111-111111111111")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"grams\":50}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingGrams").value(600.0))
        .andExpect(jsonPath("$.replayed").value(true));
    send(HttpMethod.POST, base + "/items/" + milkId + "/discard", ana, "", 200);
    mvc.perform(get(base + "/activity?limit=3").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].action").value("DISCARD_FOOD"))
        .andExpect(jsonPath("$[1].action").value("CONSUME_FOOD"))
        .andExpect(jsonPath("$[1].detail.grams").value("50.00"));
    send(HttpMethod.POST, base + "/items/" + milkId + "/consume", ana, "{\"grams\":1}", 404);
    send(
        HttpMethod.POST,
        base + "/items",
        juan,
        "{\"fridgeId\":\""
            + fridgeId
            + "\",\"trayId\":\""
            + rack
            + "\",\"food\":\"Dragon fruit\",\"grams\":10}",
        422);
    mvc.perform(get(base + "/kitchen").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").value(1));

    String snapshot =
        send(HttpMethod.POST, base + "/snapshots", ana, "{\"reason\":\"before dinner\"}", 201);
    send(HttpMethod.POST, base + "/items/" + yogurtId + "/discard", juan, "", 200);
    mvc.perform(get(base + "/kitchen").header("Authorization", juan))
        .andExpect(jsonPath("$.items").value(0));
    send(HttpMethod.POST, base + "/inventory/undo", ana, "", 403);
    mvc.perform(request(HttpMethod.POST, base + "/inventory/undo").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reason").value("DISCARD_FOOD"));
    mvc.perform(get(base + "/kitchen").header("Authorization", juan))
        .andExpect(jsonPath("$.items").value(1));

    send(HttpMethod.POST, base + "/items/" + yogurtId + "/discard", juan, "", 200);
    String snapshotPath = base + "/snapshots/" + JsonPath.read(snapshot, "$.id") + "/restore";
    send(HttpMethod.POST, snapshotPath, ana, "", 403);
    mvc.perform(request(HttpMethod.POST, snapshotPath).header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").value(1));
    mvc.perform(get(base + "/inventory").header("Authorization", juan))
        .andExpect(jsonPath("$[0].statuses[0]").value("ASK_FIRST"));
    send(HttpMethod.POST, base + "/inventory/undo", juan, "", 409);
    mvc.perform(get(base + "/snapshots").header("Authorization", ana))
        .andExpect(jsonPath("$[0].reason").value("before dinner"));
  }
}
