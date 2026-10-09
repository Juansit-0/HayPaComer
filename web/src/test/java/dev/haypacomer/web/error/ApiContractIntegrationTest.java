package dev.haypacomer.web.error;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
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
class ApiContractIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  @Test
  void publishesTheOpenApiCatalogWithBothAuthSchemes() throws Exception {
    String docs =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("HayPaComer API"))
            .andExpect(jsonPath("$.components.securitySchemes", hasKey("bearer")))
            .andExpect(jsonPath("$.components.securitySchemes", hasKey("deviceKey")))
            .andExpect(jsonPath("$.paths", hasKey("/api/v1/households/{householdId}/weekly-plans")))
            .andExpect(jsonPath("$.paths", hasKey("/api/v1/device/events")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    int paths = JsonPath.<Map<String, Object>>read(docs, "$.paths").size();
    assertTrue(paths > 60);
    mvc.perform(get("/docs")).andExpect(status().is3xxRedirection());
  }

  @Test
  void exposesHealthProbesWithoutDetails() throws Exception {
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components").doesNotExist());
    mvc.perform(get("/actuator/info"))
        .andExpect(jsonPath("$.app.name").value("HayPaComer"))
        .andExpect(jsonPath("$.build.version").value("1.0.0"))
        .andExpect(jsonPath("$.build.artifact").value("web"));
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @Test
  void everyErrorIsAProblemDocument() throws Exception {
    mvc.perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.type").value("https://haypacomer.dev/problems/unauthorized"))
        .andExpect(jsonPath("$.instance").value("/api/v1/me"));
    mvc.perform(get("/api/v1/device/whoami"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string("WWW-Authenticate", "X-Device-Key"));
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.errors", hasKey("email")));
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("The request body is not valid JSON"));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ghost@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type").value("https://haypacomer.dev/problems/invalid-credentials"));
  }
}
