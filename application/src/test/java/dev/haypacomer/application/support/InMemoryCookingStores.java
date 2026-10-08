package dev.haypacomer.application.support;

import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCookingStores {

  public final Map<MemberId, FoodProfile> profilesByMember = new HashMap<>();
  public final List<SubstitutionRule> ruleList = new ArrayList<>();

  public final FoodProfileRepository profiles =
      new FoodProfileRepository() {
        @Override
        public void save(FoodProfile profile) {
          profilesByMember.put(profile.member(), profile);
        }

        @Override
        public Optional<FoodProfile> find(MemberId member) {
          return Optional.ofNullable(profilesByMember.get(member));
        }
      };

  public final SubstitutionRuleRepository rules =
      new SubstitutionRuleRepository() {
        @Override
        public void save(SubstitutionRule rule) {
          ruleList.removeIf(existing -> existing.id().equals(rule.id()));
          ruleList.add(rule);
        }

        @Override
        public List<SubstitutionRule> all() {
          return List.copyOf(ruleList);
        }
      };
}
