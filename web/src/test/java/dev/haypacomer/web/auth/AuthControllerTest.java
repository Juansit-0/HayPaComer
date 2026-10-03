package dev.haypacomer.web.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.haypacomer.application.auth.AccessToken;
import dev.haypacomer.application.auth.AuthTokens;
import dev.haypacomer.application.auth.EmailAlreadyRegisteredException;
import dev.haypacomer.application.auth.GetUserProfile;
import dev.haypacomer.application.auth.InvalidCredentialsException;
import dev.haypacomer.application.auth.InvalidPasswordException;
import dev.haypacomer.application.auth.LogIn;
import dev.haypacomer.application.auth.LogOut;
import dev.haypacomer.application.auth.RefreshSession;
import dev.haypacomer.application.auth.RefreshTokenReuseException;
import dev.haypacomer.application.auth.RegisterUser;
import dev.haypacomer.application.auth.TooManyLoginAttemptsException;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.web.error.ApiExceptionHandler;
import dev.haypacomer.web.security.JwtAccessTokenIssuer;
import dev.haypacomer.web.security.JwtProperties;
import dev.haypacomer.web.security.SecurityConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, MeController.class})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(
    properties = {
      "haypacomer.security.jwt.secret=test-secret-with-at-least-32-bytes!!",
      "haypacomer.security.jwt.issuer=https://api.haypacomer.dev",
      "haypacomer.security.jwt.access-token-time-to-live=15m"
    })
class AuthControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  @Autowired private MockMvc mvc;
  @Autowired private JwtEncoder encoder;
  @Autowired private JwtProperties properties;

  @MockitoBean private RegisterUser registerUser;
  @MockitoBean private LogIn logIn;
  @MockitoBean private RefreshSession refreshSession;
  @MockitoBean private LogOut logOut;
  @MockitoBean private GetUserProfile getUserProfile;

  private final User juan =
      User.register(
          new EmailAddress("juan@haypacomer.dev"), new PasswordHash("$2a$12$hash"), "Juan", NOW);

  private AuthTokens tokens() {
    return new AuthTokens(
        juan.id(), new AccessToken("access", NOW.plusSeconds(900)), "refresh", NOW.plusSeconds(60));
  }

  private String bearer() {
    return "Bearer "
        + new JwtAccessTokenIssuer(encoder, properties).issue(juan.id(), Instant.now()).value();
  }

  @Test
  void registersAndReturnsTheUserWithoutSecrets() throws Exception {
    when(registerUser.register(any())).thenReturn(juan);

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"juan@haypacomer.dev","password":"fresh-milk-842","displayName":"Juan"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("juan@haypacomer.dev"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void validatesRegisterInput() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nope\",\"password\":\"\",\"displayName\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void mapsDomainErrorsToProblemDetails() throws Exception {
    when(registerUser.register(any())).thenThrow(new EmailAlreadyRegisteredException());
    String body =
        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}";

    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Email already registered"));

    doThrow(new InvalidPasswordException("Too short")).when(registerUser).register(any());
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Too short"));
  }

  @Test
  void loginReturnsBearerTokens() throws Exception {
    when(logIn.logIn(any())).thenReturn(tokens());

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.refreshToken").value("refresh"));
  }

  @Test
  void loginFailuresAreUnauthorizedOrRateLimited() throws Exception {
    String body = "{\"email\":\"juan@haypacomer.dev\",\"password\":\"wrong-password\"}";
    when(logIn.logIn(any())).thenThrow(new InvalidCredentialsException());

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());

    doThrow(new TooManyLoginAttemptsException()).when(logIn).logIn(any());
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void refreshRotatesAndReuseIsUnauthorized() throws Exception {
    when(refreshSession.refresh("refresh")).thenReturn(tokens());
    when(refreshSession.refresh("stolen")).thenThrow(new RefreshTokenReuseException());

    mvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"));
    mvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"stolen\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutReturnsNoContent() throws Exception {
    mvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh\"}"))
        .andExpect(status().isNoContent());

    verify(logOut).logOut(anyString());
  }

  @Test
  void meRequiresAValidBearerToken() throws Exception {
    when(getUserProfile.get(juan.id())).thenReturn(juan);

    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/me").header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(juan.id().value().toString()))
        .andExpect(jsonPath("$.displayName").value("Juan"));
  }
}
