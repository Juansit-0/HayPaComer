package dev.haypacomer.agent.planning;

import dev.haypacomer.agent.runtime.AgentContext;
import dev.haypacomer.agent.runtime.Decision;
import dev.haypacomer.agent.runtime.Exchange;
import dev.haypacomer.agent.runtime.Planner;
import dev.haypacomer.agent.runtime.PlannerUnavailableException;
import dev.haypacomer.agent.supervisor.Specialist;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.agent.ChatModelUnavailableException;
import dev.haypacomer.application.port.ChatModel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class LlmPlanner implements Planner {

  static final int MAX_OBSERVATION = 1500;
  static final int MAX_ANSWER = 1500;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Pattern GRAMS =
      Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:g|gr|grams|gramos)\\b", Pattern.CASE_INSENSITIVE);

  private final ChatModel model;
  private final Specialist specialist;
  private final ToolRegistry tools;

  public LlmPlanner(ChatModel model, Specialist specialist, ToolRegistry tools) {
    this.model = model;
    this.specialist = specialist;
    this.tools = tools;
  }

  @Override
  public String name() {
    return model.name();
  }

  @Override
  public Decision next(AgentContext context) {
    String answer;
    try {
      answer = model.completeJson(system(context), user(context));
    } catch (ChatModelUnavailableException unavailable) {
      throw new PlannerUnavailableException(unavailable.getMessage());
    }
    return parse(answer, context);
  }

  private String system(AgentContext context) {
    String catalog =
        tools.specs().stream()
            .filter(spec -> context.tools().contains(spec.name()))
            .map(LlmPlanner::describe)
            .collect(Collectors.joining("\n"));
    return """
        You are the %s specialist of HayPaComer, a smart fridge. %s
        You act only through these tools:
        %s
        Rules: read before you answer; never invent grams, dates, or food safety verdicts;
        quote grams only as the tools observed them; write tools are proposals that the
        person confirms; answer briefly in the language of the goal.
        Reply only JSON, either
        {"action":"call","tool":"name","arguments":{"name":"value"},"reason":"why"}
        or {"action":"answer","text":"answer","evidence":["tool names you relied on"]}.
        """
        .formatted(specialist.name(), specialist.purpose(), catalog);
  }

  private static String describe(ToolSpec spec) {
    String parameters =
        spec.parameters().stream().map(LlmPlanner::describe).collect(Collectors.joining(", "));
    return "- "
        + spec.name()
        + " ("
        + spec.kind().name().toLowerCase(java.util.Locale.ROOT)
        + "): "
        + spec.description()
        + (parameters.isEmpty() ? "" : " Arguments: " + parameters);
  }

  private static String describe(ParameterSpec parameter) {
    return parameter.name()
        + " "
        + parameter.type().name().toLowerCase(java.util.Locale.ROOT)
        + (parameter.required() ? " required" : " optional");
  }

  private static String user(AgentContext context) {
    StringBuilder user =
        new StringBuilder("Goal: ")
            .append(context.task().goal())
            .append("\nSteps left: ")
            .append(context.stepsLeft());
    for (Exchange exchange : context.history()) {
      String observed = exchange.observation().content();
      user.append("\nCalled ")
          .append(exchange.call().tool())
          .append(' ')
          .append(exchange.call().arguments())
          .append(exchange.observation().failed() ? " and it failed: " : " and observed: ")
          .append(
              observed.length() > MAX_OBSERVATION
                  ? observed.substring(0, MAX_OBSERVATION)
                  : observed);
    }
    return user.toString();
  }

  private Decision parse(String answer, AgentContext context) {
    JsonNode root;
    try {
      root = JSON.readTree(unfence(answer));
    } catch (JacksonException malformed) {
      throw new PlannerUnavailableException("The model answered invalid JSON");
    }
    if (root == null || !root.isObject()) {
      throw new PlannerUnavailableException("The model must answer a JSON object");
    }
    String action = root.path("action").asString("");
    return switch (action) {
      case "call" -> call(root);
      case "answer" -> answer(root, context);
      default -> throw new PlannerUnavailableException("Unknown action " + action);
    };
  }

  private static Decision call(JsonNode root) {
    String tool = root.path("tool").asString("").strip();
    if (tool.isEmpty() || tool.length() > 40) {
      throw new PlannerUnavailableException("A call needs a tool name");
    }
    Map<String, String> arguments = new LinkedHashMap<>();
    JsonNode given = root.path("arguments");
    if (given.isObject()) {
      for (Map.Entry<String, JsonNode> entry : given.properties()) {
        if (!entry.getValue().isValueNode() || entry.getValue().isNull()) {
          throw new PlannerUnavailableException("Arguments must be plain values");
        }
        arguments.put(entry.getKey(), entry.getValue().asString());
      }
    }
    String reason = root.path("reason").asString("").strip();
    return new Decision.CallTool(
        tool, arguments, reason.isEmpty() ? "Model chose " + tool : clip(reason, 200));
  }

  private static Decision answer(JsonNode root, AgentContext context) {
    String text = root.path("text").asString("").strip();
    if (text.isEmpty() || text.length() > MAX_ANSWER) {
      throw new PlannerUnavailableException("An answer needs text up to 1500 characters");
    }
    String observed =
        context.history().stream()
            .filter(exchange -> !exchange.observation().failed())
            .map(exchange -> exchange.observation().content())
            .collect(Collectors.joining("\n"));
    Matcher grams = GRAMS.matcher(text);
    while (grams.find()) {
      String amount = grams.group(1).replace(',', '.');
      if (!Pattern.compile("(?<![\\d.])" + Pattern.quote(amount) + "\\s*g\\b")
          .matcher(observed)
          .find()) {
        throw new PlannerUnavailableException("The answer quoted grams no tool observed");
      }
    }
    for (JsonNode cited : root.path("evidence")) {
      if (!context.called(cited.asString(""))) {
        throw new PlannerUnavailableException("The answer cited a tool it never used");
      }
    }
    return new Decision.FinalAnswer(text);
  }

  static String unfence(String answer) {
    String trimmed = answer.strip();
    if (!trimmed.startsWith("```") || !trimmed.endsWith("```") || trimmed.length() < 6) {
      return trimmed;
    }
    String inner = trimmed.substring(3, trimmed.length() - 3);
    int newline = inner.indexOf('\n');
    if (newline >= 0 && inner.substring(0, newline).strip().matches("[A-Za-z]*")) {
      inner = inner.substring(newline + 1);
    }
    return inner.strip();
  }

  private static String clip(String text, int max) {
    return text.length() > max ? text.substring(0, max) : text;
  }
}
