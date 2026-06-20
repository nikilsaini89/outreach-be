package io.github.nikilsaini.outreach.coldemailer.dto.request;

import java.util.UUID;

public record CreateCampaignRequest(
    UUID userId,
    String recipientEmail,
    String subject,
    String initialBody,
    int followupCount
) {}
