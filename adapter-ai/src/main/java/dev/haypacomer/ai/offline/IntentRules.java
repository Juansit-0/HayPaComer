package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.IntentAction;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import dev.haypacomer.domain.quantity.QuantityExpression;
import dev.haypacomer.domain.quantity.QuantityParser;
import dev.haypacomer.domain.quantity.UnitVocabulary;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class IntentRules {

  private static final int MAX_LENGTH = 200;
  private static final int MAX_FOOD_WORDS = 4;

  private static final Map<String, IntentAction> ACTIONS =
      Map.ofEntries(
              words(
                  IntentAction.STOCK,
                  "save",
                  "store",
                  "put",
                  "add",
                  "keep",
                  "guarda",
                  "guardar",
                  "guarde",
                  "agrega",
                  "agregar",
                  "mete",
                  "meter",
                  "pon",
                  "poner"),
              words(
                  IntentAction.CONSUME,
                  "ate",
                  "eat",
                  "used",
                  "use",
                  "consumed",
                  "consume",
                  "comi",
                  "comimos",
                  "use",
                  "gaste",
                  "gastamos",
                  "consumi",
                  "usamos"),
              words(
                  IntentAction.DISCARD,
                  "discard",
                  "discarded",
                  "threw",
                  "throw",
                  "toss",
                  "tossed",
                  "bote",
                  "botar",
                  "bota",
                  "tire",
                  "tirar",
                  "desechar",
                  "deseche"),
              words(
                  IntentAction.ADD_TO_MARKET,
                  "buy",
                  "need",
                  "compra",
                  "comprar",
                  "falta",
                  "faltan",
                  "necesito",
                  "necesitamos"))
          .entrySet()
          .stream()
          .flatMap(entry -> entry.getValue().stream().map(word -> Map.entry(word, entry.getKey())))
          .collect(
              Collectors.toUnmodifiableMap(
                  Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));

  private static final Set<String> MARKET_WORDS = Set.of("market", "list", "lista", "mercado");
  private static final Set<String> FILLER =
      Set.of(
          "of", "de", "del", "la", "el", "las", "los", "the", "a", "an", "un", "una", "unos",
          "unas", "some", "mi", "my", "me", "i", "we", "yo", "please", "por", "favor", "more",
          "mas");
  private static final Set<String> STOP =
      Set.of(
          "that",
          "which",
          "que",
          "expires",
          "expiring",
          "expira",
          "vence",
          "caduca",
          "until",
          "hasta",
          "for",
          "para",
          "by",
          "to",
          "al",
          "en",
          "in",
          "on",
          "and",
          "y",
          "with",
          "con");

  private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
  private static final Pattern IN_DAYS =
      Pattern.compile("\\b(?:in|en)\\s+(\\d{1,2})\\s+(?:days?|dias?)\\b");
  private static final Pattern DAY_AFTER_TOMORROW = Pattern.compile("\\bpasado\\s+manana\\b");
  private static final Pattern TOMORROW = Pattern.compile("\\b(?:tomorrow|manana)\\b");
  private static final Pattern TODAY = Pattern.compile("\\b(?:today|hoy)\\b");
  private static final Pattern WEEKDAY =
      Pattern.compile(
          "\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday"
              + "|lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b");
  private static final Map<String, DayOfWeek> WEEKDAYS =
      Map.ofEntries(
          Map.entry("monday", DayOfWeek.MONDAY),
          Map.entry("tuesday", DayOfWeek.TUESDAY),
          Map.entry("wednesday", DayOfWeek.WEDNESDAY),
          Map.entry("thursday", DayOfWeek.THURSDAY),
          Map.entry("friday", DayOfWeek.FRIDAY),
          Map.entry("saturday", DayOfWeek.SATURDAY),
          Map.entry("sunday", DayOfWeek.SUNDAY),
          Map.entry("lunes", DayOfWeek.MONDAY),
          Map.entry("martes", DayOfWeek.TUESDAY),
          Map.entry("miercoles", DayOfWeek.WEDNESDAY),
          Map.entry("jueves", DayOfWeek.THURSDAY),
          Map.entry("viernes", DayOfWeek.FRIDAY),
          Map.entry("sabado", DayOfWeek.SATURDAY),
          Map.entry("domingo", DayOfWeek.SUNDAY));
  private static final Pattern QUANTITY =
      Pattern.compile("(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?)\\s*([a-z]+\\.?)?");

  private IntentRules() {}

  static Optional<ParsedIntent> parse(String text, LocalDate today) {
    if (text == null || text.isBlank() || text.length() > MAX_LENGTH) {
      return Optional.empty();
    }
    String normalized = normalize(text);
    ExpiryMatch expiry = expiry(normalized, today);
    String rest = expiry.remaining();
    List<String> words = new ArrayList<>(Arrays.asList(rest.split("[^a-z0-9.,/]+")));
    words.removeIf(String::isBlank);
    Optional<IntentAction> action = action(words);
    if (action.isEmpty()) {
      return Optional.empty();
    }
    IntentAction resolved =
        words.stream().anyMatch(MARKET_WORDS::contains) ? IntentAction.ADD_TO_MARKET : action.get();
    Matcher quantity = QUANTITY.matcher(rest);
    QuantityExpression amount = null;
    String afterQuantity = rest;
    if (quantity.find()) {
      String unit = quantity.group(2);
      boolean unitWord = unit != null && UnitVocabulary.lookup(unit).isPresent();
      try {
        amount =
            QuantityParser.parse(quantity.group(1) + (unitWord ? " " + unit.replace(".", "") : ""));
        afterQuantity = rest.substring(unitWord ? quantity.end() : quantity.end(1));
      } catch (InvalidQuantityException unreadable) {
        amount = null;
      }
    }
    String food = food(amount == null ? rest : afterQuantity);
    if (food.isEmpty()) {
      return Optional.empty();
    }
    LocalDate expiresOn = resolved == IntentAction.STOCK ? expiry.date() : null;
    double confidence = 0.35 + (amount == null ? 0 : 0.25) + 0.25 + (expiresOn == null ? 0 : 0.15);
    return Optional.of(
        new ParsedIntent(
            resolved,
            food,
            amount,
            expiresOn,
            Math.min(1.0, confidence),
            AdvisorSource.OFFLINE_RULES));
  }

  private static Optional<IntentAction> action(List<String> words) {
    return words.stream().map(ACTIONS::get).filter(Objects::nonNull).findFirst();
  }

  private static String food(String text) {
    List<String> picked = new ArrayList<>();
    for (String word : text.split("[^a-z]+")) {
      if (word.isBlank() || FILLER.contains(word) || ACTIONS.containsKey(word)) {
        continue;
      }
      if (STOP.contains(word) || MARKET_WORDS.contains(word)) {
        if (picked.isEmpty()) {
          continue;
        }
        break;
      }
      picked.add(word);
      if (picked.size() == MAX_FOOD_WORDS) {
        break;
      }
    }
    return String.join(" ", picked);
  }

  private static ExpiryMatch expiry(String text, LocalDate today) {
    Matcher iso = ISO_DATE.matcher(text);
    if (iso.find()) {
      try {
        return new ExpiryMatch(LocalDate.parse(iso.group(1)), cut(text, iso));
      } catch (DateTimeParseException ignored) {
        return new ExpiryMatch(null, cut(text, iso));
      }
    }
    Matcher inDays = IN_DAYS.matcher(text);
    if (inDays.find()) {
      return new ExpiryMatch(today.plusDays(Long.parseLong(inDays.group(1))), cut(text, inDays));
    }
    Matcher dayAfter = DAY_AFTER_TOMORROW.matcher(text);
    if (dayAfter.find()) {
      return new ExpiryMatch(today.plusDays(2), cut(text, dayAfter));
    }
    Matcher tomorrow = TOMORROW.matcher(text);
    if (tomorrow.find()) {
      return new ExpiryMatch(today.plusDays(1), cut(text, tomorrow));
    }
    Matcher sameDay = TODAY.matcher(text);
    if (sameDay.find()) {
      return new ExpiryMatch(today, cut(text, sameDay));
    }
    Matcher weekday = WEEKDAY.matcher(text);
    if (weekday.find()) {
      return new ExpiryMatch(
          today.with(TemporalAdjusters.nextOrSame(WEEKDAYS.get(weekday.group(1)))),
          cut(text, weekday));
    }
    return new ExpiryMatch(null, text);
  }

  private static String cut(String text, Matcher match) {
    return text.substring(0, match.start()) + " " + text.substring(match.end());
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .strip();
  }

  private static Map.Entry<IntentAction, List<String>> words(IntentAction action, String... words) {
    return Map.entry(action, List.of(words));
  }

  private record ExpiryMatch(LocalDate date, String remaining) {}
}
