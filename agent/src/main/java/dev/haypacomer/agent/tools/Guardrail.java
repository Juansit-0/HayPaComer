package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.ToolInvocation;
import java.util.Optional;

public interface Guardrail {

  Optional<String> reject(ToolSpec spec, ToolInvocation invocation);
}
