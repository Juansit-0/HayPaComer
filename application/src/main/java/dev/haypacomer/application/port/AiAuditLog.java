package dev.haypacomer.application.port;

import dev.haypacomer.application.agent.AiAuditEntry;
import java.util.List;

public interface AiAuditLog {

  void record(AiAuditEntry entry);

  List<AiAuditEntry> recent(int limit);
}
