package io.github.nikilsaini.outreach.coldemailer.dto.mapper;

import io.github.nikilsaini.outreach.coldemailer.dto.response.CampaignResponse;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import java.time.LocalDateTime;
import java.util.List;

public class FollowupMapper {

  private FollowupMapper() {}

  public static Followup toEntity(Campaign campaign, String body, int sequenceNumber, LocalDateTime scheduledAt) {
    Followup followup = new Followup();
    followup.setCampaign(campaign);
    followup.setBody(body);
    followup.setSequenceNumber(sequenceNumber);
    followup.setStatus(FollowupStatus.PENDING);
    followup.setScheduledAt(scheduledAt);
    return followup;
  }

  public static FollowupResponse toResponse(Followup followup) {
    return new FollowupResponse(
        followup.getId(),
        followup.getSequenceNumber(),
        followup.getBody(),
        followup.getStatus(),
        followup.getScheduledAt()
    );
  }

  public static CampaignResponse toCampaignResponse(Campaign campaign, List<FollowupResponse> followups) {
    return new CampaignResponse(
        campaign.getId(),
        campaign.getRecipientEmail(),
        campaign.getSubject(),
        campaign.getInitialBody(),
        campaign.getStatus(),
        campaign.getCreatedAt(),
        followups
    );
  }
}
