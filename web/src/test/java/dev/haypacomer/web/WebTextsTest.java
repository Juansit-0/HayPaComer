package dev.haypacomer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.session.SessionPhase;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WebTextsTest {

  private static final Path STATIC = Path.of("src/main/resources/static");
  private static final Pattern ROW =
      Pattern.compile("\\('(es-CO|en)', '((?:[^']|'')+)', '(?:[^']|'')+'\\)");
  private static final Pattern CALL = Pattern.compile("\\b(t|plural)\\(\"([a-z0-9.-]+)\"");
  private static final Pattern ATTRIBUTE =
      Pattern.compile("data-i18n(?:-label)?=\"([a-z0-9.-]+)\"");
  private static final Pattern VISIBLE = Pattern.compile(">([^<>${}`]*[A-Za-z]{2,}[^<>${}`]*)<");

  private static Map<String, Set<String>> translations() throws IOException {
    try (InputStream sql =
        WebTextsTest.class.getResourceAsStream("/db/migration/V24__web_translations.sql")) {
      assertNotNull(sql, "V24 translations migration on the classpath");
      Matcher rows = ROW.matcher(new String(sql.readAllBytes(), StandardCharsets.UTF_8));
      Map<String, Set<String>> keys = new HashMap<>();
      while (rows.find()) {
        keys.computeIfAbsent(rows.group(1), locale -> new HashSet<>()).add(rows.group(2));
      }
      return keys;
    }
  }

  private static List<Path> scripts() throws IOException {
    try (Stream<Path> files = Files.list(STATIC.resolve("app"))) {
      return files.filter(file -> file.toString().endsWith(".js")).sorted().toList();
    }
  }

  @Test
  void everyTextTheInterfaceAsksForExistsInSpanishAndEnglish() throws IOException {
    Set<String> plain = new TreeSet<>();
    Set<String> counted = new TreeSet<>();
    for (Path script : scripts()) {
      Matcher calls = CALL.matcher(Files.readString(script));
      while (calls.find()) {
        (calls.group(1).equals("plural") ? counted : plain).add(calls.group(2));
      }
    }
    Matcher attributes = ATTRIBUTE.matcher(Files.readString(STATIC.resolve("index.html")));
    while (attributes.find()) {
      plain.add(attributes.group(1));
    }
    for (RequirementVerdict verdict : RequirementVerdict.values()) {
      plain.add("verdict." + verdict.name());
    }
    for (SessionPhase phase : SessionPhase.values()) {
      plain.add("chef.phase." + phase.name());
    }
    for (FoodCategory category : FoodCategory.values()) {
      plain.add("category." + category.name());
    }
    Map<String, Set<String>> keys = translations();
    List<String> missing = new ArrayList<>();
    for (String locale : List.of("es-CO", "en")) {
      Set<String> known = keys.getOrDefault(locale, Set.of());
      plain.stream()
          .filter(key -> !known.contains(key))
          .forEach(key -> missing.add(locale + " " + key));
      counted.stream()
          .flatMap(key -> Stream.of(key + ".one", key + ".other"))
          .filter(key -> !known.contains(key))
          .forEach(key -> missing.add(locale + " " + key));
    }
    assertTrue(plain.size() > 150, "keys found: " + plain.size());
    assertEquals(List.of(), missing);
    assertEquals(keys.get("es-CO"), keys.get("en"));
  }

  @Test
  void scriptsShowNoTextOutsideTheTranslations() throws IOException {
    List<String> literals = new ArrayList<>();
    for (Path script : scripts()) {
      if (script.getFileName().toString().equals("i18n.js")) {
        continue;
      }
      Matcher visible = VISIBLE.matcher(Files.readString(script));
      while (visible.find()) {
        if (!visible.group(1).isBlank()) {
          literals.add(script.getFileName() + ": " + visible.group(1).strip());
        }
      }
    }
    assertEquals(List.of(), literals);
  }
}
