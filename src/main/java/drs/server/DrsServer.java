package drs.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The multi-threaded Disaster Response System server. Listens on a TCP port, accepts
 * connections, and submits each one to a fixed thread pool for
 * processing by a {@link ClientHandler}.
 *
 * Bounded thread pool prevents resource exhaustion under load.
  
 */
public class DrsServer {

    private static final Logger LOG = LoggerFactory.getLogger(DrsServer.class);

    private final int port;
    private final int threadPoolSize;
    private final ServerApplicationContext context;
    private final RequestRouter router;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;

    public DrsServer(int port, int threadPoolSize,
                     ServerApplicationContext context) {
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.context = context;
        this.router = new RequestRouter(context);
    }

    /*   * Open the listening socket and accept connections until
     * {@link #shutdown()} is called.
     */
    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setName("drs-client-" + t.getId());
            t.setDaemon(false);
            return t;
        });
        this.running = true;
        LOG.info("Listening on 0.0.0.0:{} (thread pool size {})",
                port, threadPoolSize);

        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket,
                        router, context.getSessionManager());
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            if (running) {
                LOG.error("Accept loop error", e);
            }
            // If we were stopped, the IOException from accept() is expected
        } finally {
            shutdownInternal();
        }
    }

    /** Request a graceful shutdown of the server. */
    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // No-op
            }
        }
    }

    private void shutdownInternal() {
        LOG.info("Shutting down...");
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                threadPool.shutdownNow();
            }
        }
        context.getSessionManager().shutdown();
        LOG.info("Shutdown complete");
    }

    public boolean isRunning() {
        return running;
    }
}
