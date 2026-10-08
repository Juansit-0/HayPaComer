package dev.haypacomer.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

final class ProblemSecurityResponses {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ProblemSecurityResponses() {}

  static AuthenticationEntryPoint unauthorized(String challenge) {
    return (request, response, exception) -> {
      response.setHeader("WWW-Authenticate", challenge);
      write(
          request,
          response,
          HttpStatus.UNAUTHORIZED,
          "Unauthorized",
          "Send valid credentials to use this endpoint");
    };
  }

  static AccessDeniedHandler forbidden() {
    return (request, response, exception) ->
        write(
            request,
            response,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "These credentials cannot use this endpoint");
  }

  static void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String title,
      String detail)
      throws IOException {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", ProblemTypes.of(title).toString());
    problem.put("title", title);
    problem.put("status", status.value());
    problem.put("detail", detail);
    problem.put("instance", URI.create(request.getRequestURI()).toString());
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(JSON.writeValueAsString(problem));
  }
}
