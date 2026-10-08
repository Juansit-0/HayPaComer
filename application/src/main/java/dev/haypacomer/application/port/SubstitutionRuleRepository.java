package dev.haypacomer.application.port;

import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.util.List;

public interface SubstitutionRuleRepository {

  void save(SubstitutionRule rule);

  List<SubstitutionRule> all();
}
