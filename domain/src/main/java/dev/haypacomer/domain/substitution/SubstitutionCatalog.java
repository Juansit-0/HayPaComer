package dev.haypacomer.domain.substitution;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.Grams;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class SubstitutionCatalog {

  public static final SubstitutionCatalog EMPTY = new SubstitutionCatalog(List.of());

  private final List<SubstitutionRule> rules;

  public SubstitutionCatalog(List<SubstitutionRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public List<SubstitutionRule> rulesFor(FoodMetadata original) {
    return rules.stream().filter(rule -> rule.replaces(original)).toList();
  }

  public Set<SubstitutionProblem> check(
      SubstitutionRule rule, Grams shortfall, Grams substituteAvailable, DiningGroup diners) {
    Set<SubstitutionProblem> problems = EnumSet.noneOf(SubstitutionProblem.class);
    if (!rule.allowsReplacing(shortfall)) {
      problems.add(SubstitutionProblem.OVER_LIMIT);
    }
    if (!substituteAvailable.isAtLeast(rule.substituteGramsFor(shortfall))) {
      problems.add(SubstitutionProblem.NOT_ENOUGH_STOCK);
    }
    if (!diners.allows(rule.substitute())) {
      problems.add(SubstitutionProblem.CONFLICTS_WITH_DINERS);
    }
    return problems;
  }

  public Optional<Substitution> propose(
      FoodMetadata original,
      Grams shortfall,
      Function<FoodMetadata, Grams> available,
      DiningGroup diners) {
    return rulesFor(original).stream()
        .filter(
            rule -> check(rule, shortfall, available.apply(rule.substitute()), diners).isEmpty())
        .findFirst()
        .map(rule -> new Substitution(rule, shortfall, rule.substituteGramsFor(shortfall)));
  }
}
