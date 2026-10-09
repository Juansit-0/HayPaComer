package dev.haypacomer.web.i18n;

import dev.haypacomer.application.i18n.ResolveLocale;
import dev.haypacomer.application.i18n.TranslateMessages;
import dev.haypacomer.application.port.TranslationRepository;
import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.domain.identity.UserId;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class Localizer {

  private static final String ATTRIBUTE = Localizer.class.getName() + ".locale";
  private static final String EXPLICIT = Localizer.class.getName() + ".explicit";

  private final ResolveLocale resolveLocale;
  private final TranslateMessages translateMessages;
  private final TranslationRepository translations;
  private final UserLocaleRepository userLocales;

  public Localizer(
      ResolveLocale resolveLocale,
      TranslateMessages translateMessages,
      TranslationRepository translations,
      UserLocaleRepository userLocales) {
    this.resolveLocale = resolveLocale;
    this.translateMessages = translateMessages;
    this.translations = translations;
    this.userLocales = userLocales;
  }

  public String locale() {
    HttpServletRequest request = request();
    if (request == null) {
      return resolveLocale.resolve(Optional.empty(), List.of());
    }
    Object known = request.getAttribute(ATTRIBUTE);
    if (known instanceof String code) {
      return code;
    }
    List<Locale> accepted =
        request.getHeader("Accept-Language") == null
            ? List.of()
            : Collections.list(request.getLocales());
    String code = resolveLocale.resolve(user(), accepted);
    request.setAttribute(ATTRIBUTE, code);
    return code;
  }

  public String message(String text) {
    try {
      if (!explicit()) {
        return text;
      }
      return translateMessages.into(locale()).translate(text);
    } catch (RuntimeException unavailable) {
      return text;
    }
  }

  public String text(String key, String fallback) {
    try {
      if (!explicit()) {
        return fallback;
      }
      return translations.texts(locale()).getOrDefault(key, fallback);
    } catch (RuntimeException unavailable) {
      return fallback;
    }
  }

  private boolean explicit() {
    HttpServletRequest request = request();
    if (request == null) {
      return false;
    }
    Object known = request.getAttribute(EXPLICIT);
    if (known instanceof Boolean value) {
      return value;
    }
    boolean value =
        request.getHeader("Accept-Language") != null
            || user().flatMap(userLocales::locale).isPresent();
    request.setAttribute(EXPLICIT, value);
    return value;
  }

  private static Optional<UserId> user() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      try {
        return Optional.of(new UserId(UUID.fromString(jwt.getSubject())));
      } catch (IllegalArgumentException notAUser) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private static HttpServletRequest request() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes current
        ? current.getRequest()
        : null;
  }
}
