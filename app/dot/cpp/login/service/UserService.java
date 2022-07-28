package dot.cpp.login.service;

import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.Password;
import com.password4j.types.Argon2;
import com.typesafe.config.Config;
import dot.cpp.core.services.EntityService;
import dot.cpp.login.constants.Error;
import dot.cpp.login.constants.UserStatus;
import dot.cpp.login.enums.UserRole;
import dot.cpp.login.exceptions.UserException;
import dot.cpp.login.models.user.entity.User;
import dot.cpp.login.models.user.repository.UserRepository;
import dot.cpp.login.models.user.request.AcceptInviteRequest;
import dot.cpp.login.models.user.request.ResetPasswordRequest;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserService extends EntityService<User> {
  private final String passwordPepper;
  private final Argon2Function argon2 = Argon2Function.getInstance(1000, 4, 2, 32, Argon2.ID, 19);

  @Inject
  public UserService(UserRepository userRepository, Config config) {
    super(userRepository);
    this.passwordPepper = config.getString("password.pepper");
  }

  public String generateUserWithInvitation(String email, UserRole userRole) {
    User user = new User();
    final String resetPasswordUuid = UUID.randomUUID().toString();

    user.setEmail(email);
    user.setRole(userRole);
    user.setUserName("temporary");
    user.setPassword("temporary");
    user.setStatus(UserStatus.INACTIVE);
    user.setResetPasswordUuid(resetPasswordUuid);

    repository.save(user);

    return resetPasswordUuid;
  }

  public String generateResetPasswordUuid(String email) throws UserException {
    final User user = repository.findByField("email", email);

    if (user == null) {
      throw new UserException(Error.USER_EMAIL_NOT_FOUND);
    }

    if (!user.isActive()) {
      throw new UserException(Error.ACCOUNT_INACTIVE);
    }

    final String resetPasswordUuid = UUID.randomUUID().toString();
    user.setResetPasswordUuid(resetPasswordUuid);
    repository.save(user);

    logger.debug("{}", user);
    return resetPasswordUuid;
  }

  public User resetPassword(ResetPasswordRequest resetPasswordRequest, String resetPasswordUuid)
      throws UserException {
    logger.debug("{}", resetPasswordRequest);
    logger.debug("{}", resetPasswordUuid);

    final var user = repository.findByField("resetPasswordUuid", resetPasswordUuid);

    if (user == null) {
      throw new UserException(Error.NOT_FOUND);
    }

    final Hash hashedPassword = getHashedPassword(resetPasswordRequest.getPassword());

    logger.debug("{}", hashedPassword);
    user.setPassword(hashedPassword.getResult());
    user.setResetPasswordUuid("");

    logger.debug("{}", user);
    repository.save(user);
    return user;
  }

  public boolean checkPassword(String hashedPassword, String password) {
    boolean verified =
        Password.check(password, hashedPassword).addPepper(passwordPepper).with(argon2);
    logger.debug("verified {}", verified);
    return verified;
  }

  public User userIsActiveAndHasRole(String userId, List<UserRole> userRoles) throws UserException {
    final var user = repository.findById(userId);

    if (user == null) {
      throw new UserException(Error.NOT_FOUND);
    } else {
      logger.debug("{}", user);

      if (!user.isActive()) {
        throw new UserException(Error.ACCOUNT_INACTIVE);
      }
      if (!userRoles.isEmpty() && !userRoles.contains(user.getRole())) {
        throw new UserException(Error.USER_ROLE_MISMATCH);
      }
    }

    return user;
  }

  public User acceptInvitation(AcceptInviteRequest acceptInviteRequest, String resetPasswordUuid) {
    logger.debug("{}", acceptInviteRequest);
    logger.debug("{}", resetPasswordUuid);

    final var user = repository.findByField("resetPasswordUuid", resetPasswordUuid);

    final Hash hashedPassword = getHashedPassword(acceptInviteRequest.getPassword());

    user.setPassword(hashedPassword.getResult());
    user.setUserName(acceptInviteRequest.getUsername());
    user.setResetPasswordUuid("");
    user.setStatus(UserStatus.ACTIVE);

    logger.debug("{}", user);
    repository.save(user);
    return user;
  }

  private Hash getHashedPassword(String password) {
    return Password.hash(password).addRandomSalt(16).addPepper(passwordPepper).with(argon2);
  }
}
