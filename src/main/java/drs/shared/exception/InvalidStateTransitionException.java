package drs.shared.exception;

/**
 * Thrown when an incident or task is asked to transition to a state that
 * the state registry does not allow.
  
 */
public class InvalidStateTransitionException extends DrsException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
