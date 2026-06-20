package io.github.nikilsaini.outreach.auth.oauth.service;

import io.github.nikilsaini.outreach.auth.oauth.config.GoogleOAuthProperties;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleIdTokenClaims;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleTokenResponse;
import io.github.nikilsaini.outreach.auth.oauth.exception.InvalidIdTokenException;
import java.net.URI;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

  private static final String RESPONSE_TYPE_CODE = "code";
  private static final String ACCESS_TYPE_OFFLINE = "offline";
  private static final String PROMPT_CONSENT = "consent";
  private static final String GRANT_TYPE_AUTH_CODE = "authorization_code";
  private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
  private static final String SCOPE_SEPARATOR = " ";

  private final GoogleOAuthProperties properties;
  private final RestClient googleRestClient;
  private final ObjectMapper objectMapper;

  public URI buildAuthorizationUri() {
    return UriComponentsBuilder
        .fromUriString(properties.authUri())
        .queryParam("client_id", properties.clientId())
        .queryParam("redirect_uri", properties.redirectUri())
        .queryParam("response_type", RESPONSE_TYPE_CODE)
        .queryParam("scope", String.join(SCOPE_SEPARATOR, properties.scopes()))
        .queryParam("access_type", ACCESS_TYPE_OFFLINE)
        .queryParam("prompt", PROMPT_CONSENT)
        .build()
        .toUri();
  }

  public GoogleTokenResponse exchangeCodeForTokens(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());
    form.add("redirect_uri", properties.redirectUri());
    form.add("grant_type", GRANT_TYPE_AUTH_CODE);
    return fetchTokens(form);
  }

  public GoogleTokenResponse refreshAccessToken(String refreshToken) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("refresh_token", refreshToken);
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());
    form.add("grant_type", GRANT_TYPE_REFRESH_TOKEN);
    return fetchTokens(form);
  }

  private GoogleTokenResponse fetchTokens(MultiValueMap<String, String> form) {
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
      throw new InvalidIdTokenException("Failed to parse id_token claims");
    }
  }
}
