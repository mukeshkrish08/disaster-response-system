package drs.shared.exception;

/**
 * Thrown when login credentials are invalid or the account is inactive.
  
 */
public class AuthenticationException extends DrsException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
