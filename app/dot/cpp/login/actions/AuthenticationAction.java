package dot.cpp.login.actions;

import static dot.cpp.login.helpers.CookieHelper.getCookie;

import com.google.gson.JsonObject;
import dot.cpp.core.constants.Patterns;
import dot.cpp.login.annotations.Authentication;
import dot.cpp.login.constants.Constants;
import dot.cpp.login.exceptions.LoginException;
import dot.cpp.login.exceptions.UserException;
import dot.cpp.login.helpers.CookieHelper;
import dot.cpp.login.service.LoginService;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;

public class AuthenticationAction extends Action<Authentication> {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationAction.class);

  @Inject private MessagesApi languageService;
  @Inject private LoginService loginService;

  @Override
  public CompletionStage<Result> call(Request request) {

    final var messages = languageService.preferred(request);
    final var accessToken = CookieHelper.getCookieString(request, Constants.ACCESS_TOKEN);
    final var refreshToken = CookieHelper.getCookieString(request, Constants.REFRESH_TOKEN);
    final var authHeader = request.header(Http.HeaderNames.AUTHORIZATION).orElse("");

    logger.debug("Authentication");
    logger.debug("request: {}", request);
    logger.debug("authHeader: {}", authHeader);
    logger.debug("accessToken: {}", accessToken);
    logger.debug("refreshToken: {}", refreshToken);

    final var constructedAccessToken = constructToken(authHeader, accessToken);
    if (isEmpty(constructedAccessToken) || isInvalidJwt(constructedAccessToken)) {
      logger.warn("Token invalid {}", constructedAccessToken);
      return statusIfPresentOrResult(redirectWithError(messages));
    }
    logger.debug("{}", constructedAccessToken);

    try {
      final var user =
          loginService.authorizeRequest(
              accessToken, Arrays.stream(configuration.userRoles()).collect(Collectors.toList()));
      return delegate.call(request.addAttr(Constants.USER, user));
    } catch (LoginException | UserException e) {
      try {
        final JsonObject tokens = loginService.refreshTokens(refreshToken);
        logger.debug("{}", tokens);
        return getSuccessfulResult(tokens);
      } catch (LoginException loginException) {
        logger.debug("{}", e.getMessage());
        return statusIfPresentOrResult(redirectWithError(messages));
      }
    }
  }

  private CompletableFuture<Result> getSuccessfulResult(JsonObject tokens) {
    return CompletableFuture.completedFuture(
        ok().withCookies(
                getCookie(Constants.ACCESS_TOKEN, tokens.get(Constants.ACCESS_TOKEN).getAsString()),
                getCookie(
                    Constants.REFRESH_TOKEN, tokens.get(Constants.REFRESH_TOKEN).getAsString())));
  }

  private String constructToken(
      final String headerAuthorization, final String cookieAuthorization) {
    return isEmpty(headerAuthorization)
        ? cookieAuthorization
        : headerAuthorization.replace("Bearer", "").trim();
  }

  private boolean isInvalidJwt(String token) {
    return !token.matches(Patterns.JWT_TOKEN);
  }

  private CompletableFuture<Result> statusIfPresentOrResult(Result result) {
    final var status = configuration.status();
    if (status != -1) {
      return CompletableFuture.completedFuture(status(status));
    }
    return CompletableFuture.completedFuture(result);
  }

  private Result redirectWithError(Messages messages) {
    logger.debug("Session expired");
    return redirect(configuration.redirectUrl())
        .flashing("alert-danger", messages.apply("general.session.expired"));
  }

  private boolean isEmpty(String string) {
    return string == null || string.isBlank();
  }
}
