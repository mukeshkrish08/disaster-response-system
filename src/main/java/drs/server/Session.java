package drs.server;

import drs.shared.enums.UserRole;
import drs.shared.model.User;

import java.time.LocalDateTime;

/**
 * Represents an authenticated session. Stored in {@link SessionManager}
 * keyed by token. Fields are mostly immutable; only {@code lastActivity}
 * is updated each time the token is validated.
  
 */
public class Session {

    private final String token;
    private final int userPk;
    private final String userCode;
    private final String fullName;
    private final UserRole role;
    private final String clientIp;
    private final LocalDateTime createdAt;
    private volatile LocalDateTime lastActivity;

    public Session(String token, User user, String clientIp) {
        this.token = token;
        this.userPk = user.getUserPk();
        this.userCode = user.getUserCode();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.clientIp = clientIp;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = this.createdAt;
    }

    public String getToken()            { return token; }
    public int getUserPk()               { return userPk; }
    public String getUserCode()          { return userCode; }
    public String getFullName()          { return fullName; }
    public UserRole getRole()            { return role; }
    public String getClientIp()          { return clientIp; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getLastActivity() { return lastActivity; }

    /** Package-private: only SessionManager updates this. */
    void touch() {
        this.lastActivity = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Session{" + userCode + " (" + role + "), token=" + token + "}";
    }
}
