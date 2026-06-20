package io.github.nikilsaini.outreach.auth.oauth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleIdTokenClaims(
    @JsonProperty("sub") String subject,
    @JsonProperty("email") String email,
    @JsonProperty("given_name") String firstName,
    @JsonProperty("family_name") String lastName
) {}
