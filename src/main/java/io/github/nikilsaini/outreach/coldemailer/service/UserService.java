package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.UserMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateUserRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.exception.UserNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final EncryptionService encryptionService;

  @Transactional
  public User upsertFromGoogle(CreateUserRequest request) {
    return userRepository.findByEmail(request.email())
        .map(existing -> updateRefreshToken(existing, request.refreshToken()))
        .orElseGet(() -> createUser(request));
  }

  public String getDecryptedRefreshToken(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    return encryptionService.decrypt(user.getEncryptedRefreshToken());
  }

  private User updateRefreshToken(User user, String refreshToken) {
    user.setEncryptedRefreshToken(encryptionService.encrypt(refreshToken));
    return userRepository.save(user);
  }

  private User createUser(CreateUserRequest request) {
    User user = UserMapper.toEntity(request);
    user.setEncryptedRefreshToken(encryptionService.encrypt(request.refreshToken()));
    return userRepository.save(user);
  }
}
