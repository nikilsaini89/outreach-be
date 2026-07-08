package io.github.nikilsaini.outreach.coldemailer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaConfig {

  public static final String TOPIC_FOLLOWUP_GENERATION = "followup-generation-requested";

  @Bean
  public NewTopic followupGenerationTopic() {
    return TopicBuilder.name(TOPIC_FOLLOWUP_GENERATION)
        .partitions(6)
        .replicas(1)
        .build();
  }
}
