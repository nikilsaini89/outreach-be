package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.config.EncryptionProperties;
import io.github.nikilsaini.outreach.coldemailer.exception.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;

  private final EncryptionProperties properties;

  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Prepend IV to ciphertext so decrypt knows the IV
      byte[] ivPlusCiphertext = new byte[IV_LENGTH_BYTES + ciphertext.length];
      System.arraycopy(iv, 0, ivPlusCiphertext, 0, IV_LENGTH_BYTES);
      System.arraycopy(ciphertext, 0, ivPlusCiphertext, IV_LENGTH_BYTES, ciphertext.length);

      return Base64.getEncoder().encodeToString(ivPlusCiphertext);
    } catch (Exception e) {
      throw new EncryptionException("Encryption failed", e);
    }
  }

  public String decrypt(String encryptedValue) {
    try {
      byte[] ivPlusCiphertext = Base64.getDecoder().decode(encryptedValue);
      byte[] iv = Arrays.copyOfRange(ivPlusCiphertext, 0, IV_LENGTH_BYTES);
      byte[] ciphertext = Arrays.copyOfRange(ivPlusCiphertext, IV_LENGTH_BYTES, ivPlusCiphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new EncryptionException("Decryption failed", e);
    }
  }

  private SecretKey secretKey() {
    byte[] keyBytes = Base64.getDecoder().decode(properties.secretKey());
    return new SecretKeySpec(keyBytes, "AES");
  }
}
