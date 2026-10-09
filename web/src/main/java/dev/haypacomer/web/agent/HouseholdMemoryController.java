package dev.haypacomer.web.agent;

import dev.haypacomer.application.agent.ClearHouseholdMemory;
import dev.haypacomer.application.agent.ForgetForHousehold;
import dev.haypacomer.application.agent.MemoryNote;
import dev.haypacomer.application.agent.MemoryTopic;
import dev.haypacomer.application.agent.RememberForHousehold;
import dev.haypacomer.application.agent.ViewHouseholdMemory;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/agent/memory")
public class HouseholdMemoryController {

  private final ViewHouseholdMemory view;
  private final RememberForHousehold remember;
  private final ForgetForHousehold forget;
  private final ClearHouseholdMemory clear;

  public HouseholdMemoryController(
      ViewHouseholdMemory view,
      RememberForHousehold remember,
      ForgetForHousehold forget,
      ClearHouseholdMemory clear) {
    this.view = view;
    this.remember = remember;
    this.forget = forget;
    this.clear = clear;
  }

  @GetMapping
  List<NoteResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return view.view(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(NoteResponse::from)
        .toList();
  }

  @PatchMapping
  List<NoteResponse> edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody EditRequest request) {
    UserId actor = CurrentUser.of(jwt);
    HouseholdId household = new HouseholdId(householdId);
    List<NoteRequest> forgotten = request.forget() == null ? List.of() : request.forget();
    List<NoteRequest> remembered = request.remember() == null ? List.of() : request.remember();
    forgotten.forEach(note -> forget.forget(actor, household, note.topic(), note.subject()));
    remembered.stream()
        .map(note -> new MemoryNote(note.topic(), note.subject(), note.valueOrEmpty()))
        .toList()
        .forEach(note -> remember.remember(actor, household, note));
    return list(jwt, householdId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void clear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    clear.clear(CurrentUser.of(jwt), new HouseholdId(householdId));
  }

  record EditRequest(
      @Valid @Size(max = 20) List<NoteRequest> remember,
      @Valid @Size(max = 20) List<NoteRequest> forget) {}

  record NoteRequest(@NotNull MemoryTopic topic, @NotBlank String subject, String value) {

    String valueOrEmpty() {
      return value == null ? "" : value;
    }
  }

  record NoteResponse(MemoryTopic topic, String subject, String value) {

    static NoteResponse from(MemoryNote note) {
      return new NoteResponse(note.topic(), note.subject(), note.value());
    }
  }
}
