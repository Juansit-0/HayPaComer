package dev.haypacomer.web.security;

import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.InvalidDeviceKeyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class DeviceKeyAuthenticationFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Device-Key";

  private final AuthenticateDevice authenticateDevice;

  public DeviceKeyAuthenticationFilter(AuthenticateDevice authenticateDevice) {
    this.authenticateDevice = authenticateDevice;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new DeviceAuthentication(authenticateDevice.authenticate(request.getHeader(HEADER))));
    } catch (InvalidDeviceKeyException exception) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response
          .getWriter()
          .write(
              "{\"type\":\"about:blank\",\"title\":\"Invalid device key\",\"status\":401,"
                  + "\"detail\":\"Send a valid X-Device-Key header\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
