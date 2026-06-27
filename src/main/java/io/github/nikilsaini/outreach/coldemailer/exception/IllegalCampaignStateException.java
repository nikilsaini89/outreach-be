package io.github.nikilsaini.outreach.coldemailer.exception;

import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import java.util.UUID;

public class IllegalCampaignStateException extends RuntimeException {

  public IllegalCampaignStateException(UUID campaignId, CampaignStatus current, String action) {
    super("Cannot " + action + " campaign " + campaignId + " while it is " + current);
  }
}
