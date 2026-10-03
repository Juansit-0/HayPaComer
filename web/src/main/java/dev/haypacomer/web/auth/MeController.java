package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.GetUserProfile;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.auth.AuthDtos.UserResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final GetUserProfile getUserProfile;

  public MeController(GetUserProfile getUserProfile) {
    this.getUserProfile = getUserProfile;
  }

  @GetMapping
  UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return UserResponse.from(getUserProfile.get(new UserId(UUID.fromString(jwt.getSubject()))));
  }
}
