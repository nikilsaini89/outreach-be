package io.github.nikilsaini.outreach.coldemailer.scheduler;

import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import io.github.nikilsaini.outreach.coldemailer.service.EncryptionService;
import io.github.nikilsaini.outreach.coldemailer.service.FollowupService;
import io.github.nikilsaini.outreach.coldemailer.service.GmailService;
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

  private final FollowupService followupService;
  private final EncryptionService encryptionService;
  private final GoogleOAuthService googleOAuthService;
  private final GmailService gmailService;

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void sendDueFollowups() {
    List<Followup> due = followupService.findDue();
    if (due.isEmpty()) return;

    log.info("Found {} due follow-up(s) to send", due.size());

    for (Followup followup : due) {
      followupService.updateStatus(followup, FollowupStatus.PROCESSING);
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
    if (followupService.hasOutstanding(campaign.getId())) {
      return;
    }
    CampaignStatus terminal =
        followupService.hasFailures(campaign.getId()) ? CampaignStatus.FAILED : CampaignStatus.COMPLETED;
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

      followupService.updateStatus(followup, FollowupStatus.SENT);
      log.info("Sent follow-up #{} for campaign {}", followup.getSequenceNumber(), campaign.getId());
    } catch (Exception e) {
      // Terminal FAILED with no reattempt today; deferred "retry for failed send" behaviour
      // (backoff + attempt count) tracked as OPEN-DECISION-2 in docs/OPEN_DECISIONS.md.
      followupService.updateStatus(followup, FollowupStatus.FAILED);
      log.error("Failed to send follow-up #{} for campaign {}: {}", followup.getSequenceNumber(), campaign.getId(), e.getMessage());
    }
  }
}
