package drs.server.router;

import drs.server.Session;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

/**
 * A per-feature handler. {@link drs.server.RequestRouter} dispatches each
 * Request to the handler registered for its operation type.
  
 */
public interface RequestHandler {

    /*   * Handle one request and return a Response.
         * @param request the inbound request
     * @param session the active session (may be null for LOGIN)
     * @return a Response (never null)
     */
    Response handle(Request request, Session session);
}
