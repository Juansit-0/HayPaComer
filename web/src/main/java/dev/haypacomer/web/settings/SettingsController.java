package dev.haypacomer.web.settings;

import dev.haypacomer.application.settings.ChangeHouseholdSetting;
import dev.haypacomer.application.settings.ListSettingDefinitions;
import dev.haypacomer.application.settings.SettingDefinition;
import dev.haypacomer.application.settings.SettingView;
import dev.haypacomer.application.settings.ViewHouseholdSettings;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.i18n.Localizer;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

  private final ViewHouseholdSettings viewSettings;
  private final ChangeHouseholdSetting changeSetting;
  private final ListSettingDefinitions definitions;
  private final Localizer localizer;

  public SettingsController(
      ViewHouseholdSettings viewSettings,
      ChangeHouseholdSetting changeSetting,
      ListSettingDefinitions definitions,
      Localizer localizer) {
    this.localizer = localizer;
    this.viewSettings = viewSettings;
    this.changeSetting = changeSetting;
    this.definitions = definitions;
  }

  @GetMapping("/api/v1/settings/defaults")
  List<DefinitionResponse> defaults() {
    return definitions.list().stream()
        .map(DefinitionResponse::from)
        .map(
            response ->
                new DefinitionResponse(
                    response.key(),
                    response.kind(),
                    response.scope(),
                    response.value(),
                    response.min(),
                    response.max(),
                    localizer.text("setting." + response.key(), response.description())))
        .toList();
  }

  @GetMapping("/api/v1/households/{householdId}/settings")
  List<SettingResponse> view(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return viewSettings.view(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(SettingResponse::from)
        .map(this::localized)
        .toList();
  }

  @PutMapping("/api/v1/households/{householdId}/settings")
  SettingResponse change(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody ChangeRequest request) {
    return localized(
        SettingResponse.from(
            changeSetting.change(
                CurrentUser.of(jwt),
                new HouseholdId(householdId),
                request.key(),
                Optional.ofNullable(request.value()))));
  }

  private SettingResponse localized(SettingResponse response) {
    return new SettingResponse(
        response.key(),
        response.kind(),
        response.value(),
        response.defaultValue(),
        response.min(),
        response.max(),
        response.overridden(),
        localizer.text("setting." + response.key(), response.description()));
  }

  record ChangeRequest(@NotBlank String key, String value) {}

  record DefinitionResponse(
      String key,
      String kind,
      String scope,
      String value,
      BigDecimal min,
      BigDecimal max,
      String description) {

    static DefinitionResponse from(SettingDefinition definition) {
      return new DefinitionResponse(
          definition.key(),
          definition.kind().name(),
          definition.scope().name(),
          definition.value(),
          definition.min(),
          definition.max(),
          definition.description());
    }
  }

  record SettingResponse(
      String key,
      String kind,
      String value,
      String defaultValue,
      BigDecimal min,
      BigDecimal max,
      boolean overridden,
      String description) {

    static SettingResponse from(SettingView view) {
      SettingDefinition definition = view.definition();
      return new SettingResponse(
          definition.key(),
          definition.kind().name(),
          view.value(),
          definition.value(),
          definition.min(),
          definition.max(),
          view.overridden(),
          definition.description());
    }
  }
}
