package io.github.nikilsaini.outreach.coldemailer.kafka;

import io.github.nikilsaini.outreach.coldemailer.config.KafkaConfig;
import io.github.nikilsaini.outreach.coldemailer.dto.event.FollowupGenerationPayload;
import io.github.nikilsaini.outreach.coldemailer.service.FollowupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowupGenerationConsumer {

  private final FollowupService followupService;

  @RetryableTopic(
      attempts = "3",
      backOff = @BackOff(delay = 2000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = KafkaConfig.TOPIC_FOLLOWUP_GENERATION)
  public void consume(FollowupGenerationPayload payload) {
    log.atInfo().setMessage("Received followup generation request")
        .addKeyValue("campaignId", payload.campaignId())
        .log();
    followupService.generateAndApplyBodies(payload);
  }

  @DltHandler
  public void handleDlt(FollowupGenerationPayload payload) {
    log.atError().setMessage("Followup generation exhausted all retries — marking FAILED")
        .addKeyValue("campaignId", payload.campaignId())
        .log();
    followupService.markGenerationFailed(payload.campaignId());
  }
}
