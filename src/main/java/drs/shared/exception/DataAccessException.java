package drs.shared.exception;

/**
 * Thrown when a database operation fails. Wraps the underlying SQL
 * exception so callers see a domain-meaningful error rather than a raw
 * JDBC failure.
  
 */
public class DataAccessException extends DrsException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
