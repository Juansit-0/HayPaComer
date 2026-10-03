package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.audit.AuditEntry;
import dev.haypacomer.application.audit.ListActivity;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActivityController {

  private final ListActivity listActivity;

  public ActivityController(ListActivity listActivity) {
    this.listActivity = listActivity;
  }

  @GetMapping("/api/v1/households/{householdId}/activity")
  List<ActivityResponse> activity(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam(defaultValue = "20") int limit) {
    return listActivity.list(CurrentUser.of(jwt), new HouseholdId(householdId), limit).stream()
        .map(ActivityResponse::from)
        .toList();
  }

  record ActivityResponse(
      UUID actor,
      String action,
      String entity,
      UUID entityId,
      Map<String, String> detail,
      Instant at) {

    static ActivityResponse from(AuditEntry entry) {
      return new ActivityResponse(
          entry.actor().value(),
          entry.action(),
          entry.entity(),
          entry.entityId(),
          entry.detail(),
          entry.at());
    }
  }
}
