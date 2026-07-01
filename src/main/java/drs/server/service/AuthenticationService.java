package drs.server.service;

import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.server.util.PasswordHasher;
import drs.shared.enums.UserRole;
import drs.shared.exception.AuthenticationException;
import drs.shared.exception.ValidationException;
import drs.shared.model.User;
import drs.shared.util.InputValidator;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Verifies credentials against the BCrypt hash stored on the user row,
 * and handles citizen self-registration.
  
 */
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AuthenticationService(UserRepository userRepository,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /*   * Authenticate a user by email and plaintext password.
         * @param email             email address
     * @param plaintextPassword candidate password
     * @param clientIp          remote address (for audit)
     * @return the authenticated User
     * @throws AuthenticationException on bad credentials or inactive
     */
    public User authenticate(String email, String plaintextPassword,
                             String clientIp) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            auditService.logAction(null, "LOGIN_FAILED", "User", email,
                    "User not found", clientIp, false);
            throw new AuthenticationException(
                    "Email or password is incorrect.");
        }
        User user = opt.get();
        if (!user.isActive()) {
            auditService.logAction(user.getUserPk(), "LOGIN_FAILED",
                    "User", user.getUserCode(),
                    "Account inactive", clientIp, false);
            throw new AuthenticationException(
                    "Your account is inactive. Contact administration.");
        }
        if (!PasswordHasher.verify(plaintextPassword, user.getPasswordHash())) {
            auditService.logAction(user.getUserPk(), "LOGIN_FAILED",
                    "User", user.getUserCode(),
                    "Bad password", clientIp, false);
            throw new AuthenticationException(
                    "Email or password is incorrect.");
        }
        userRepository.updateLastLogin(user.getUserPk(), LocalDateTime.now());
        auditService.logAction(user.getUserPk(), "LOGIN_SUCCESS",
                "User", user.getUserCode(), null, clientIp, true);
        // Don't return the password hash to the client
        user.setPasswordHash(null);
        return user;
    }

    /*   * Register a new citizen account from a public self-service signup
     * form. The role is hardcoded to {@link UserRole#CITIZEN} so that
     * even a malicious client cannot register itself as a privileged
     * user.
         * @param fullName          full name to display on profile
     * @param email             unique email address (lower-cased)
     * @param plaintextPassword plaintext password (will be hashed)
     * @param clientIp          remote address (for audit)
     * @return the newly created citizen (without password hash)
     * @throws ValidationException if any field is invalid or the email
     *                            is already registered
     */
    public User registerCitizen(String fullName, String email,
                                String plaintextPassword,
                                String clientIp) {
        // ----- Input validation -------------------------------------
        if (fullName == null || fullName.trim().length() < 2
                || fullName.trim().length() > 80) {
            throw new ValidationException(
                    "Full name must be between 2 and 80 characters.");
        }
        if (!InputValidator.validateEmail(email)) {
            throw new ValidationException(
                    "Please provide a valid email address.");
        }
        if (!InputValidator.validatePassword(plaintextPassword)) {
            throw new ValidationException(
                    "Password must be at least 8 characters and include "
                            + "uppercase, lowercase, digit and special "
                            + "character.");
        }

        // ----- Uniqueness check (case-insensitive) ------------------
        String normalisedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalisedEmail).isPresent()) {
            // Audit the failed attempt so duplicate-signup attempts are
            // traceable without revealing which emails already exist.
            auditService.logAction(null, "REGISTER_FAILED", "User",
                    normalisedEmail, "Email already registered",
                    clientIp, false);
            throw new ValidationException(
                    "An account with this email already exists.");
        }

        // ----- Build user (role HARDCODED to CITIZEN) ---------------
        User user = new User();
        user.setUserCode(IdGenerator.generateUserCode());
        user.setFullName(fullName.trim());
        user.setEmail(normalisedEmail);
        user.setPasswordHash(PasswordHasher.hash(plaintextPassword));
        user.setRole(UserRole.CITIZEN);          // never from client
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        int newPk = userRepository.save(user);
        user.setUserPk(newPk);

        auditService.logAction(newPk, "REGISTER_CITIZEN", "User",
                user.getUserCode(), null, clientIp, true);

        // Don't return the password hash to the client
        user.setPasswordHash(null);
        return user;
    }

    /*   * Create a staff account from the admin panel. The role must be
     * one of COORDINATOR, TEAM_LEADER, AGENCY_REP, or ADMIN. Citizen
     * accounts cannot be provisioned through this path - citizens
     * self-register via {@link #registerCitizen}.
         * This method assumes the caller's session has already been
     * verified as ADMIN at the handler level.
         * @param fullName          full name (2..80 chars)
     * @param email             unique email address (normalised)
     * @param plaintextPassword plaintext password meeting strength rules
     * @param role              the staff role to assign (never CITIZEN)
     * @param departmentPk      department PK for AGENCY_REP / TEAM_LEADER;
     *                         must be null for COORDINATOR / ADMIN
     * @param adminUserCode     code of the admin doing the creation
     * @param clientIp          admin's client IP (for audit)
     * @return the new staff user (without password hash)
     * @throws ValidationException if any field is invalid or the role
     *                            constraints are violated
     */
    public User createStaffUser(String fullName, String email,
                                String plaintextPassword,
                                UserRole role,
                                Integer departmentPk,
                                String adminUserCode,
                                String clientIp) {
        // ----- Role guard --------------------------------------------
        if (role == null || role == UserRole.CITIZEN) {
            throw new ValidationException(
                    "Staff role must be one of COORDINATOR, "
                            + "TEAM_LEADER, AGENCY_REP, or ADMIN.");
        }
        // Department linkage required for AGENCY_REP; required for
        // TEAM_LEADER (so we can scope their incidents). For
        // COORDINATOR and ADMIN, department_pk must be unset.
        if ((role == UserRole.AGENCY_REP || role == UserRole.TEAM_LEADER)
                && departmentPk == null) {
            throw new ValidationException(
                    "Department is required for " + role.name()
                            + " accounts.");
        }
        if ((role == UserRole.COORDINATOR || role == UserRole.ADMIN)
                && departmentPk != null) {
            throw new ValidationException(
                    "Department should not be set for " + role.name()
                            + " accounts.");
        }

        // ----- Input validation --------------------------------------
        if (fullName == null || fullName.trim().length() < 2
                || fullName.trim().length() > 80) {
            throw new ValidationException(
                    "Full name must be between 2 and 80 characters.");
        }
        if (!InputValidator.validateEmail(email)) {
            throw new ValidationException(
                    "Please provide a valid email address.");
        }
        if (!InputValidator.validatePassword(plaintextPassword)) {
            throw new ValidationException(
                    "Password must be at least 8 characters and include "
                            + "uppercase, lowercase, digit and special "
                            + "character.");
        }

        // ----- Uniqueness check --------------------------------------
        String normalisedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalisedEmail).isPresent()) {
            auditService.logAction(null, "CREATE_STAFF_FAILED", "User",
                    normalisedEmail, "Email already registered",
                    clientIp, false);
            throw new ValidationException(
                    "An account with this email already exists.");
        }

        // ----- Build and save ----------------------------------------
        User user = new User();
        user.setUserCode(IdGenerator.generateUserCode());
        user.setFullName(fullName.trim());
        user.setEmail(normalisedEmail);
        user.setPasswordHash(PasswordHasher.hash(plaintextPassword));
        user.setRole(role);
        user.setDepartmentPk(departmentPk);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        int newPk = userRepository.save(user);
        user.setUserPk(newPk);

        auditService.logAction(newPk, "CREATE_STAFF_USER", "User",
                user.getUserCode(),
                "By admin " + adminUserCode + " as " + role.name(),
                clientIp, true);

        user.setPasswordHash(null);
        return user;
    }

    /*   * Deactivate a user account. Sets {@code active} to false; the
     * row stays so allocations, audit entries and incident reports
     * that reference this user remain intact.
     */
    public void deactivateUser(String targetUserCode,
                               String adminUserCode,
                               String clientIp) {
        java.util.Optional<User> opt =
                userRepository.findByCode(targetUserCode);
        if (opt.isEmpty()) {
            throw new ValidationException(
                    "User not found: " + targetUserCode);
        }
        User user = opt.get();
        if (!user.isActive()) {
            // Idempotent - already inactive
            return;
        }
        userRepository.setActive(user.getUserPk(), false);
        auditService.logAction(user.getUserPk(), "DEACTIVATE_USER",
                "User", user.getUserCode(),
                "By admin " + adminUserCode, clientIp, true);
    }
}
