package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.ToolInvocation;
import java.util.List;
import java.util.Optional;

public final class GuardrailChain {

  private final List<Guardrail> guardrails;

  public GuardrailChain(List<Guardrail> guardrails) {
    this.guardrails = List.copyOf(guardrails);
  }

  public Optional<String> reject(ToolSpec spec, ToolInvocation invocation) {
    for (Guardrail guardrail : guardrails) {
      Optional<String> rejection = guardrail.reject(spec, invocation);
      if (rejection.isPresent()) {
        return rejection;
      }
    }
    return Optional.empty();
  }
}
