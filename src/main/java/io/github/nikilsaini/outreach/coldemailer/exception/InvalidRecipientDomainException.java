package io.github.nikilsaini.outreach.coldemailer.exception;

public class InvalidRecipientDomainException extends RuntimeException {

  public InvalidRecipientDomainException(String domain) {
    super("Recipient domain has no mail servers: " + domain);
  }
}
