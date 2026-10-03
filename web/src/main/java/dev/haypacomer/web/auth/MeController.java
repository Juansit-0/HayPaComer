package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.GetUserProfile;
import dev.haypacomer.application.auth.RequestEmailVerification;
import dev.haypacomer.web.auth.AuthDtos.UserResponse;
import dev.haypacomer.web.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final GetUserProfile getUserProfile;
  private final RequestEmailVerification requestEmailVerification;

  public MeController(
      GetUserProfile getUserProfile, RequestEmailVerification requestEmailVerification) {
    this.getUserProfile = getUserProfile;
    this.requestEmailVerification = requestEmailVerification;
  }

  @PostMapping("/email-verification")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void resendVerification(@AuthenticationPrincipal Jwt jwt) {
    requestEmailVerification.request(CurrentUser.of(jwt));
  }

  @GetMapping
  UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return UserResponse.from(getUserProfile.get(CurrentUser.of(jwt)));
  }
}
