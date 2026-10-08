package dev.haypacomer.application.port;

import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import java.util.Optional;

public interface FoodProfileRepository {

  void save(FoodProfile profile);

  Optional<FoodProfile> find(MemberId member);
}
