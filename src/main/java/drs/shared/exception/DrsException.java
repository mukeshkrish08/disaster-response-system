package drs.shared.exception;

/**
 * Base class for all DRS-specific exceptions. Extends RuntimeException so
 * callers are not forced to declare every business condition in throws
 * clauses; specific subclasses signal the exact condition.
 *
 * Implements Serializable so it can travel inside a {@code Response}
 * across the socket.
  
 */
public class DrsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DrsException(String message) {
        super(message);
    }

    public DrsException(String message, Throwable cause) {
        super(message, cause);
    }
}
