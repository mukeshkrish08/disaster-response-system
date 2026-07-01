package drs.shared.protocol;

import java.io.Serializable;

/**
 * The server's reply to a {@code Request}.
 *
 * Use one of the static factories ({@code ok}, {@code error}) rather than
 * the constructor for clarity. The {@code data} field is wrapped to be
 * Serializable - pass an entity, a List of entities, a String, or a
 * Map<String, Object>.
  
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private Object data;
    private String errorCode;
    private String errorMessage;

    public Response() {
        // No-arg for deserialisation
    }

    private Response(boolean success, Object data, String errorCode,
                     String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /*   * Build a successful response.
         * @param data payload (must be Serializable)
     * @return success Response
     */
    public static Response ok(Object data) {
        return new Response(true, data, null, null);
    }

    /** Build a successful response with no data payload. */
    public static Response ok() {
        return new Response(true, null, null, null);
    }

    /*   * Build an error response.
         * @param errorCode    short stable code (see ProtocolConstants)
     * @param errorMessage human-readable message
     * @return failure Response
     */
    public static Response error(String errorCode, String errorMessage) {
        return new Response(false, null, errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    /*   * Convenience accessor with caller-asserted type.
         * @param <T> caller-asserted type
     * @return data cast to T
     */
    @SuppressWarnings("unchecked")
    public <T> T dataAs() {
        return (T) data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return success
                ? "Response{ok, data=" + (data == null ? "null" : data.getClass().getSimpleName()) + "}"
                : "Response{error " + errorCode + ": " + errorMessage + "}";
    }
}
