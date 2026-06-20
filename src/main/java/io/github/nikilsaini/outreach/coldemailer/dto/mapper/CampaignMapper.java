package io.github.nikilsaini.outreach.coldemailer.dto.mapper;

import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateCampaignRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;

public class CampaignMapper {

  private CampaignMapper() {}

  public static Campaign toEntity(CreateCampaignRequest request, User user, String gmailThreadId, String rootMessageId) {
    Campaign campaign = new Campaign();
    campaign.setUser(user);
    campaign.setRecipientEmail(request.recipientEmail());
    campaign.setSubject(request.subject());
    campaign.setInitialBody(request.initialBody());
    campaign.setStatus(CampaignStatus.ACTIVE);
    campaign.setGmailThreadId(gmailThreadId);
    campaign.setRootMessageId(rootMessageId);
    return campaign;
  }
}
