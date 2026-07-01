package drs.server;

import drs.shared.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe session store, keyed by UUID token. Sessions expire after
 * a configurable idle timeout; a background sweep thread removes expired
 * sessions every 60 seconds.
  
 */
public class SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    private final ConcurrentHashMap<String, Session> sessions =
            new ConcurrentHashMap<>();
    private final long timeoutMillis;
    private final ScheduledExecutorService sweeper;

    public SessionManager(int timeoutMins) {
        this.timeoutMillis = TimeUnit.MINUTES.toMillis(timeoutMins);
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-sweeper");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleAtFixedRate(this::sweepExpired, 60, 60, TimeUnit.SECONDS);
    }

    /*   * Create a new session for a freshly authenticated user.
         * @param user      authenticated user
     * @param clientIp  socket remote address (nullable)
     * @return new Session
     */
    public Session createSession(User user, String clientIp) {
        String token = UUID.randomUUID().toString();
        Session s = new Session(token, user, clientIp);
        sessions.put(token, s);
        LOG.info("Session created for {} (token suffix ...{})",
                user.getUserCode(),
                token.substring(token.length() - 8));
        return s;
    }

    /*   * Look up and refresh a session by token. Returns empty if missing
     * or expired (in which case the entry is purged).
         * @param token candidate token
     * @return live Session if valid
     */
    public Optional<Session> validateToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Session s = sessions.get(token);
        if (s == null) {
            return Optional.empty();
        }
        if (isExpired(s)) {
            sessions.remove(token);
            return Optional.empty();
        }
        s.touch();
        return Optional.of(s);
    }

    /** Immediately invalidate a session. */
    public void invalidate(String token) {
        if (token != null && sessions.remove(token) != null) {
            LOG.info("Session invalidated (token suffix ...{})",
                    token.substring(Math.max(0, token.length() - 8)));
        }
    }

    /** Number of currently active sessions. */
    public int activeSessionCount() {
        return sessions.size();
    }

    /** Remove all expired sessions. Called periodically. */
    public void sweepExpired() {
        sessions.entrySet().removeIf(e -> isExpired(e.getValue()));
    }

    private boolean isExpired(Session s) {
        long idleMillis = java.time.Duration.between(
                s.getLastActivity(), LocalDateTime.now()).toMillis();
        return idleMillis > timeoutMillis;
    }

    /** Stop the sweep thread (called on server shutdown). */
    public void shutdown() {
        sweeper.shutdownNow();
    }
}
