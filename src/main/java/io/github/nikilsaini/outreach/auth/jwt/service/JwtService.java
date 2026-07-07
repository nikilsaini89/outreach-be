package io.github.nikilsaini.outreach.auth.jwt.service;

import io.github.nikilsaini.outreach.auth.jwt.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtProperties jwtProperties;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAuthToken(UUID userId, String email, String name) {
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("name", name)
        .claim("type", "auth")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.authExpiryMs()))
        .signWith(key())
        .compact();
  }

  public String generateRefreshToken(UUID userId) {
    return Jwts.builder()
        .subject(userId.toString())
        .claim("type", "refresh")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.refreshExpiryMs()))
        .signWith(key())
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isAuthToken(Claims claims) {
    return "auth".equals(claims.get("type", String.class));
  }

  public boolean isRefreshToken(Claims claims) {
    return "refresh".equals(claims.get("type", String.class));
  }
}
