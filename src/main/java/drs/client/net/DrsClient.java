package drs.client.net;

import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Persistent client socket. Opens once on {@link #connect()}, then each
 * {@link #send(Request)} writes a Request and reads back a Response.
 *
 * Not thread-safe - JavaFX is single-threaded for UI work, and the
 * client is called from the UI thread or background tasks one at a time.
  
 */
public class DrsClient {

    private static final Logger LOG = LoggerFactory.getLogger(DrsClient.class);

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public DrsClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /*   * Open the socket and the object streams.
         * @throws ServerOfflineException if the server cannot be reached
     */
    public synchronized void connect() {
        if (isConnected()) {
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port),
                    ProtocolConstants.SOCKET_CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(ProtocolConstants.SOCKET_READ_TIMEOUT_MS);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            LOG.info("Connected to {}:{}", host, port);
        } catch (IOException e) {
            disconnect();
            throw new ServerOfflineException(
                    "Cannot reach the server at " + host + ":" + port, e);
        }
    }

    /*   * Send a Request and block for the Response.
         * @param request the Request (must have OperationType set)
     * @return the Response (never null)
     * @throws ServerOfflineException if the socket fails mid-operation
     */
    public synchronized Response send(Request request) {
        if (!isConnected()) {
            connect();
        }
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            Object obj = in.readObject();
            if (!(obj instanceof Response)) {
                throw new ServerOfflineException(
                        "Server returned an unexpected reply.");
            }
            return (Response) obj;
        } catch (SocketException | java.io.EOFException e) {
            disconnect();
            throw new ServerOfflineException(
                    "The server connection was lost.", e);
        } catch (IOException | ClassNotFoundException e) {
            disconnect();
            throw new ServerOfflineException(
                    "Network error: " + e.getMessage(), e);
        }
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized void disconnect() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) { /* No-op */ }
        try {
            if (out != null) out.close();
        } catch (IOException ignored) { /* No-op */ }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) { /* No-op */ }
        in = null;
        out = null;
        socket = null;
    }

    public String getHost() { return host; }
    public int getPort()    { return port; }
}
