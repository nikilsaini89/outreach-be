package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.config.KafkaConfig;
import io.github.nikilsaini.outreach.coldemailer.dto.event.FollowupGenerationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowupGenerationPublisher {

  private final KafkaTemplate<String, FollowupGenerationPayload> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFollowupGenerationRequested(FollowupGenerationPayload payload) {
    kafkaTemplate.send(
        KafkaConfig.TOPIC_FOLLOWUP_GENERATION,
        payload.campaignId().toString(),
        payload
    );
    log.atInfo().setMessage("Published followup generation event")
        .addKeyValue("campaignId", payload.campaignId())
        .log();
  }
}
