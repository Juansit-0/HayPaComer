package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.AuthTokens;
import dev.haypacomer.domain.identity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

final class AuthDtos {

  private AuthDtos() {}

  record RegisterRequest(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 128) String password,
      @NotBlank @Size(max = 80) String displayName) {

    @Override
    public String toString() {
      return "RegisterRequest[email=" + email + "]";
    }
  }

  record LoginRequest(@NotBlank String email, @NotBlank String password) {

    @Override
    public String toString() {
      return "LoginRequest[email=" + email + "]";
    }
  }

  record RefreshRequest(@NotBlank String refreshToken) {

    @Override
    public String toString() {
      return "RefreshRequest[protected]";
    }
  }

  record TokenRequest(@NotBlank String token) {

    @Override
    public String toString() {
      return "TokenRequest[protected]";
    }
  }

  record ForgotPasswordRequest(@NotBlank String email) {}

  record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
      return "ResetPasswordRequest[protected]";
    }
  }

  record UserResponse(UUID id, String email, String displayName, boolean emailVerified) {

    static UserResponse from(User user) {
      return new UserResponse(
          user.id().value(), user.email().value(), user.displayName(), user.emailVerified());
    }
  }

  record TokenResponse(
      String tokenType,
      String accessToken,
      Instant accessTokenExpiresAt,
      String refreshToken,
      Instant refreshTokenExpiresAt) {

    static TokenResponse from(AuthTokens tokens) {
      return new TokenResponse(
          "Bearer",
          tokens.accessToken().value(),
          tokens.accessToken().expiresAt(),
          tokens.refreshToken(),
          tokens.refreshExpiresAt());
    }

    @Override
    public String toString() {
      return "TokenResponse[accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
  }
}
