package io.github.nikilsaini.outreach.auth.oauth.controller;

import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleIdTokenClaims;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.GoogleTokenResponse;
import io.github.nikilsaini.outreach.auth.oauth.dto.response.LoginResponse;
import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateUserRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.service.UserService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

  private final GoogleOAuthService googleOAuthService;
  private final UserService userService;

  @GetMapping("/login")
  public URI loginWithGoogle() {
    return googleOAuthService.buildAuthorizationUri();
  }

  @GetMapping("/callback")
  public LoginResponse handleGoogleCallback(@RequestParam("code") String code) {
    GoogleTokenResponse tokens = googleOAuthService.exchangeCodeForTokens(code);
    GoogleIdTokenClaims claims = googleOAuthService.decodeIdToken(tokens.idToken());

    CreateUserRequest request = new CreateUserRequest(
        claims.email(), claims.firstName(), claims.lastName(), tokens.refreshToken()
    );
    User user = userService.upsertFromGoogle(request);
    return new LoginResponse(user.getId(), user.getEmail());
  }
}
