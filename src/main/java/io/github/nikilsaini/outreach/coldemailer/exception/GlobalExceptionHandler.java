package io.github.nikilsaini.outreach.coldemailer.exception;

import io.github.nikilsaini.outreach.auth.oauth.exception.InvalidIdTokenException;
import io.github.nikilsaini.outreach.coldemailer.exception.CampaignNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(CampaignNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleCampaignNotFound(CampaignNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(InvalidIdTokenException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidIdToken(InvalidIdTokenException ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(EncryptionException.class)
  public ResponseEntity<Map<String, Object>> handleEncryption(EncryptionException ex) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of(
        "status", status.value(),
        "error", status.getReasonPhrase(),
        "message", message,
        "timestamp", Instant.now().toString()
    ));
  }
}
