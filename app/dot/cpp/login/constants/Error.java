package dot.cpp.login.constants;

public enum Error {
  NOT_FOUND("Not found"),
  INCORRECT_PASSWORD("Incorrect password"),
  EXPIRED_ACCESS("Access token expired"),
  USER_ROLE_MISMATCH("User does not have role"),
  USER_EMAIL_NOT_FOUND("No user associated with the given email was found"),
  ACCOUNT_INACTIVE("Account is inactive"),
  SESSION_NOT_FOUND("Session not found"),
  INVALID_JWT("Invalid JWT"),
  RESET_PASS_UUID_NOT_FOUND("Reset password UUID not found");

  final String message;

  Error(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
