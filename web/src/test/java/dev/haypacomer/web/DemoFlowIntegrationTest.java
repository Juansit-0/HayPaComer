package dev.haypacomer.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import dev.haypacomer.sensors.esp32.Esp32Envelope;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class DemoFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();

  @TestConfiguration
  static class CapturingMail {

    @Bean
    @Primary
    EmailSender capturingEmailSender() {
      return OUTBOX::add;
    }
  }

  @Autowired private MockMvc mvc;
  @Autowired private CheckFridgeAlerts checkFridgeAlerts;

  private ResultActions call(HttpMethod method, String path, String bearer, String body)
      throws Exception {
    var builder = request(method, path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      builder.header("Authorization", bearer);
    }
    return mvc.perform(builder);
  }

  private String post(String path, String bearer, String body, int expected) throws Exception {
    return call(HttpMethod.POST, path, bearer, body)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String read(String path, String bearer) throws Exception {
    return call(HttpMethod.GET, path, bearer, "")
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String signUp(String email, String name) throws Exception {
    post(
        "/api/v1/auth/register",
        null,
        "{\"email\":\""
            + email
            + "\",\"password\":\"fresh-milk-842\",\"displayName\":\""
            + name
            + "\"}",
        201);
    return "Bearer "
        + JsonPath.read(
            post(
                "/api/v1/auth/login",
                null,
                "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}",
                200),
            "$.accessToken");
  }

  private record Kitchen(String base, String fridge, String tray) {}

  private Kitchen kitchen(String owner, String name) throws Exception {
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                post(
                    "/api/v1/households",
                    owner,
                    "{\"name\":\"" + name + "\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
                    201),
                "$.id");
    String fridge = post(base + "/fridges", owner, "{\"name\":\"Kitchen\"}", 201);
    return new Kitchen(
        base, JsonPath.read(fridge, "$.id"), JsonPath.read(fridge, "$.children[0].children[0].id"));
  }

  private String stock(Kitchen kitchen, String bearer, String food, String extra) throws Exception {
    return JsonPath.read(
        post(
            kitchen.base() + "/items",
            bearer,
            "{\"fridgeId\":\""
                + kitchen.fridge()
                + "\",\"trayId\":\""
                + kitchen.tray()
                + "\",\"food\":\""
                + food
                + "\","
                + extra
                + "}",
            201),
        "$.itemId");
  }

  private String device(Kitchen kitchen, String bearer, String name) throws Exception {
    return post(
        kitchen.base() + "/devices",
        bearer,
        "{\"fridgeId\":\""
            + kitchen.fridge()
            + "\",\"name\":\""
            + name
            + "\",\"kind\":\"SIMULATOR\"}",
        201);
  }

  private ResultActions events(String key, String json) throws Exception {
    return mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/device/events")
            .header("X-Device-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private static double gramsOf(String inventory, String food) {
    List<Number> grams = JsonPath.read(inventory, "$[?(@.name == '" + food + "')].grams");
    return grams.getFirst().doubleValue();
  }

  @Test
  void theAcceptanceDemoRunsEndToEnd() throws Exception {
    String juan = signUp("demo-juan@haypacomer.dev", "Juan");
    String ana = signUp("demo-ana@haypacomer.dev", "Ana");
    Kitchen kitchen = kitchen(juan, "Apartment 402");
    String base = kitchen.base();
    post(
        base + "/invitations",
        juan,
        "{\"email\":\"demo-ana@haypacomer.dev\",\"role\":\"MEMBER\"}",
        201);
    String link = OUTBOX.getLast().link();
    String token =
        URLDecoder.decode(link.substring(link.indexOf("#token=") + 7), StandardCharsets.UTF_8);
    post("/api/v1/invitations/accept", ana, "{\"token\":\"" + token + "\"}", 200);

    String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();
    String milk = stock(kitchen, juan, "Milk", "\"grams\":892,\"tareGrams\":50");
    String yogurt = stock(kitchen, juan, "Yogurt", "\"grams\":125,\"visibility\":\"PRIVATE\"");
    String chicken =
        stock(kitchen, juan, "Chicken breast", "\"grams\":80,\"expiresOn\":\"" + tomorrow + "\"");
    stock(kitchen, juan, "Tuna", "\"grams\":300");
    stock(kitchen, juan, "Rice", "\"grams\":900");

    post(base + "/items/" + yogurt + "/consume", ana, "{\"grams\":50}", 403);
    String anaId = JsonPath.read(read("/api/v1/me", ana), "$.id");
    post(base + "/items/" + yogurt + "/grants", juan, "{\"userId\":\"" + anaId + "\"}", 200);
    post(base + "/items/" + yogurt + "/consume", ana, "{\"grams\":50}", 200);

    String door = device(kitchen, juan, "Door");
    String doorKey = JsonPath.read(door, "$.apiKey");
    Esp32Simulator doorSensor = new Esp32Simulator("door-01");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    events(
            doorKey,
            doorSensor.toJson(
                List.of(
                    doorSensor.door(true, now.minusSeconds(45)),
                    doorSensor.temperature("4.1", now.minusSeconds(40)))))
        .andExpect(status().isAccepted());
    checkFridgeAlerts.check();
    call(HttpMethod.GET, "/api/v1/device/commands", null, "").andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/device/commands").header("X-Device-Key", doorKey))
        .andExpect(jsonPath("$", hasItem("DOOR_OPEN_BEEP")));
    call(HttpMethod.GET, "/api/v1/notifications", ana, "")
        .andExpect(jsonPath("$[*].type", hasItem("DOOR_LEFT_OPEN")));

    String scale = device(kitchen, juan, "Scale");
    String scaleKey = JsonPath.read(scale, "$.apiKey");
    call(
            HttpMethod.PUT,
            base + "/devices/" + JsonPath.read(scale, "$.device.id") + "/scale/item",
            juan,
            "{\"itemId\":\"" + milk + "\"}")
        .andExpect(status().isOk());
    Esp32Simulator scaleSensor = new Esp32Simulator("scale-01");
    events(scaleKey, scaleSensor.toJson(List.of(scaleSensor.weight("700", true, "FRIDGE", now))))
        .andExpect(status().isAccepted());
    assertEquals(650.0, gramsOf(read(base + "/inventory", juan), "Milk"));

    String run =
        post(
            base + "/agent/runs",
            juan,
            "{\"goal\":\"Organize dinner and what to buy so nothing goes to waste\"}",
            201);
    List<String> specialists = JsonPath.read(run, "$.runs[*].specialist");
    assertEquals(2, specialists.size());
    String runId = JsonPath.read(run, "$.runs[0].id");
    call(HttpMethod.GET, "/api/v1/agent/runs/" + runId + "/trace", juan, "")
        .andExpect(jsonPath("$.steps[*].kind", hasItem("TOOL_CALL")));

    String recipe =
        "{\"name\":\"Rice with chicken\",\"servings\":2,\"minutes\":35,\"targetServings\":2,"
            + "\"requirements\":[{\"food\":\"Chicken breast\",\"grams\":200},"
            + "{\"food\":\"Rice\",\"quantity\":\"150 g\"}]";
    call(HttpMethod.POST, base + "/recipes/evaluate", juan, recipe + ",\"strategy\":\"FLEXIBLE\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("REDUCE"))
        .andExpect(jsonPath("$.achievableServings").value(1));
    call(HttpMethod.POST, base + "/recipes/evaluate", juan, recipe + ",\"strategy\":\"RESCUE\"}")
        .andExpect(jsonPath("$.verdict").value("SUBSTITUTE"))
        .andExpect(jsonPath("$.requirements[0].substitute").value("Tuna"));
    String anaMember =
        JsonPath.<List<String>>read(
                read(base, juan), "$.members[?(@.userId == '" + anaId + "')].memberId")
            .getFirst();
    call(
            HttpMethod.PUT,
            base + "/profile",
            ana,
            "{\"diet\":\"OMNIVORE\",\"allergies\":[\"FISH\"],\"avoidedFoods\":[]}")
        .andExpect(status().isOk());
    call(
            HttpMethod.POST,
            base + "/recipes/evaluate",
            juan,
            recipe + ",\"strategy\":\"RESCUE\",\"diners\":[\"" + anaMember + "\"]}")
        .andExpect(jsonPath("$.requirements[0].substitute").doesNotExist());

    String session =
        post(
            base + "/cooking-sessions",
            juan,
            recipe
                + ",\"steps\":[{\"instruction\":\"Rinse the rice\"},{\"instruction\":\"Cook\"}]}",
            201);
    String sessionId = JsonPath.read(session, "$.id");
    call(HttpMethod.POST, base + "/cooking-sessions/" + sessionId + "/next", juan, "")
        .andExpect(jsonPath("$.phase").value("COOKING"));
    call(HttpMethod.POST, base + "/cooking-sessions/" + sessionId + "/next", juan, "")
        .andExpect(jsonPath("$.currentStep").value(2));
    post(base + "/items/" + chicken + "/consume", juan, "{\"grams\":80}", 200);

    mvc.perform(
            multipart(base + "/recipes/from-photo")
                .file(
                    new MockMultipartFile(
                        "photo",
                        "card.jpg",
                        "image/jpeg",
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}))
                .header("Authorization", juan))
        .andExpect(status().isServiceUnavailable());
    call(HttpMethod.GET, "/api/v1/status", juan, "").andExpect(jsonPath("$.state").value("OK"));
    post(base + "/agent/chat", ana, "{\"message\":\"Que cocino esta noche?\"}", 200);

    call(HttpMethod.GET, base + "/analytics", juan, "")
        .andExpect(jsonPath("$.total.rescuedGrams").value(80.0))
        .andExpect(jsonPath("$.total.consumedGrams").value(greaterThan(300.0)))
        .andExpect(jsonPath("$.moneySaved").value(greaterThan(0.0)));
    call(HttpMethod.GET, base + "/market-list", juan, "").andExpect(status().isOk());
  }

  @Test
  void extremeSensorNoiseNeverCorruptsTheStock() throws Exception {
    String juan = signUp("noise@haypacomer.dev", "Juan");
    Kitchen kitchen = kitchen(juan, "Noisy");
    String milk = stock(kitchen, juan, "Milk", "\"grams\":892,\"tareGrams\":50");
    String sensor = device(kitchen, juan, "Everything");
    String key = JsonPath.read(sensor, "$.apiKey");
    call(
            HttpMethod.PUT,
            kitchen.base() + "/devices/" + JsonPath.read(sensor, "$.device.id") + "/scale/item",
            juan,
            "{\"itemId\":\"" + milk + "\"}")
        .andExpect(status().isOk());

    Esp32Simulator noisy = new Esp32Simulator("noise-01");
    Random random = new Random(842);
    Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(600);
    List<Esp32Envelope> batch = new ArrayList<>();
    for (int index = 0; batch.size() < 480; index++) {
      Instant at = start.plusSeconds(random.nextInt(590));
      switch (index % 6) {
        case 0 -> batch.add(noisy.temperature(String.valueOf(3 + random.nextInt(2)), at));
        case 1 -> batch.add(noisy.temperature(random.nextBoolean() ? "99" : "-45", at));
        case 2 ->
            batch.add(
                noisy.weight(
                    String.valueOf(892 - random.nextInt(3)), random.nextInt(4) == 0, "FRIDGE", at));
        case 3 ->
            batch.add(noisy.weight(String.valueOf(random.nextInt(5000)), false, "FRIDGE", at));
        case 4 -> batch.add(noisy.door(random.nextBoolean(), at));
        default -> batch.add(noisy.temperature("4.0", Instant.now().plusSeconds(3600)));
      }
    }
    batch.addAll(batch.subList(0, 20));

    String report =
        events(key, noisy.toJson(batch))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int accepted = JsonPath.read(report, "$.accepted");
    int rejected = JsonPath.read(report, "$.rejected");
    int duplicates = JsonPath.read(report, "$.duplicates");
    assertEquals(500, accepted + rejected + duplicates + (int) JsonPath.read(report, "$.dropped"));
    assertTrue(rejected >= 160, "rejected " + rejected);
    int again =
        JsonPath.read(
            events(key, noisy.toJson(batch))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.duplicates");
    assertTrue(again >= accepted, "duplicates " + again + " accepted " + accepted);
    events(key, "{\"type\":").andExpect(status().isBadRequest());

    assertEquals(842.0, gramsOf(read(kitchen.base() + "/inventory", juan), "Milk"));
    assertEquals(
        0,
        JsonPath.<List<?>>read(
                read(kitchen.base() + "/activity", juan), "$[?(@.action == 'CONSUME_FOOD')]")
            .size());
  }

  @Test
  void concurrentUseOfTheSameFoodNeverLosesGrams() throws Exception {
    String juan = signUp("rush@haypacomer.dev", "Juan");
    Kitchen kitchen = kitchen(juan, "Rush hour");
    String milk = stock(kitchen, juan, "Milk", "\"grams\":842");
    ExecutorService pool = Executors.newFixedThreadPool(8);
    List<Future<Integer>> results = new ArrayList<>();
    for (int index = 0; index < 20; index++) {
      results.add(
          pool.submit(
              () ->
                  call(
                          HttpMethod.POST,
                          kitchen.base() + "/items/" + milk + "/consume",
                          juan,
                          "{\"grams\":50}")
                      .andReturn()
                      .getResponse()
                      .getStatus()));
    }
    int succeeded = 0;
    for (Future<Integer> result : results) {
      int code = result.get();
      assertTrue(code == 200 || code == 400 || code == 404, "status " + code);
      if (code == 200) {
        succeeded++;
      }
    }
    pool.shutdown();

    String inventory = read(kitchen.base() + "/inventory", juan);
    double left = gramsOf(inventory, "Milk");
    assertEquals(16, succeeded);
    assertEquals(842.0 - 50.0 * succeeded, left);
    assertTrue(left >= 0);
    int logged =
        JsonPath.<List<?>>read(
                read(kitchen.base() + "/activity?limit=50", juan),
                "$[?(@.action == 'CONSUME_FOOD')]")
            .size();
    assertEquals(succeeded, logged);
  }
}
