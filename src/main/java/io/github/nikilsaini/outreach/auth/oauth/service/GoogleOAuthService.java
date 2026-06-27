package io.github.nikilsaini.outreach.auth.oauth.service;

import io.github.nikilsaini.outreach.auth.oauth.config.GoogleOAuthProperties;
import io.github.nikilsaini.outreach.auth.oauth.constant.GoogleOAuthConstants;
import io.github.nikilsaini.outreach.auth.oauth.dto.request.RefreshTokenRequest;
import io.github.nikilsaini.outreach.auth.oauth.dto.request.TokenExchangeRequest;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleIdTokenClaims;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleTokenResponse;
import io.github.nikilsaini.outreach.auth.oauth.exception.InvalidIdTokenException;
import io.github.nikilsaini.outreach.auth.oauth.util.GoogleOAuthUriBuilder;
import java.net.URI;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

  private final GoogleOAuthProperties properties;
  private final RestClient googleRestClient;
  private final ObjectMapper objectMapper;

  public URI buildAuthorizationUri() {
    return GoogleOAuthUriBuilder.buildAuthorizationUri(properties);
  }

  public GoogleTokenResponse exchangeCodeForTokens(String code) {
    log.atDebug().setMessage("Exchanging Google authorization code").log();
    return fetchTokens(new TokenExchangeRequest(
        code, properties.clientId(), properties.clientSecret(),
        properties.redirectUri(), GoogleOAuthConstants.GRANT_TYPE_AUTH_CODE
    ).toFormParams());
  }

  public GoogleTokenResponse refreshAccessToken(String refreshToken) {
    log.atDebug().setMessage("Refreshing Google access token").log();
    return fetchTokens(new RefreshTokenRequest(
        refreshToken, properties.clientId(), properties.clientSecret(), GoogleOAuthConstants.GRANT_TYPE_REFRESH_TOKEN
    ).toFormParams());
  }

  private GoogleTokenResponse fetchTokens(org.springframework.util.MultiValueMap<String, String> form) {
    return googleRestClient.post()
        .uri(properties.tokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(GoogleTokenResponse.class);
  }

  public GoogleIdTokenClaims decodeIdToken(String idToken) {
    String[] segments = idToken.split("\\.");
    if (segments.length < 2) {
      throw new InvalidIdTokenException("Malformed id_token: expected 3 segments, got " + segments.length);
    }
    try {
      byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
      return objectMapper.readValue(payload, GoogleIdTokenClaims.class);
    } catch (Exception e) {
      // Preserve the root cause here; the handler only sees the InvalidIdTokenException message.
      log.atDebug().setMessage("Failed to parse id_token claims").setCause(e).log();
      throw new InvalidIdTokenException("Failed to parse id_token claims");
    }
  }
}
