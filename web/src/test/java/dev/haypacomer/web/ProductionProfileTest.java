package dev.haypacomer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProductionProfileTest {

  private static PropertySource<?> production() throws IOException {
    List<PropertySource<?>> sources =
        new YamlPropertySourceLoader().load("prod", new ClassPathResource("application-prod.yml"));
    return sources.getFirst();
  }

  @Test
  void productionListensOnThePlatformPortBehindItsProxy() throws IOException {
    PropertySource<?> prod = production();

    assertEquals("${PORT:8080}", String.valueOf(prod.getProperty("server.port")));
    assertEquals("framework", String.valueOf(prod.getProperty("server.forward-headers-strategy")));
    assertEquals("graceful", String.valueOf(prod.getProperty("server.shutdown")));
    assertEquals(false, prod.getProperty("haypacomer.mail.log-links"));
  }

  @Test
  void productionTakesDatabaseAndRedisOnlyFromTheEnvironment() throws IOException {
    PropertySource<?> prod = production();

    assertEquals(
        "jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}?sslmode=${DB_SSLMODE:prefer}",
        String.valueOf(prod.getProperty("spring.datasource.url")));
    assertEquals("${DB_PASSWORD}", String.valueOf(prod.getProperty("spring.datasource.password")));
    assertEquals("${REDIS_URL}", String.valueOf(prod.getProperty("spring.data.redis.url")));
  }

  @Test
  void theImageRunsTheProductionProfileAsAnUnprivilegedUser() throws IOException {
    String dockerfile = Files.readString(Path.of("../Dockerfile"));
    String ignored = Files.readString(Path.of("../.dockerignore"));

    assertTrue(dockerfile.contains("SPRING_PROFILES_ACTIVE=prod"));
    assertTrue(dockerfile.contains("USER haypacomer"));
    assertTrue(dockerfile.contains("/actuator/health/readiness"));
    assertTrue(ignored.contains(".env"));
    assertTrue(ignored.contains(".demo-credentials.json"));
  }
}
