package drs.server.service;

import drs.server.repository.UserRepository;
import drs.shared.enums.UserRole;
import drs.shared.exception.ValidationException;
import drs.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AuthenticationService}.
 *
 * These tests exercise the role and password validation rules that
 * run BEFORE any database calls happen, so they work without a live
 * MySQL connection.  Repository and audit-service collaborators are
 * stubbed.
 *
 * Covers both citizen self-registration and admin-driven staff
 * creation, with particular attention to:
 *
 * <ul>
 *  <li>citizen role is always {@code CITIZEN}, even if request payload
 *      smuggles a privileged role;</li>
 *  <li>admin {@code createStaffUser} explicitly forbids {@code CITIZEN};</li>
 *  <li>{@code AGENCY_REP} and {@code TEAM_LEADER} require a department,
 *      while {@code COORDINATOR} and {@code ADMIN} must not have one;</li>
 *  <li>password strength rules are enforced.</li>
 * </ul>
  
 */
class AuthenticationServiceTest {

    private AuthenticationService auth;

    @BeforeEach
    void setUp() {
        // Stubs that satisfy the constructor but never write to disk.
        // Repository's findByEmail returns empty so uniqueness checks
        // always pass; the role/validation tests below abort before
        // either stub is invoked further.
        UserRepository repoStub = new UserRepository() {
            @Override
            public Optional<User> findByEmail(String email) {
                return Optional.empty();
            }
            @Override
            public int save(User u) {
                u.setUserPk(1);
                return 1;
            }
        };
        AuditService auditStub = new AuditService(null) {
            @Override
            public synchronized drs.shared.model.AuditLog logAction(
                    Integer userPk, String action,
                    String entityType, String entityCode,
                    String details, String clientIp,
                    boolean success) {
                // no-op stub; AuditService logs are not under test
                return null;
            }
        };
        auth = new AuthenticationService(repoStub, auditStub);
    }

    // -------- registerCitizen ---------------------------------------

    @Test
    @DisplayName("registerCitizen with valid data creates CITIZEN role")
    void registerCitizenAssignsCitizenRole() {
        User user = auth.registerCitizen(
                "Alex Morgan", "alex@example.com",
                "Strong@Pass1", "127.0.0.1");
        assertNotNull(user);
        assertEquals(UserRole.CITIZEN, user.getRole());
        assertNull(user.getPasswordHash(),
                "Password hash should be stripped before return");
    }

    @Test
    @DisplayName("registerCitizen rejects weak passwords")
    void registerCitizenRejectsWeakPassword() {
        assertThrows(ValidationException.class,
                () -> auth.registerCitizen("Alex Morgan",
                        "alex2@example.com", "weak", "127.0.0.1"));
    }

    @Test
    @DisplayName("registerCitizen rejects invalid email")
    void registerCitizenRejectsInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> auth.registerCitizen("Alex Morgan",
                        "not-an-email", "Strong@Pass1", "127.0.0.1"));
    }

    @Test
    @DisplayName("registerCitizen rejects short full name")
    void registerCitizenRejectsShortName() {
        assertThrows(ValidationException.class,
                () -> auth.registerCitizen("A",
                        "alex3@example.com", "Strong@Pass1", "127.0.0.1"));
    }

    // -------- createStaffUser ---------------------------------------

    @Test
    @DisplayName("createStaffUser rejects CITIZEN role explicitly")
    void createStaffUserRejectsCitizenRole() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Alex Morgan",
                        "alex@example.com", "Strong@Pass1",
                        UserRole.CITIZEN, null,
                        "USR-2026-0005", "127.0.0.1"));
        // The role-guard message contains "must be one of"
        assertEquals(true, ex.getMessage().contains("must be one of"));
    }

    @Test
    @DisplayName("createStaffUser rejects null role")
    void createStaffUserRejectsNullRole() {
        assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Alex Morgan",
                        "alex@example.com", "Strong@Pass1",
                        null, null,
                        "USR-2026-0005", "127.0.0.1"));
    }

    @Test
    @DisplayName("createStaffUser AGENCY_REP requires departmentPk")
    void agencyRepRequiresDepartment() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Helena Marsh",
                        "helena@example.com", "Strong@Pass1",
                        UserRole.AGENCY_REP, null,
                        "USR-2026-0005", "127.0.0.1"));
        assertEquals(true, ex.getMessage().contains("Department is required"));
    }

    @Test
    @DisplayName("createStaffUser TEAM_LEADER requires departmentPk")
    void teamLeaderRequiresDepartment() {
        assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Rachel Burke",
                        "rachel@example.com", "Strong@Pass1",
                        UserRole.TEAM_LEADER, null,
                        "USR-2026-0005", "127.0.0.1"));
    }

    @Test
    @DisplayName("createStaffUser COORDINATOR must NOT have departmentPk")
    void coordinatorMustNotHaveDepartment() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Sam Coord",
                        "sam@example.com", "Strong@Pass1",
                        UserRole.COORDINATOR, 1,
                        "USR-2026-0005", "127.0.0.1"));
        assertEquals(true, ex.getMessage().contains("should not be set"));
    }

    @Test
    @DisplayName("createStaffUser ADMIN must NOT have departmentPk")
    void adminMustNotHaveDepartment() {
        assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Sam Admin",
                        "samadmin@example.com", "Strong@Pass1",
                        UserRole.ADMIN, 1,
                        "USR-2026-0005", "127.0.0.1"));
    }

    @Test
    @DisplayName("createStaffUser AGENCY_REP with valid department succeeds")
    void agencyRepWithDepartmentSucceeds() {
        User user = auth.createStaffUser("Helena Marsh",
                "helena2@example.com", "Strong@Pass1",
                UserRole.AGENCY_REP, 1,
                "USR-2026-0005", "127.0.0.1");
        assertNotNull(user);
        assertEquals(UserRole.AGENCY_REP, user.getRole());
        assertEquals(Integer.valueOf(1), user.getDepartmentPk());
    }

    @Test
    @DisplayName("createStaffUser COORDINATOR without department succeeds")
    void coordinatorWithoutDepartmentSucceeds() {
        User user = auth.createStaffUser("Sam Coord",
                "sam2@example.com", "Strong@Pass1",
                UserRole.COORDINATOR, null,
                "USR-2026-0005", "127.0.0.1");
        assertNotNull(user);
        assertEquals(UserRole.COORDINATOR, user.getRole());
        assertNull(user.getDepartmentPk());
    }

    @Test
    @DisplayName("createStaffUser rejects weak password")
    void createStaffUserRejectsWeakPassword() {
        assertThrows(ValidationException.class,
                () -> auth.createStaffUser("Helena Marsh",
                        "helena3@example.com", "weak",
                        UserRole.AGENCY_REP, 1,
                        "USR-2026-0005", "127.0.0.1"));
    }
}
