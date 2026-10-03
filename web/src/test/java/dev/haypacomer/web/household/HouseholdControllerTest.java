package dev.haypacomer.web.household;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.household.ChangeMemberRole;
import dev.haypacomer.application.household.CreateHousehold;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.household.ListHouseholds;
import dev.haypacomer.application.household.RemoveMember;
import dev.haypacomer.application.household.TransferOwnership;
import dev.haypacomer.application.household.UpdateHousehold;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.error.ApiExceptionHandler;
import dev.haypacomer.web.security.JwtAccessTokenIssuer;
import dev.haypacomer.web.security.JwtProperties;
import dev.haypacomer.web.security.SecurityConfiguration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(controllers = HouseholdController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(
    properties = {
      "haypacomer.security.jwt.secret=test-secret-with-at-least-32-bytes!!",
      "haypacomer.security.jwt.issuer=https://api.haypacomer.dev",
      "haypacomer.security.jwt.access-token-time-to-live=15m"
    })
class HouseholdControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private AuthenticateDevice authenticateDevice;
  @Autowired private JwtEncoder encoder;
  @Autowired private JwtProperties properties;

  @MockitoBean private CreateHousehold createHousehold;
  @MockitoBean private ListHouseholds listHouseholds;
  @MockitoBean private GetHousehold getHousehold;
  @MockitoBean private UpdateHousehold updateHousehold;
  @MockitoBean private ChangeMemberRole changeMemberRole;
  @MockitoBean private RemoveMember removeMember;
  @MockitoBean private TransferOwnership transferOwnership;

  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household apartment;

  @BeforeEach
  void createHousehold() {
    apartment =
        Household.create(
            "Apartment 402", Currency.getInstance("COP"), ZoneId.of("America/Bogota"), juan, NOW);
    apartment.join(ana, Role.MEMBER, NOW);
  }

  private String bearer(UserId user) {
    return "Bearer "
        + new JwtAccessTokenIssuer(encoder, properties).issue(user, Instant.now()).value();
  }

  private String path() {
    return "/api/v1/households/" + apartment.id().value();
  }

  @Test
  void requiresAuthentication() throws Exception {
    mvc.perform(get("/api/v1/households")).andExpect(status().isUnauthorized());
  }

  @Test
  void createsAHouseholdForTheCaller() throws Exception {
    when(createHousehold.create(eq(juan), any())).thenReturn(apartment);

    mvc.perform(
            post("/api/v1/households")
                .header("Authorization", bearer(juan))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Apartment 402\",\"currency\":\"COP\",\"timezone\":\"America/Bogota\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.myRole").value("OWNER"))
        .andExpect(jsonPath("$.members.length()").value(2));
  }

  @Test
  void validatesCreateInput() throws Exception {
    mvc.perform(
            post("/api/v1/households")
                .header("Authorization", bearer(juan))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"currency\":\"PESOS\",\"timezone\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listsAndShowsHouseholdsWithTheViewersRole() throws Exception {
    when(listHouseholds.list(ana)).thenReturn(List.of(apartment));
    when(getHousehold.get(eq(ana), any())).thenReturn(apartment);

    mvc.perform(get("/api/v1/households").header("Authorization", bearer(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].myRole").value("MEMBER"));
    mvc.perform(get(path()).header("Authorization", bearer(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("COP"))
        .andExpect(jsonPath("$.timezone").value("America/Bogota"));
  }

  @Test
  void hidesHouseholdsFromNonMembers() throws Exception {
    UserId stranger = UserId.newId();
    when(getHousehold.get(eq(stranger), any())).thenThrow(new HouseholdNotFoundException());

    mvc.perform(get(path()).header("Authorization", bearer(stranger)))
        .andExpect(status().isNotFound());
  }

  @Test
  void mapsPermissionAndInvariantErrors() throws Exception {
    when(updateHousehold.update(eq(ana), any(), any()))
        .thenThrow(new AccessDeniedException("Missing permission"));
    doThrow(new IllegalStateException("The owner cannot leave"))
        .when(removeMember)
        .remove(eq(juan), any(), eq(juan));

    mvc.perform(
            patch(path())
                .header("Authorization", bearer(ana))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mine\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(delete(path() + "/members/" + juan.value()).header("Authorization", bearer(juan)))
        .andExpect(status().isConflict());
  }

  @Test
  void managesMembersAndOwnership() throws Exception {
    when(changeMemberRole.change(eq(juan), any(), eq(ana), eq(Role.GUEST)))
        .thenReturn(apartment.membershipOf(ana).orElseThrow().withRole(Role.GUEST));
    when(transferOwnership.transfer(eq(juan), any(), eq(ana))).thenReturn(apartment);

    mvc.perform(
            patch(path() + "/members/" + ana.value())
                .header("Authorization", bearer(juan))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("GUEST"));
    mvc.perform(delete(path() + "/members/" + ana.value()).header("Authorization", bearer(juan)))
        .andExpect(status().isNoContent());
    mvc.perform(
            put(path() + "/owner")
                .header("Authorization", bearer(juan))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + ana.value() + "\"}"))
        .andExpect(status().isOk());
  }
}
