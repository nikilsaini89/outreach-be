package io.github.nikilsaini.outreach.coldemailer.dto.request;

import static io.github.nikilsaini.outreach.coldemailer.config.CampaignConstraints.MAX_FOLLOWUP_COUNT;
import static io.github.nikilsaini.outreach.coldemailer.config.CampaignConstraints.MAX_GAP_DAYS;
import static io.github.nikilsaini.outreach.coldemailer.config.CampaignConstraints.MIN_FOLLOWUP_COUNT;
import static io.github.nikilsaini.outreach.coldemailer.config.CampaignConstraints.MIN_GAP_DAYS;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record CreateCampaignRequest(
    @NotBlank @Email String recipientEmail,
    @NotBlank String subject,
    @NotBlank String initialBody,
    @Min(MIN_FOLLOWUP_COUNT) @Max(MAX_FOLLOWUP_COUNT) int followupCount,
    @Min(MIN_GAP_DAYS) @Max(MAX_GAP_DAYS) int gapDays,
    @Min(0) @Max(23) int preferredHour
) {}
