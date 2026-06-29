package io.github.nikilsaini.outreach.auth.controller;

import io.github.nikilsaini.outreach.auth.jwt.dto.RefreshRequest;
import io.github.nikilsaini.outreach.auth.jwt.dto.TokenResponse;
import io.github.nikilsaini.outreach.auth.jwt.service.JwtService;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final JwtService jwtService;
  private final UserService userService;

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
    try {
      Claims claims = jwtService.parseClaims(request.refreshToken());
      if (!jwtService.isRefreshToken(claims)) {
        return ResponseEntity.status(401).build();
      }
      UUID userId = UUID.fromString(claims.getSubject());
      User user = userService.getById(userId);
      String newAuthToken = jwtService.generateAuthToken(userId, user.getEmail());
      log.atDebug().setMessage("Auth token refreshed").addKeyValue("userId", userId).log();
      return ResponseEntity.ok(new TokenResponse(newAuthToken));
    } catch (JwtException e) {
      log.atDebug().setMessage("Refresh token invalid").setCause(e).log();
      return ResponseEntity.status(401).build();
    }
  }
}
