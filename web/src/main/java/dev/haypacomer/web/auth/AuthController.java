package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.LogIn;
import dev.haypacomer.application.auth.LogOut;
import dev.haypacomer.application.auth.LoginCommand;
import dev.haypacomer.application.auth.RefreshSession;
import dev.haypacomer.application.auth.RegisterCommand;
import dev.haypacomer.application.auth.RegisterUser;
import dev.haypacomer.application.auth.RequestEmailVerification;
import dev.haypacomer.application.auth.RequestPasswordReset;
import dev.haypacomer.application.auth.ResetPassword;
import dev.haypacomer.application.auth.VerifyEmail;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.web.auth.AuthDtos.ForgotPasswordRequest;
import dev.haypacomer.web.auth.AuthDtos.LoginRequest;
import dev.haypacomer.web.auth.AuthDtos.RefreshRequest;
import dev.haypacomer.web.auth.AuthDtos.RegisterRequest;
import dev.haypacomer.web.auth.AuthDtos.ResetPasswordRequest;
import dev.haypacomer.web.auth.AuthDtos.TokenRequest;
import dev.haypacomer.web.auth.AuthDtos.TokenResponse;
import dev.haypacomer.web.auth.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final RegisterUser registerUser;
  private final LogIn logIn;
  private final RefreshSession refreshSession;
  private final LogOut logOut;
  private final RequestEmailVerification requestEmailVerification;
  private final VerifyEmail verifyEmail;
  private final RequestPasswordReset requestPasswordReset;
  private final ResetPassword resetPassword;

  public AuthController(
      RegisterUser registerUser,
      LogIn logIn,
      RefreshSession refreshSession,
      LogOut logOut,
      RequestEmailVerification requestEmailVerification,
      VerifyEmail verifyEmail,
      RequestPasswordReset requestPasswordReset,
      ResetPassword resetPassword) {
    this.registerUser = registerUser;
    this.logIn = logIn;
    this.refreshSession = refreshSession;
    this.logOut = logOut;
    this.requestEmailVerification = requestEmailVerification;
    this.verifyEmail = verifyEmail;
    this.requestPasswordReset = requestPasswordReset;
    this.resetPassword = resetPassword;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  UserResponse register(@Valid @RequestBody RegisterRequest request) {
    User user =
        registerUser.register(
            new RegisterCommand(request.email(), request.password(), request.displayName()));
    requestEmailVerification.request(user.id());
    return UserResponse.from(user);
  }

  @PostMapping("/login")
  TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return TokenResponse.from(logIn.logIn(new LoginCommand(request.email(), request.password())));
  }

  @PostMapping("/refresh")
  TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return TokenResponse.from(refreshSession.refresh(request.refreshToken()));
  }

  @PostMapping("/verify-email")
  UserResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
    return UserResponse.from(verifyEmail.verify(request.token()));
  }

  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    requestPasswordReset.request(request.email());
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    resetPassword.reset(request.token(), request.password());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(@Valid @RequestBody RefreshRequest request) {
    logOut.logOut(request.refreshToken());
  }
}
