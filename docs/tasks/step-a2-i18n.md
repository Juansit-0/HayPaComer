# Step A2: Translations and localized messages in the database

Commit and pull request title: `feat(i18n): translations and localized messages in the database`

Owner: Jenifer Urbano (`Jenifrutica`). Second backend step of plan v2.

## Goal

HayPaComer speaks Colombian Spanish by default and English on request. Every translated text lives in PostgreSQL, so adding a language or fixing a word needs no deploy.

## Scope

- Flyway `V22__i18n`:
  - `locales` (`es-CO` default, `en`);
  - `translations` (locale, key, text): food names as `food.<name_key>` in both languages and setting descriptions as `setting.<key>`; the web UI adds its own keys in step B3;
  - `message_templates` (locale, source, target): Spanish for problem titles, error details, alert titles and bodies, briefing titles, and cold investigation verdicts. `{}` marks a value copied from the original sentence;
  - `users.locale`.
- Application:
  - ports `TranslationRepository` and `UserLocaleRepository`;
  - `MessageTranslator` matches the exact sentence first, then the template with the longest fixed text, and returns the original when nothing matches;
  - use cases `ListLocales`, `ViewTranslations`, `ResolveLocale` (saved preference, then `Accept-Language` by tag and then by language, then the default), `ChangeUserLocale`, and `TranslateMessages` (one compiled translator per locale while the cached templates do not change).
- Persistence: `PostgresTranslationRepository` caches for `haypacomer.i18n.cache-ttl` (5 minutes by default) and serves the last copy if the database fails; `PostgresUserLocaleRepository`; catalog search also matches the Spanish name without accents ("pechu" or "platano").
- Web:
  - `Localizer` resolves the locale once per request;
  - `LocalizedProblems` translates the title and detail of every RFC 7807 answer;
  - notifications, setting descriptions, and cold investigation reasons are translated too;
  - a request without `Accept-Language` from a person with no saved language keeps the original text, so API clients and scripts see stable English;
  - `GET /api/v1/i18n` (current and available locales) and `GET /api/v1/i18n/{locale}` (texts, ETag, 5 minute cache) are public; `PUT /api/v1/me/locale` saves the person's language.

## Tests (definition of done)

- `I18nTest`: exact and template translation, the most specific template wins, the resolution order, unknown locales, and bundles.
- `PostgresTranslationRepositoryTest`: seeds, the alert template, user locale, and Spanish catalog search.
- `I18nIntegrationTest`: public bundles with ETag and 304, browser language, Spanish and English errors, the saved language wins over the browser, setting descriptions, 404 for an unknown locale, and 401 to save the language anonymously.
