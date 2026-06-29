package io.github.nikilsaini.outreach.auth.jwt.filter;

import io.github.nikilsaini.outreach.auth.jwt.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = jwtService.parseClaims(token);
        if (jwtService.isAuthToken(claims)) {
          UUID userId = UUID.fromString(claims.getSubject());
          SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken(userId, null, List.of())
          );
        }
      } catch (JwtException e) {
        log.atDebug().setMessage("JWT validation failed").setCause(e).log();
      }
    }
    chain.doFilter(request, response);
  }
}
