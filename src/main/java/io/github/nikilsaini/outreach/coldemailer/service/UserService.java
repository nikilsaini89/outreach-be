package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.UserMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateUserRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.exception.UserNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final EncryptionService encryptionService;

  @Transactional
  public User upsertFromGoogle(CreateUserRequest request) {
    log.atInfo().setMessage("Upserting user from Google sign-in").addKeyValue("email", request.email()).log();
    return userRepository.findByEmail(request.email())
        .map(existing -> updateRefreshToken(existing, request.refreshToken()))
        .orElseGet(() -> createUser(request));
  }

  @Transactional(readOnly = true)
  public User getById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
  }

  @Transactional(readOnly = true)
  public boolean existsById(UUID userId) {
    return userRepository.existsById(userId);
  }

  public String getDecryptedRefreshToken(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    return encryptionService.decrypt(user.getEncryptedRefreshToken());
  }

  private User updateRefreshToken(User user, String refreshToken) {
    log.atDebug().setMessage("Updating user refresh token").addKeyValue("userId", user.getId()).log();
    user.setEncryptedRefreshToken(encryptionService.encrypt(refreshToken));
    return userRepository.save(user);
  }

  private User createUser(CreateUserRequest request) {
    User user = UserMapper.toEntity(request);
    user.setEncryptedRefreshToken(encryptionService.encrypt(request.refreshToken()));
    User saved = userRepository.save(user);
    log.atInfo().setMessage("User registered")
        .addKeyValue("userId", saved.getId())
        .addKeyValue("email", saved.getEmail())
        .log();
    return saved;
  }
}
