package io.github.nikilsaini.outreach.auth.oauth.dto.response;

import java.util.UUID;

public record LoginResponse(UUID userId, String email) {}
