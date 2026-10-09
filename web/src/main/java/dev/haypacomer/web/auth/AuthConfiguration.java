package dev.haypacomer.web.auth;

import dev.haypacomer.application.auth.AuthSettings;
import dev.haypacomer.application.auth.GetUserProfile;
import dev.haypacomer.application.auth.LogIn;
import dev.haypacomer.application.auth.LogOut;
import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.auth.RefreshSession;
import dev.haypacomer.application.auth.RegisterUser;
import dev.haypacomer.application.auth.RequestEmailVerification;
import dev.haypacomer.application.auth.RequestPasswordReset;
import dev.haypacomer.application.auth.ResetPassword;
import dev.haypacomer.application.auth.SessionIssuer;
import dev.haypacomer.application.auth.VerifyEmail;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.AccessTokenIssuer;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.LoginAttemptLog;
import dev.haypacomer.application.port.PasswordHasher;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.web.security.BCryptPasswordHasher;
import dev.haypacomer.web.security.JwtAccessTokenIssuer;
import dev.haypacomer.web.security.JwtProperties;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
public class AuthConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  @DependsOn("flywayInitializer")
  AuthSettings authSettings(PolicySource policies) {
    return policies.auth();
  }

  @Bean
  OpaqueTokens opaqueTokens() {
    return new OpaqueTokens();
  }

  @Bean
  PasswordHasher passwordHasher() {
    return new BCryptPasswordHasher();
  }

  @Bean
  AccessTokenIssuer accessTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
    return new JwtAccessTokenIssuer(encoder, properties);
  }

  @Bean
  SessionIssuer sessionIssuer(
      AccessTokenIssuer accessTokens,
      RefreshTokenStore refreshTokens,
      OpaqueTokens opaqueTokens,
      AuthSettings settings) {
    return new SessionIssuer(accessTokens, refreshTokens, opaqueTokens, settings);
  }

  @Bean
  RegisterUser registerUser(
      UserRepository users, PasswordHasher hasher, Clock clock, AuthSettings settings) {
    return new RegisterUser(users, hasher, clock, settings);
  }

  @Bean
  LogIn logIn(
      UserRepository users,
      PasswordHasher hasher,
      LoginAttemptLog attempts,
      SessionIssuer sessions,
      Clock clock,
      AuthSettings settings) {
    return new LogIn(users, hasher, attempts, sessions, clock, settings);
  }

  @Bean
  RefreshSession refreshSession(
      RefreshTokenStore refreshTokens,
      UserRepository users,
      SessionIssuer sessions,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    return new RefreshSession(refreshTokens, users, sessions, opaqueTokens, clock);
  }

  @Bean
  LogOut logOut(RefreshTokenStore refreshTokens, OpaqueTokens opaqueTokens, Clock clock) {
    return new LogOut(refreshTokens, opaqueTokens, clock);
  }

  @Bean
  GetUserProfile getUserProfile(UserRepository users) {
    return new GetUserProfile(users);
  }

  @Bean
  RequestEmailVerification requestEmailVerification(
      UserRepository users,
      UserTokenStore tokens,
      OpaqueTokens opaqueTokens,
      EmailSender email,
      MailLinks links,
      Clock clock) {
    return new RequestEmailVerification(users, tokens, opaqueTokens, email, links, clock);
  }

  @Bean
  VerifyEmail verifyEmail(
      UserRepository users, UserTokenStore tokens, OpaqueTokens opaqueTokens, Clock clock) {
    return new VerifyEmail(users, tokens, opaqueTokens, clock);
  }

  @Bean
  RequestPasswordReset requestPasswordReset(
      UserRepository users,
      UserTokenStore tokens,
      OpaqueTokens opaqueTokens,
      EmailSender email,
      MailLinks links,
      Clock clock) {
    return new RequestPasswordReset(users, tokens, opaqueTokens, email, links, clock);
  }

  @Bean
  ResetPassword resetPassword(
      UserRepository users,
      UserTokenStore tokens,
      OpaqueTokens opaqueTokens,
      PasswordHasher hasher,
      RefreshTokenStore refreshTokens,
      Clock clock,
      AuthSettings settings) {
    return new ResetPassword(users, tokens, opaqueTokens, hasher, refreshTokens, clock, settings);
  }
}
