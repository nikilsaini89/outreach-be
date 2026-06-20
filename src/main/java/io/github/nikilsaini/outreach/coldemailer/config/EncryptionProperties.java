package io.github.nikilsaini.outreach.coldemailer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(String secretKey) {}
