package dev.haypacomer.application.i18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageTranslator {

  private static final String HOLE = "{}";

  private record Template(Pattern pattern, String target, int literal) {}

  private final Map<String, String> exact;
  private final List<Template> patterns;

  public MessageTranslator(Map<String, String> templates) {
    this.exact = Map.copyOf(templates);
    List<Template> compiled = new ArrayList<>();
    templates.forEach(
        (source, target) -> {
          if (source.contains(HOLE)) {
            compiled.add(compile(source, target));
          }
        });
    compiled.sort(Comparator.comparingInt(Template::literal).reversed());
    this.patterns = List.copyOf(compiled);
  }

  public String translate(String message) {
    if (message == null || message.isBlank()) {
      return message;
    }
    String direct = exact.get(message);
    if (direct != null) {
      return direct;
    }
    for (Template template : patterns) {
      Matcher matcher = template.pattern().matcher(message);
      if (matcher.matches()) {
        return fill(template.target(), matcher);
      }
    }
    return message;
  }

  private static Template compile(String source, String target) {
    String[] parts = source.split(Pattern.quote(HOLE), -1);
    StringBuilder regex = new StringBuilder();
    int literal = 0;
    for (int index = 0; index < parts.length; index++) {
      if (index > 0) {
        regex.append("(.+?)");
      }
      regex.append(Pattern.quote(parts[index]));
      literal += parts[index].length();
    }
    return new Template(Pattern.compile(regex.toString(), Pattern.DOTALL), target, literal);
  }

  private static String fill(String target, Matcher matcher) {
    StringBuilder result = new StringBuilder();
    int group = 1;
    int from = 0;
    int hole;
    while ((hole = target.indexOf(HOLE, from)) >= 0) {
      result.append(target, from, hole);
      result.append(group <= matcher.groupCount() ? matcher.group(group++) : HOLE);
      from = hole + HOLE.length();
    }
    return result.append(target.substring(from)).toString();
  }
}
