package dot.cpp.login.service;

import com.google.gson.JsonObject;
import dot.cpp.login.constants.Constants;
import dot.cpp.login.constants.Error;
import dot.cpp.login.enums.UserRole;
import dot.cpp.login.exceptions.LoginException;
import dot.cpp.login.exceptions.UserException;
import dot.cpp.login.models.session.entity.Session;
import dot.cpp.login.models.session.repository.SessionRepository;
import dot.cpp.login.models.user.entity.User;
import dot.cpp.login.models.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoginService {

  private final SecretKey key;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  public final UserService userService;
  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;

  @Inject
  public LoginService(
      UserService userService, UserRepository userRepository, SessionRepository sessionRepository) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;

    key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
  }

  public JsonObject login(String userName, String password) throws LoginException {
    var user = userRepository.findByField("userName", userName);

    if (user == null) {
      throw new LoginException(Error.NOT_FOUND);
    } else {
      if (!userService.checkPassword(user.getPassword(), password)) {
        throw new LoginException(Error.INCORRECT_PASSWORD);
      }

      logger.debug("{}", user.getId());

      Date expirationDateRefresh = new Date();
      expirationDateRefresh.setTime(expirationDateRefresh.getTime() + 86400000L); // one day

      var session = new Session();
      var refreshToken = UUID.randomUUID().toString();
      session.setRefreshToken(refreshToken);
      session.setRefreshExpiryDate(expirationDateRefresh.getTime());
      session.setCreateTime(Instant.now().toEpochMilli());
      session.setUserId(user.getId().toString());
      sessionRepository.save(session);

      logger.debug("{}", session);

      final String accessToken = getAccessToken(user.getId().toString());

      final JsonObject tokens = new JsonObject();
      tokens.addProperty(Constants.ACCESS_TOKEN, accessToken);
      tokens.addProperty(Constants.REFRESH_TOKEN, refreshToken);

      logger.debug("{}", tokens);

      return tokens;
    }
  }

  private String getAccessToken(String userId) {
    Date expirationDateAccess = new Date();
    expirationDateAccess.setTime(expirationDateAccess.getTime() + 1200000L); // 20 minutes

    String jws =
        Jwts.builder()
            .setSubject(userId)
            .setExpiration(expirationDateAccess)
            .signWith(key)
            .compact();

    logger.debug("{}", jws);

    return jws;
  }

  public String checkJwtAndGetUserId(String jwtToken) throws LoginException {
    logger.debug("{}", jwtToken);

    final var claims = getJwsClaims(jwtToken).getBody();
    final var expirationDate = claims.getExpiration();

    if (expirationDate.before(new Date())) {
      throw new LoginException(Error.EXPIRED_ACCESS);
    }

    return claims.getSubject();
  }

  private Jws<Claims> getJwsClaims(String jwtToken) throws LoginException {
    try {
      return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtToken);
    } catch (Exception e) {
      logger.info("JWT error {}", e.getMessage());
      throw new LoginException(Error.INVALID_JWT);
    }
  }

  public User authorizeRequest(String accessToken, List<UserRole> permittedUserRoles)
      throws LoginException, UserException {
    final String userId = checkJwtAndGetUserId(accessToken);

    return userService.userIsActiveAndHasRole(userId, permittedUserRoles);
  }

  public JsonObject refreshTokens(String refreshToken) throws LoginException {
    final Session session = sessionRepository.findByField("refreshToken", refreshToken);

    if (session == null) {
      throw new LoginException(Error.SESSION_NOT_FOUND);
    }

    logger.debug("before refresh {}", session);

    Date expirationDateRefresh = new Date();
    expirationDateRefresh.setTime(expirationDateRefresh.getTime() + 86400000L); // one day
    final String newRefreshToken = UUID.randomUUID().toString();

    session.setRefreshExpiryDate(expirationDateRefresh.getTime());
    session.setRefreshToken(newRefreshToken);
    sessionRepository.save(session);

    logger.debug("after refresh {}", session);

    final String accessToken = getAccessToken(session.getUserId());

    final JsonObject tokens = new JsonObject();
    tokens.addProperty(Constants.ACCESS_TOKEN, accessToken);
    tokens.addProperty(Constants.REFRESH_TOKEN, refreshToken);

    logger.debug("refreshed tokens {}", tokens);
    return tokens;
  }

  public void logout(String userId) throws UserException {
    final var session = sessionRepository.findByField("userId", userId);
    if (session == null) {
      throw new UserException(Error.SESSION_NOT_FOUND);
    }

    sessionRepository.delete(session);
  }
}
