package dev.haypacomer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class RenderBlueprintTest {

  @SuppressWarnings("unchecked")
  private static Map<String, Object> blueprint() throws IOException {
    try (Reader reader = Files.newBufferedReader(Path.of("../render.yaml"))) {
      return new Yaml().load(reader);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> service(String type) throws IOException {
    return ((List<Map<String, Object>>) blueprint().get("services"))
        .stream().filter(service -> type.equals(service.get("type"))).findFirst().orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Map<String, Object>> variables() throws IOException {
    Map<String, Map<String, Object>> byKey = new java.util.HashMap<>();
    ((List<Map<String, Object>>) service("web").get("envVars"))
        .forEach(variable -> byKey.put((String) variable.get("key"), variable));
    return byKey;
  }

  @Test
  void theWebServiceDeploysTheImageOnlyAfterChecksPass() throws IOException {
    Map<String, Object> web = service("web");

    assertEquals("docker", web.get("runtime"));
    assertEquals("/actuator/health/readiness", web.get("healthCheckPath"));
    assertEquals("checksPass", web.get("autoDeployTrigger"));
    assertEquals("noeviction", service("keyvalue").get("maxmemoryPolicy"));
    assertEquals(List.of(), service("keyvalue").get("ipAllowList"));
  }

  @Test
  void secretsNeverLiveInTheRepository() throws IOException {
    Map<String, Map<String, Object>> variables = variables();

    assertEquals(true, variables.get("JWT_SECRET").get("generateValue"));
    assertEquals(false, variables.get("GEMINI_API_KEY").get("sync"));
    assertEquals(false, variables.get("TELEGRAM_BOT_TOKEN").get("sync"));
    assertTrue(variables.get("DB_PASSWORD").containsKey("fromDatabase"));
    assertTrue(variables.get("REDIS_URL").containsKey("fromService"));
    variables.values().stream()
        .filter(variable -> variable.containsKey("value"))
        .forEach(
            variable ->
                assertFalse(
                    String.valueOf(variable.get("key")).matches(".*(SECRET|PASSWORD|KEY|TOKEN).*"),
                    "literal value for " + variable.get("key")));
  }
}
