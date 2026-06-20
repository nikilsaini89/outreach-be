package io.github.nikilsaini.outreach.coldemailer.exception;

import java.util.UUID;

public class CampaignNotFoundException extends RuntimeException {

  public CampaignNotFoundException(UUID campaignId) {
    super("Campaign not found with id: " + campaignId);
  }
}
