package drs.shared.exception;

/**
 * Thrown by geo-dispatch when no suitable team can be found.
  
 */
public class DispatchException extends DrsException {

    private static final long serialVersionUID = 1L;

    public DispatchException(String message) {
        super(message);
    }
}
