package io.github.nikilsaini.outreach.coldemailer.dto.event;

import java.util.UUID;

public record FollowupGenerationPayload(
    UUID campaignId,
    String subject,
    String initialBody,
    int followupCount,
    int gapDays,
    int preferredHour
) {}
