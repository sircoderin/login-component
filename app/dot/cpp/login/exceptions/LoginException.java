package dot.cpp.login.exceptions;

import dot.cpp.login.constants.Error;

public class LoginException extends Exception {
  public LoginException(Error error) {
    super(error.getMessage());
  }
}
