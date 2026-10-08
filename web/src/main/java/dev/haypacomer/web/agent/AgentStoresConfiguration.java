package dev.haypacomer.web.agent;

import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.persistence.redis.RedisAgentRunStore;
import dev.haypacomer.persistence.redis.RedisAiAuditLog;
import dev.haypacomer.persistence.redis.RedisConfirmationStore;
import dev.haypacomer.persistence.redis.RedisConversationStore;
import dev.haypacomer.persistence.redis.RedisHouseholdMemory;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AgentStoresConfiguration {

  @Bean
  HouseholdMemory householdMemory(StringRedisTemplate redis) {
    return new RedisHouseholdMemory(redis);
  }

  @Bean
  ConversationStore conversationStore(StringRedisTemplate redis) {
    return new RedisConversationStore(redis);
  }

  @Bean
  AgentRunStore agentRunStore(StringRedisTemplate redis) {
    return new RedisAgentRunStore(redis);
  }

  @Bean
  ConfirmationStore confirmationStore(StringRedisTemplate redis, Clock clock) {
    return new RedisConfirmationStore(redis, clock);
  }

  @Bean
  AiAuditLog aiAuditLog(StringRedisTemplate redis) {
    return new RedisAiAuditLog(redis);
  }
}
