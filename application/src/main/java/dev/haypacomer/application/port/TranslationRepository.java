package dev.haypacomer.application.port;

import dev.haypacomer.application.i18n.LocaleOption;
import java.util.List;
import java.util.Map;

public interface TranslationRepository {

  List<LocaleOption> locales();

  Map<String, String> texts(String locale);

  Map<String, String> templates(String locale);
}
