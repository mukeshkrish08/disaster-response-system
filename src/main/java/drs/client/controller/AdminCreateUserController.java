package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.UserRole;
import drs.shared.model.Department;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Admin: create a staff account.
 *
 * Role determines whether a Department selection is required.
 * AGENCY_REP and TEAM_LEADER must have a department; COORDINATOR and
 * ADMIN must not. CITIZEN is forbidden (citizens self-register).
  
 */
public class AdminCreateUserController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<UserRole> roleCombo;
    @FXML private Label departmentLabel;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private javafx.scene.control.Button createButton;

    @FXML
    public void initialize() {
        populateUserHeader();
        // Hide CITIZEN - citizens self-register via login screen.
        roleCombo.setItems(FXCollections.observableArrayList(
                UserRole.COORDINATOR,
                UserRole.TEAM_LEADER,
                UserRole.AGENCY_REP,
                UserRole.ADMIN));

        // Toggle department visibility whenever role changes.
        roleCombo.valueProperty().addListener(
                (obs, oldVal, newVal) -> updateDepartmentVisibility(newVal));
        // Initial state: nothing selected → hide.
        updateDepartmentVisibility(null);

        loadDepartments();
    }

    /*   * Show or hide the Department selector based on the chosen role.
     * AGENCY_REP / TEAM_LEADER show it; COORDINATOR / ADMIN hide it.
     */
    private void updateDepartmentVisibility(UserRole role) {
        boolean show = role == UserRole.AGENCY_REP
                || role == UserRole.TEAM_LEADER;
        if (departmentLabel != null) {
            departmentLabel.setVisible(show);
            departmentLabel.setManaged(show);
        }
        if (departmentCombo != null) {
            departmentCombo.setVisible(show);
            departmentCombo.setManaged(show);
            if (!show) {
                departmentCombo.getSelectionModel().clearSelection();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDepartments() {
        try {
            Request request = new Request(OperationType.LIST_DEPARTMENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                List<Department> depts = response.dataAs();
                departmentCombo.setItems(
                        FXCollections.observableArrayList(depts));
            } else {
                statusLabel.setText("Could not load departments: "
                        + response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onCreate() {
        // ---- Client-side guardrails (server re-validates) ----
        String fullName = fullNameField.getText();
        if (fullName == null || fullName.trim().length() < 2) {
            statusLabel.setText("Full name is required.");
            return;
        }
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()
                || !email.contains("@")) {
            statusLabel.setText("A valid email is required.");
            return;
        }
        UserRole role = roleCombo.getValue();
        if (role == null) {
            statusLabel.setText("Choose a role.");
            return;
        }
        String password = passwordField.getText();
        if (password == null || password.length() < 8) {
            statusLabel.setText(
                    "Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirmPasswordField.getText())) {
            statusLabel.setText("Passwords do not match.");
            return;
        }
        // Department gates
        Department dept = departmentCombo.getValue();
        if ((role == UserRole.AGENCY_REP || role == UserRole.TEAM_LEADER)
                && dept == null) {
            statusLabel.setText(
                    "Department is required for " + role + " accounts.");
            return;
        }

        // ---- Send to server ----
        if (createButton != null) createButton.setDisable(true);
        try {
            Request request = new Request(OperationType.CREATE_STAFF_USER,
                    ClientSession.instance().getSessionToken());
            request.with("fullName", fullName.trim());
            request.with("email", email.trim());
            request.with("password", password);
            request.with("role", role.name());
            if (dept != null) {
                request.with("departmentCode", dept.getDepartmentCode());
            }
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Staff user created.");
                SceneNavigator.showView("/fxml/admin-panel-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
                if (createButton != null) createButton.setDisable(false);
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
            if (createButton != null) createButton.setDisable(false);
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/admin-panel-view.fxml");
    }

    /*   * Fill the header right-side labels with the currently logged-in
     * user's name and role.
     */
    private void populateUserHeader() {
        User u = ClientSession.instance().getCurrentUser();
        if (u == null) {
            return;
        }
        if (userNameLabel != null) {
            userNameLabel.setText(u.getFullName());
        }
        if (userRoleLabel != null) {
            userRoleLabel.setText(u.getRole() == null
                    ? "" : u.getRole().name());
        }
    }

    /*   * Sign the current user out and return to the login screen.
     */
    @FXML
    public void onSignOut() {
        try {
            Request request = new Request(OperationType.LOGOUT,
                    ClientSession.instance().getSessionToken());
            ClientSession.instance().getDrsClient().send(request);
        } catch (Exception ignored) {
            // Best effort
        }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }
}
