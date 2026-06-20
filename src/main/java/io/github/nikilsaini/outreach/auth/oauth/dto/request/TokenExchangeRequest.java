package io.github.nikilsaini.outreach.auth.oauth.dto.request;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public record TokenExchangeRequest(
    String code,
    String clientId,
    String clientSecret,
    String redirectUri,
    String grantType
) {
  public MultiValueMap<String, String> toFormParams() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("grant_type", grantType);
    return form;
  }
}
