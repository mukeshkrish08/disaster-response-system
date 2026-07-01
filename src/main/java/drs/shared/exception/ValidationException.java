package drs.shared.exception;

/**
 * Thrown when user input fails validation rules.
  
 */
public class ValidationException extends DrsException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
