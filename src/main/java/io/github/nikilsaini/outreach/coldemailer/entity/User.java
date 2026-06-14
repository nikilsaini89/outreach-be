package io.github.nikilsaini.outreach.coldemailer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User extends BaseEntity{

  @NotBlank
  @Column(name = "first_name", nullable = false)
  private String firstName;

  @NotBlank
  @Column(name = "last_name", nullable = false)
  private String lastName;

  @NotBlank
  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @NotBlank
  @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
  private String encryptedRefreshToken;
}