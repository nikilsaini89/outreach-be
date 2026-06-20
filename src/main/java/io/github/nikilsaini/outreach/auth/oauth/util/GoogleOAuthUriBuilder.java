package io.github.nikilsaini.outreach.auth.oauth.util;

import io.github.nikilsaini.outreach.auth.oauth.config.GoogleOAuthProperties;
import io.github.nikilsaini.outreach.auth.oauth.constant.GoogleOAuthConstants;
import java.net.URI;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

public final class GoogleOAuthUriBuilder {

  private GoogleOAuthUriBuilder() {}

  public static URI buildAuthorizationUri(GoogleOAuthProperties properties) {
    return UriComponentsBuilder
        .fromUriString(properties.authUri())
        .queryParam("client_id", properties.clientId())
        .queryParam("redirect_uri", properties.redirectUri())
        .queryParam("response_type", GoogleOAuthConstants.RESPONSE_TYPE_CODE)
        .queryParam("scope", buildScope(properties.scopes()))
        .queryParam("access_type", GoogleOAuthConstants.ACCESS_TYPE_OFFLINE)
        .queryParam("prompt", GoogleOAuthConstants.PROMPT_CONSENT)
        .build()
        .toUri();
  }

  private static String buildScope(List<String> scopes) {
    return String.join(GoogleOAuthConstants.SCOPE_SEPARATOR, scopes);
  }
}
