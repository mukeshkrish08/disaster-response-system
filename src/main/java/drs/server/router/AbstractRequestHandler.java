package drs.server.router;

import drs.server.AuthorizationService;
import drs.server.ServerApplicationContext;
import drs.shared.protocol.Response;

/**
 * Common helpers for request handlers - quick access to the
 * {@link ServerApplicationContext} and the
 * {@link AuthorizationService}.
  
 */
public abstract class AbstractRequestHandler implements RequestHandler {

    protected final ServerApplicationContext context;
    protected final AuthorizationService authz;

    protected AbstractRequestHandler(ServerApplicationContext context) {
        this.context = context;
        this.authz = context.getAuthorizationService();
    }

    protected Response ok(Object data) {
        return Response.ok(data);
    }

    protected Response ok() {
        return Response.ok();
    }

    protected Response error(String code, String message) {
        return Response.error(code, message);
    }
}
