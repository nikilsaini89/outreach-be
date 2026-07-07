package io.github.nikilsaini.outreach.auth.jwt.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
