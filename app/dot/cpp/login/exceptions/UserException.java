package dot.cpp.login.exceptions;

import dot.cpp.login.constants.Error;

public class UserException extends Exception {
  public UserException(Error error) {
    super(error.getMessage());
  }
}
