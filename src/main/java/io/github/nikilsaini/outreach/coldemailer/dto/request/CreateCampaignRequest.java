package io.github.nikilsaini.outreach.coldemailer.dto.request;

public record CreateCampaignRequest(
    String recipientEmail,
    String subject,
    String initialBody,
    int followupCount,
    int gapDays,
    int preferredHour
) {}
