package drs.shared.exception;

/**
 * Thrown when an authenticated user attempts an operation their role does
 * not permit.
  
 */
public class AuthorizationException extends DrsException {

    private static final long serialVersionUID = 1L;

    public AuthorizationException(String message) {
        super(message);
    }
}
