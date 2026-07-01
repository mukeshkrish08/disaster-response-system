package drs.shared.model;

import drs.shared.enums.UserRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A registered user of the DRS system. The {@code passwordHash} field
 * stores a BCrypt hash, never plaintext.
  
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userPk;
    private String userCode;
    private String fullName;
    private String email;
    private String passwordHash;
    private UserRole role;
    /*   * Department that this user belongs to. NULL for citizens,
     * coordinators, and admins. Populated for agency_rep (whose
     * inbox is filtered to this department) and optionally for
     * team_leader. Stored as nullable Integer to preserve JDBC NULL.
     */
    private Integer departmentPk;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {
        // No-arg for JDBC/Serialization
    }

    public User(String userCode, String fullName, String email,
                String passwordHash, UserRole role) {
        this.userCode = userCode;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public int getUserPk() { return userPk; }
    public void setUserPk(int userPk) { this.userPk = userPk; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Integer getDepartmentPk() { return departmentPk; }
    public void setDepartmentPk(Integer departmentPk) {
        this.departmentPk = departmentPk;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userPk == user.userPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userPk);
    }

    @Override
    public String toString() {
        return "User{" + userCode + ", " + fullName + " (" + role + ")}";
    }
}
