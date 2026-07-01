package drs.shared.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A single operation invocation sent from client to server.
 *
 * Every object placed in {@code payload} must implement
 * {@code Serializable}, otherwise the {@code ObjectOutputStream} will
 * throw {@code NotSerializableException} at runtime. All entity classes
 * in {@code drs.shared.model} are Serializable for this reason.
 *
 * Prefer passing whole entity objects in the payload over loose key-value
 * scalars where it makes sense - easier to evolve.
  
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionToken;
    private OperationType operation;
    private Map<String, Object> payload;

    public Request() {
        this.payload = new HashMap<>();
    }

    public Request(OperationType operation) {
        this();
        this.operation = operation;
    }

    public Request(OperationType operation, String sessionToken) {
        this(operation);
        this.sessionToken = sessionToken;
    }

    /*   * Add a value to the payload and return this Request for chaining.
         * @param key   payload key
     * @param value Serializable value
     * @return this Request
     */
    public Request with(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    /*   * Convenience: fetch a typed value from the payload.
         * @param key payload key
     * @param <T> caller-asserted type
     * @return value or null if absent
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) payload.get(key);
    }

    @Override
    public String toString() {
        return "Request{" + operation + ", payloadKeys=" + payload.keySet() + "}";
    }
}
