package drs.client.net;

import drs.shared.exception.DrsException;

/**
 * Thrown when the client cannot reach the server (connection refused,
 * timeout, or socket closed mid-request).
  
 */
public class ServerOfflineException extends DrsException {

    private static final long serialVersionUID = 1L;

    public ServerOfflineException(String message) {
        super(message);
    }

    public ServerOfflineException(String message, Throwable cause) {
        super(message, cause);
    }
}
