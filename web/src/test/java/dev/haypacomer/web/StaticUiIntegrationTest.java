package dev.haypacomer.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class StaticUiIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  @Test
  void servesTheInterfaceAndBrandAssetsWithoutSigningIn() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    mvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/app/main.js")));
    mvc.perform(get("/app/main.js")).andExpect(status().isOk());
    mvc.perform(get("/app/analytics.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Who rescued the most")));
    mvc.perform(get("/index.html")).andExpect(content().string(containsString("#/numbers")));
    mvc.perform(get("/index.html")).andExpect(content().string(containsString("#/chef")));
    mvc.perform(get("/app/chef.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/agent/chat")));
    mvc.perform(get("/app/voice.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("SpeechRecognition")));
    mvc.perform(get("/app/api.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("localStorage")))
        .andExpect(content().string(containsString("navigator.locks")))
        .andExpect(content().string(not(containsString("sessionStorage"))));
    mvc.perform(get("/app/app.css")).andExpect(status().isOk());
    mvc.perform(get("/brand/tokens.css"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("--color-primary")));
    mvc.perform(get("/brand/logo-mark.svg")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/households")).andExpect(status().isUnauthorized());
    mvc.perform(get("/app/../api/v1/me")).andExpect(status().is4xxClientError());
  }
}
