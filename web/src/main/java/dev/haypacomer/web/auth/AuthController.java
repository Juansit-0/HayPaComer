package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.LogIn;
import dev.haypacomer.application.auth.LogOut;
import dev.haypacomer.application.auth.LoginCommand;
import dev.haypacomer.application.auth.RefreshSession;
import dev.haypacomer.application.auth.RegisterCommand;
import dev.haypacomer.application.auth.RegisterUser;
import dev.haypacomer.web.auth.AuthDtos.LoginRequest;
import dev.haypacomer.web.auth.AuthDtos.RefreshRequest;
import dev.haypacomer.web.auth.AuthDtos.RegisterRequest;
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

  public AuthController(
      RegisterUser registerUser, LogIn logIn, RefreshSession refreshSession, LogOut logOut) {
    this.registerUser = registerUser;
    this.logIn = logIn;
    this.refreshSession = refreshSession;
    this.logOut = logOut;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return UserResponse.from(
        registerUser.register(
            new RegisterCommand(request.email(), request.password(), request.displayName())));
  }

  @PostMapping("/login")
  TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return TokenResponse.from(logIn.logIn(new LoginCommand(request.email(), request.password())));
  }

  @PostMapping("/refresh")
  TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return TokenResponse.from(refreshSession.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(@Valid @RequestBody RefreshRequest request) {
    logOut.logOut(request.refreshToken());
  }
}
