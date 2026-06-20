package io.github.nikilsaini.outreach.coldemailer.dto.request;

public record CreateUserRequest(
    String email,
    String firstName,
    String lastName,
    String refreshToken
) {}
