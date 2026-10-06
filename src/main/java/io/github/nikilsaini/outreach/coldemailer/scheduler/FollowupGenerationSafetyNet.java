package io.github.nikilsaini.outreach.coldemailer.scheduler;

import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowupGenerationSafetyNet {

  private static final int STALE_THRESHOLD_MINUTES = 5;

  private final FollowupRepository followupRepository;
  private final CampaignRepository campaignRepository;

  /**
   * Recovers follow-ups stuck in GENERATING after a JVM crash or Kafka partition rebalance.
   * Any GENERATING stub older than STALE_THRESHOLD_MINUTES is marked FAILED, and its campaign is
   * also marked FAILED so the user knows generation did not complete.
   */
  @Scheduled(fixedDelay = 5 * 60 * 1000)
  @Transactional
  public void recoverStaleGeneratingFollowups() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
    List<Followup> stale = followupRepository.findStaleGenerating(FollowupStatus.GENERATING, cutoff);
    if (stale.isEmpty()) return;

    Set<UUID> campaignIds = stale.stream()
        .map(f -> f.getCampaign().getId())
        .collect(Collectors.toSet());

    log.atWarn().setMessage("Safety net: recovering stale GENERATING follow-ups")
        .addKeyValue("count", stale.size())
        .addKeyValue("campaigns", campaignIds.size())
        .log();

    stale.forEach(f -> f.setStatus(FollowupStatus.FAILED));
    followupRepository.saveAll(stale);

    campaignIds.forEach(campaignId ->
        campaignRepository.findById(campaignId).ifPresent(c -> {
          if (c.getStatus() == CampaignStatus.ACTIVE) {
            c.setStatus(CampaignStatus.FAILED);
            campaignRepository.save(c);
          }
        })
    );
  }
}
