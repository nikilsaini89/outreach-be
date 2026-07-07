package io.github.nikilsaini.outreach.coldemailer.config;

public final class CampaignConstraints {

  public static final int MAX_FOLLOWUP_COUNT = 6;
  public static final int MIN_FOLLOWUP_COUNT = 1;
  public static final int MIN_GAP_DAYS = 1;
  public static final int MAX_GAP_DAYS = 30;

  private CampaignConstraints() {}
}
