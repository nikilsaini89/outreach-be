package io.github.nikilsaini.outreach.auth.oauth.controller;

import io.github.nikilsaini.outreach.auth.jwt.service.JwtService;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleIdTokenClaims;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleTokenResponse;
import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateUserRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.service.UserService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/oauth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

  private final GoogleOAuthService googleOAuthService;
  private final UserService userService;
  private final JwtService jwtService;

  @Value("${app.frontend-url}")
  private String frontendUrl;

  @GetMapping("/login")
  public URI loginWithGoogle() {
    log.atDebug().setMessage("Request: Google login").log();
    return googleOAuthService.buildAuthorizationUri();
  }

  @GetMapping("/callback")
  public ResponseEntity<Void> handleGoogleCallback(@RequestParam("code") String code) {
    log.atDebug().setMessage("Request: Google OAuth callback").log();
    GoogleTokenResponse tokens = googleOAuthService.exchangeCodeForTokens(code);
    GoogleIdTokenClaims claims = googleOAuthService.decodeIdToken(tokens.idToken());

    CreateUserRequest request = new CreateUserRequest(
        claims.email(), claims.firstName(), claims.lastName(), tokens.refreshToken()
    );
    User user = userService.upsertFromGoogle(request);
    log.atInfo().setMessage("Login complete").addKeyValue("userId", user.getId()).log();

    String name = (user.getFirstName() + " " + user.getLastName()).trim();
    String authToken = jwtService.generateAuthToken(user.getId(), user.getEmail(), name);
    String refreshToken = jwtService.generateRefreshToken(user.getId());

      URI location = UriComponentsBuilder.fromUriString(frontendUrl + "/")
          .queryParam("authToken", authToken)
          .queryParam("refreshToken", refreshToken)
          .build().toUri();
    return ResponseEntity.status(302).location(location).build();
  }
}
