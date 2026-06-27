package io.github.nikilsaini.outreach.coldemailer.dto.response;

import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CampaignResponse(
    UUID id,
    String recipientEmail,
    String subject,
    String initialBody,
    CampaignStatus status,
    LocalDateTime createdAt,
    List<FollowupResponse> followups
) {}
