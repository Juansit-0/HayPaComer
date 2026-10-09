package dev.haypacomer.web.i18n;

import dev.haypacomer.application.i18n.ChangeUserLocale;
import dev.haypacomer.application.i18n.ListLocales;
import dev.haypacomer.application.i18n.ResolveLocale;
import dev.haypacomer.application.i18n.TranslateMessages;
import dev.haypacomer.application.i18n.ViewTranslations;
import dev.haypacomer.application.port.TranslationRepository;
import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.persistence.relational.PostgresTranslationRepository;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class I18nConfiguration {

  @Bean
  TranslationRepository translationRepository(
      DataSource dataSource,
      Clock clock,
      @Value("${haypacomer.i18n.cache-ttl:PT5M}") Duration timeToLive) {
    return new PostgresTranslationRepository(dataSource, clock, timeToLive);
  }

  @Bean
  ListLocales listLocales(TranslationRepository translations) {
    return new ListLocales(translations);
  }

  @Bean
  ViewTranslations viewTranslations(TranslationRepository translations) {
    return new ViewTranslations(translations);
  }

  @Bean
  ResolveLocale resolveLocale(TranslationRepository translations, UserLocaleRepository users) {
    return new ResolveLocale(translations, users);
  }

  @Bean
  ChangeUserLocale changeUserLocale(
      TranslationRepository translations, UserLocaleRepository users) {
    return new ChangeUserLocale(translations, users);
  }

  @Bean
  TranslateMessages translateMessages(TranslationRepository translations) {
    return new TranslateMessages(translations);
  }
}
