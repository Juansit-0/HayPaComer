package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.InventorySnapshot;
import dev.haypacomer.application.inventory.ListSnapshots;
import dev.haypacomer.application.inventory.RestoreSnapshot;
import dev.haypacomer.application.inventory.SnapshotKind;
import dev.haypacomer.application.inventory.TakeSnapshot;
import dev.haypacomer.application.inventory.UndoLastChange;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class HistoryController {

  private final UndoLastChange undoLastChange;
  private final TakeSnapshot takeSnapshot;
  private final ListSnapshots listSnapshots;
  private final RestoreSnapshot restoreSnapshot;

  public HistoryController(
      UndoLastChange undoLastChange,
      TakeSnapshot takeSnapshot,
      ListSnapshots listSnapshots,
      RestoreSnapshot restoreSnapshot) {
    this.undoLastChange = undoLastChange;
    this.takeSnapshot = takeSnapshot;
    this.listSnapshots = listSnapshots;
    this.restoreSnapshot = restoreSnapshot;
  }

  @PostMapping("/inventory/undo")
  SnapshotResponse undo(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return SnapshotResponse.from(
        undoLastChange.undo(CurrentUser.of(jwt), new HouseholdId(householdId)));
  }

  @PostMapping("/snapshots")
  @ResponseStatus(HttpStatus.CREATED)
  SnapshotResponse take(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestBody(required = false) SnapshotRequest request) {
    return SnapshotResponse.from(
        takeSnapshot.take(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            request == null ? null : request.reason()));
  }

  @GetMapping("/snapshots")
  List<SnapshotResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listSnapshots.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(SnapshotResponse::from)
        .toList();
  }

  @PostMapping("/snapshots/{snapshotId}/restore")
  SnapshotResponse restore(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID snapshotId) {
    return SnapshotResponse.from(
        restoreSnapshot.restore(CurrentUser.of(jwt), new HouseholdId(householdId), snapshotId));
  }

  record SnapshotRequest(@Size(max = 120) String reason) {}

  record SnapshotResponse(
      UUID id, SnapshotKind kind, String reason, int fridges, int items, Instant at) {

    static SnapshotResponse from(InventorySnapshot snapshot) {
      return new SnapshotResponse(
          snapshot.id(),
          snapshot.kind(),
          snapshot.reason(),
          snapshot.memento().fridges().size(),
          snapshot.memento().fridges().stream()
              .flatMap(fridge -> fridge.zones().stream())
              .flatMap(zone -> zone.trays().stream())
              .mapToInt(tray -> tray.items().size())
              .sum(),
          snapshot.at());
    }
  }
}
