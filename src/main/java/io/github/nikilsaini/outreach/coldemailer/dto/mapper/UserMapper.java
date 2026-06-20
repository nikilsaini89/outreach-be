package io.github.nikilsaini.outreach.coldemailer.dto.mapper;

import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateUserRequest;
import io.github.nikilsaini.outreach.coldemailer.entity.User;

public class UserMapper {

  private UserMapper() {}

  public static User toEntity(CreateUserRequest request) {
    User user = new User();
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    return user;
  }
}
