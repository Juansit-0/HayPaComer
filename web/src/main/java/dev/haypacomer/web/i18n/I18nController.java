package dev.haypacomer.web.i18n;

import dev.haypacomer.application.i18n.ChangeUserLocale;
import dev.haypacomer.application.i18n.ListLocales;
import dev.haypacomer.application.i18n.LocaleOption;
import dev.haypacomer.application.i18n.ViewTranslations;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
public class I18nController {

  private final ListLocales listLocales;
  private final ViewTranslations viewTranslations;
  private final ChangeUserLocale changeUserLocale;
  private final Localizer localizer;

  public I18nController(
      ListLocales listLocales,
      ViewTranslations viewTranslations,
      ChangeUserLocale changeUserLocale,
      Localizer localizer) {
    this.listLocales = listLocales;
    this.viewTranslations = viewTranslations;
    this.changeUserLocale = changeUserLocale;
    this.localizer = localizer;
  }

  @GetMapping("/api/v1/i18n")
  LocalesResponse locales() {
    return new LocalesResponse(localizer.locale(), listLocales.list());
  }

  @GetMapping("/api/v1/i18n/{locale}")
  ResponseEntity<Map<String, String>> translations(
      @PathVariable String locale, WebRequest request) {
    Map<String, String> texts = new TreeMap<>(viewTranslations.view(locale));
    String tag = "\"" + Integer.toHexString(texts.hashCode()) + "\"";
    if (request.checkNotModified(tag)) {
      return null;
    }
    return ResponseEntity.ok()
        .eTag(tag)
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
        .body(texts);
  }

  @PutMapping("/api/v1/me/locale")
  LocaleResponse change(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LocaleRequest body) {
    return new LocaleResponse(changeUserLocale.change(CurrentUser.of(jwt), body.locale()));
  }

  record LocaleRequest(@NotBlank String locale) {}

  record LocaleResponse(String locale) {}

  record LocalesResponse(String current, List<LocaleOption> available) {}
}
