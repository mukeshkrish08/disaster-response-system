package drs.server;

import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * One {@code ClientHandler} runs per accepted client connection in a
 * server thread pool. Reads Request objects, dispatches to the
 * RequestRouter, writes Response objects, and loops until the client
 * closes the socket or sends LOGOUT.
  
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final RequestRouter router;
    private final SessionManager sessionManager;
    private final String remoteAddress;

    public ClientHandler(Socket socket, RequestRouter router,
                         SessionManager sessionManager) {
        this.socket = socket;
        this.router = router;
        this.sessionManager = sessionManager;
        this.remoteAddress = socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void run() {
        LOG.info("Client connected from {}", remoteAddress);
        try {
            socket.setSoTimeout(ProtocolConstants.SOCKET_READ_TIMEOUT_MS);
        } catch (IOException ignored) {
            // Non-fatal
        }
        // Outer try opens the streams; inner loop reads / writes
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.flush();
            while (!socket.isClosed()) {
                Request request;
                try {
                    Object obj = in.readObject();
                    if (!(obj instanceof Request)) {
                        Response err = Response.error(
                                ProtocolConstants.ERR_INTERNAL,
                                "Expected Request, got "
                                        + (obj == null ? "null"
                                                       : obj.getClass().getName()));
                        out.writeObject(err);
                        out.flush();
                        out.reset();
                        continue;
                    }
                    request = (Request) obj;
                } catch (java.net.SocketTimeoutException e) {
                    LOG.info("Client {} idle timeout - closing", remoteAddress);
                    break;
                } catch (java.io.EOFException e) {
                    LOG.info("Client {} disconnected", remoteAddress);
                    break;
                }

                Session session = null;
                if (request.getSessionToken() != null) {
                    session = sessionManager
                            .validateToken(request.getSessionToken())
                            .orElse(null);
                }
                Response response = router.route(request, session);
                out.writeObject(response);
                out.flush();
                // Reset the stream's handle table - otherwise repeated
                // identical objects get cached and never re-sent.
                out.reset();
            }
        } catch (Exception e) {
            LOG.warn("Client {} error: {}", remoteAddress, e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // No-op
            }
            LOG.info("Client {} session closed", remoteAddress);
        }
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
}
