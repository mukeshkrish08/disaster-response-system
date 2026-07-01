package drs.shared.exception;

/**
 * Generic wrapper for server-side errors that don't fit a specific
 * subclass. Used to relay server failures back to the client without
 * leaking implementation details.
  
 */
public class ServerException extends DrsException {

    private static final long serialVersionUID = 1L;

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
