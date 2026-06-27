package io.github.nikilsaini.outreach.coldemailer.scheduler;

import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import io.github.nikilsaini.outreach.coldemailer.service.EncryptionService;
import io.github.nikilsaini.outreach.coldemailer.service.GmailService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowupScheduler {

  private final FollowupRepository followupRepository;
  private final EncryptionService encryptionService;
  private final GoogleOAuthService googleOAuthService;
  private final GmailService gmailService;

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void sendDueFollowups() {
    List<Followup> due = followupRepository.findDueFollowups(
        FollowupStatus.PENDING, LocalDateTime.now(), CampaignStatus.ACTIVE);
    if (due.isEmpty()) return;

    log.info("Found {} due follow-up(s) to send", due.size());

    for (Followup followup : due) {
      followup.setStatus(FollowupStatus.PROCESSING);
      followupRepository.save(followup);
      sendFollowup(followup);
      updateCampaignStatus(followup.getCampaign());
    }
  }

  /**
   * Closes out a campaign once none of its follow-ups remain PENDING or PROCESSING. If every
   * remaining follow-up sent cleanly the campaign is COMPLETED; if any failed it is marked FAILED.
   */
  private void updateCampaignStatus(Campaign campaign) {
    if (campaign.getStatus() != CampaignStatus.ACTIVE) {
      return;
    }
    long outstanding = followupRepository.countByCampaignIdAndStatusIn(
        campaign.getId(), List.of(FollowupStatus.PENDING, FollowupStatus.PROCESSING));
    if (outstanding > 0) {
      return;
    }
    long failed = followupRepository.countByCampaignIdAndStatusIn(
        campaign.getId(), List.of(FollowupStatus.FAILED));
    CampaignStatus terminal = failed > 0 ? CampaignStatus.FAILED : CampaignStatus.COMPLETED;
    campaign.setStatus(terminal);
    log.info("Campaign {} reached terminal status {}", campaign.getId(), terminal);
  }

  private void sendFollowup(Followup followup) {
    Campaign campaign = followup.getCampaign();
    try {
      String refreshToken = encryptionService.decrypt(campaign.getUser().getEncryptedRefreshToken());
      String accessToken = googleOAuthService.refreshAccessToken(refreshToken).accessToken();

      gmailService.sendFollowup(
          accessToken,
          campaign.getUser().getEmail(),
          campaign.getRecipientEmail(),
          campaign.getSubject(),
          followup.getBody(),
          campaign.getGmailThreadId(),
          campaign.getRootMessageId()
      );

      followup.setStatus(FollowupStatus.SENT);
      log.info("Sent follow-up #{} for campaign {}", followup.getSequenceNumber(), campaign.getId());
    } catch (Exception e) {
      followup.setStatus(FollowupStatus.FAILED);
      log.error("Failed to send follow-up #{} for campaign {}: {}", followup.getSequenceNumber(), campaign.getId(), e.getMessage());
    }
    followupRepository.save(followup);
  }
}
