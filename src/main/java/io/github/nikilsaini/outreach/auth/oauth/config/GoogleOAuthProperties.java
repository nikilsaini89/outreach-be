package io.github.nikilsaini.outreach.auth.oauth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.oauth")
public record GoogleOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String authUri,
    String tokenUri,
    List<String> scopes
) {}
