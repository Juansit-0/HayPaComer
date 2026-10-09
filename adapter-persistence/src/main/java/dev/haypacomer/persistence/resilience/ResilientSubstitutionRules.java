package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.util.List;

public final class ResilientSubstitutionRules implements SubstitutionRuleRepository {

  private final SubstitutionRuleRepository delegate;
  private final ResilientReads reads;

  public ResilientSubstitutionRules(SubstitutionRuleRepository delegate, ResilientReads reads) {
    this.delegate = delegate;
    this.reads = reads;
  }

  @Override
  public void save(SubstitutionRule rule) {
    reads.write(
        () -> {
          delegate.save(rule);
          return rule;
        });
  }

  @Override
  public List<SubstitutionRule> all() {
    return reads.read("all", delegate::all);
  }
}
