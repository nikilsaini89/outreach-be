package io.github.nikilsaini.outreach.auth.oauth.dto.request;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public record RefreshTokenRequest(
    String refreshToken,
    String clientId,
    String clientSecret,
    String grantType
) {
  public MultiValueMap<String, String> toFormParams() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("refresh_token", refreshToken);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("grant_type", grantType);
    return form;
  }
}
