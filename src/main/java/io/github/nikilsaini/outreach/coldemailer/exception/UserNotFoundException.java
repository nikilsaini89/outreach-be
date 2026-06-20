package io.github.nikilsaini.outreach.coldemailer.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UUID userId) {
    super("User not found with id: " + userId);
  }

  public UserNotFoundException(String email) {
    super("User not found with email: " + email);
  }
}
