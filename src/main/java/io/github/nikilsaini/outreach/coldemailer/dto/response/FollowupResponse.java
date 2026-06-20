package io.github.nikilsaini.outreach.coldemailer.dto.response;

import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record FollowupResponse(
    UUID id,
    int sequenceNumber,
    String body,
    FollowupStatus status,
    LocalDateTime scheduledAt
) {}
