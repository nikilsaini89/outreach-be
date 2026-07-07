package io.github.nikilsaini.outreach.coldemailer.exception;

import io.github.nikilsaini.outreach.auth.oauth.exception.InvalidIdTokenException;
import io.github.nikilsaini.outreach.coldemailer.exception.CampaignNotFoundException;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .findFirst()
        .orElse("Invalid request");
    log.atWarn().setMessage("Validation failed").addKeyValue("detail", message).log();
    return error(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
    log.atWarn().setMessage("User not found").addKeyValue("detail", ex.getMessage()).log();
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(CampaignNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleCampaignNotFound(CampaignNotFoundException ex) {
    log.atWarn().setMessage("Campaign not found").addKeyValue("detail", ex.getMessage()).log();
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(IllegalCampaignStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalCampaignState(IllegalCampaignStateException ex) {
    log.atWarn().setMessage("Rejected campaign state transition").addKeyValue("detail", ex.getMessage()).log();
    return error(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(InvalidIdTokenException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidIdToken(InvalidIdTokenException ex) {
    log.atWarn().setMessage("Invalid Google id_token").addKeyValue("detail", ex.getMessage()).log();
    return error(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidRecipientDomainException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidRecipientDomain(InvalidRecipientDomainException ex) {
    log.atWarn().setMessage("Invalid recipient domain").addKeyValue("detail", ex.getMessage()).log();
    return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(EncryptionException.class)
  public ResponseEntity<Map<String, Object>> handleEncryption(EncryptionException ex) {
    log.atError().setMessage("Cryptographic operation failed").setCause(ex).log();
    return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    log.atError().setMessage("Unhandled exception").addKeyValue("type", ex.getClass().getSimpleName()).setCause(ex).log();
    return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getSimpleName() + ": " + ex.getMessage());
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
