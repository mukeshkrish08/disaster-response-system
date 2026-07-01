package drs.shared.exception;

/**
 * Thrown when a session token is no longer valid (expired or
 * invalidated).
  
 */
public class SessionExpiredException extends DrsException {

    private static final long serialVersionUID = 1L;

    public SessionExpiredException(String message) {
        super(message);
    }
}
