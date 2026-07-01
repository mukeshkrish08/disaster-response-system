package drs.shared.protocol;

/**
 * Constants for the wire protocol. Keeping these in one place means both
 * sides of the socket are guaranteed to agree.
  
 */
public final class ProtocolConstants {

    /** Default server port if drs.properties does not override. */
    public static final int DEFAULT_SERVER_PORT = 5050;

    /** Default server host if drs.properties does not override. */
    public static final String DEFAULT_SERVER_HOST = "localhost";

    /*   * Socket read timeout (milliseconds). Set to 30 minutes so that idle
     * pauses during demos or marker inspection do not drop the
     * connection. For production use this would be much shorter and the
     * client would auto-reconnect.
     */
    public static final int SOCKET_READ_TIMEOUT_MS = 30 * 60 * 1000;

    /** Connect timeout when client opens a socket (milliseconds). */
    public static final int SOCKET_CONNECT_TIMEOUT_MS = 5_000;

    /** Error code returned when the session token is missing or expired. */
    public static final String ERR_SESSION_EXPIRED = "SESSION_EXPIRED";

    /** Error code returned when the role check fails. */
    public static final String ERR_AUTHZ_DENIED = "AUTHZ_DENIED";

    /** Error code returned for bad credentials. */
    public static final String ERR_AUTH_FAILED = "AUTH_FAILED";

    /** Error code returned for client input validation failures. */
    public static final String ERR_VALIDATION = "VALIDATION";

    /** Error code returned for invalid state transitions. */
    public static final String ERR_INVALID_TRANSITION = "INVALID_TRANSITION";

    /** Error code returned for database failures. */
    public static final String ERR_DATA_ACCESS = "DATA_ACCESS";

    /** Error code returned when the requested entity does not exist. */
    public static final String ERR_NOT_FOUND = "NOT_FOUND";

    /** Error code for any other server-side failure. */
    public static final String ERR_INTERNAL = "INTERNAL";

    /** Error code for an unknown operation type. */
    public static final String ERR_UNKNOWN_OPERATION = "UNKNOWN_OPERATION";

    private ProtocolConstants() {
        // Constants holder - not instantiable
    }
}
