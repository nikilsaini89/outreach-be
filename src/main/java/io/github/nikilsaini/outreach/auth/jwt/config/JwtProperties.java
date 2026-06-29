package io.github.nikilsaini.outreach.auth.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long authExpiryMs, long refreshExpiryMs) {}
