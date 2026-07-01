package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.AuditLog;
import drs.shared.model.Department;
import drs.shared.model.Location;
import drs.shared.model.ResponseTeam;
import drs.shared.model.Resource;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import drs.client.ui.StatusFeedback;

import java.util.List;
import java.util.Map;

/**
 * Admin panel - users, departments, teams, resources, audit log,
 * verify-chain button.
  
 */
public class AdminPanelController {

    @FXML private TabPane adminTabs;
    @FXML private ListView<String> usersList;
    @FXML private ListView<String> departmentsList;
    @FXML private ListView<String> teamsList;
    @FXML private ListView<String> locationsList;
    @FXML private ListView<String> resourcesList;
    @FXML private ListView<String> auditEntriesList;
    @FXML private Label verifyChainStatusLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        // Populate header user info from active session
        User user = ClientSession.instance().getCurrentUser();
        if (user != null) {
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole() == null
                        ? "" : user.getRole().name());
            }
        }
        loadAll();
    }

    @FXML
    public void onRefresh() {
        loadAll();
    }

    private void loadAll() {
        loadUsers();
        loadDepartments();
        loadTeams();
        loadLocations();
        loadResources();
        loadAuditLog();
    }

    private void loadUsers() {
        try {
            Response r = send(OperationType.LIST_USERS);
            if (r.isSuccess()) {
                List<User> list = r.dataAs();
                usersList.setItems(FXCollections.observableArrayList(
                        list.stream().map(u -> u.getUserCode() + " | "
                                + u.getFullName() + " | "
                                + u.getRole().displayName()
                                + (u.isActive() ? "" : " [INACTIVE]"))
                                .toList()));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    private void loadDepartments() {
        try {
            Response r = send(OperationType.LIST_DEPARTMENTS);
            if (r.isSuccess()) {
                List<Department> list = r.dataAs();
                departmentsList.setItems(FXCollections.observableArrayList(
                        list.stream().map(d -> d.getDepartmentCode() + " | "
                                + d.getName()
                                + (d.isActive() ? "" : " [INACTIVE]"))
                                .toList()));
            }
        } catch (ServerOfflineException e) {
            // already shown
        }
    }

    private void loadTeams() {
        try {
            Response r = send(OperationType.LIST_TEAMS);
            if (r.isSuccess()) {
                List<ResponseTeam> list = r.dataAs();
                teamsList.setItems(FXCollections.observableArrayList(
                        list.stream().map(t -> t.getTeamCode() + " | "
                                + t.getTeamName() + " ("
                                + t.getAvailability().displayName() + ")"
                                + (t.isActive() ? "" : " [INACTIVE]"))
                                .toList()));
            }
        } catch (ServerOfflineException e) {
            // already shown
        }
    }

    private void loadResources() {
        try {
            Response r = send(OperationType.LIST_RESOURCES);
            if (r.isSuccess()) {
                List<Resource> list = r.dataAs();
                resourcesList.setItems(FXCollections.observableArrayList(
                        list.stream().map(res -> res.getResourceCode() + " | "
                                + res.getResourceName() + " ("
                                + res.getQuantityAvailable() + "/"
                                + res.getQuantityTotal() + ")").toList()));
            }
        } catch (ServerOfflineException e) {
            // already shown
        }
    }

    private void loadAuditLog() {
        try {
            Response r = send(OperationType.LIST_AUDIT_LOG);
            if (r.isSuccess()) {
                List<AuditLog> list = r.dataAs();
                auditEntriesList.setItems(FXCollections.observableArrayList(
                        list.stream().map(a -> DateTimeUtil.format(a.getCreatedAt())
                                + " | " + a.getAction() + " | "
                                + (a.getUserCode() == null ? "(no user)"
                                        : a.getUserCode())
                                + " | " + (a.isSuccess() ? "OK" : "FAIL")).toList()));
            }
        } catch (ServerOfflineException e) {
            // already shown
        }
    }

    private void loadLocations() {
        try {
            Response r = send(OperationType.LIST_LOCATIONS);
            if (r.isSuccess()) {
                List<Location> list = r.dataAs();
                locationsList.setItems(FXCollections.observableArrayList(
                        list.stream().map(l -> l.getLocationCode() + " | "
                                + l.getDisplayName()
                                + (l.isActive() ? "" : " [INACTIVE]"))
                                .toList()));
            }
        } catch (ServerOfflineException e) {
            // already shown
        }
    }

    // -----------------------------------------------------------------
    // Add / Deactivate handlers.
    // The Add buttons navigate to dedicated forms; the Deactivate
    // buttons read the selected row's leading code, confirm, send the
    // matching DEACTIVATE_* operation, then refresh.
    // -----------------------------------------------------------------

    @FXML
    public void onAddUser() {
        SceneNavigator.showView("/fxml/admin-create-user-view.fxml");
    }

    @FXML
    public void onAddTeam() {
        SceneNavigator.showView("/fxml/admin-add-team-view.fxml");
    }

    @FXML
    public void onAddDepartment() {
        SceneNavigator.showView("/fxml/admin-add-department-view.fxml");
    }

    @FXML
    public void onAddLocation() {
        SceneNavigator.showView("/fxml/admin-add-location-view.fxml");
    }

    @FXML
    public void onDeactivateUser() {
        String code = extractCode(usersList.getSelectionModel()
                .getSelectedItem());
        if (code == null) {
            statusLabel.setText("Select a user first.");
            return;
        }
        if (!SceneNavigator.confirm("Deactivate user " + code + "?")) {
            return;
        }
        sendDeactivate(OperationType.DEACTIVATE_USER, "userCode", code,
                "User deactivated.");
    }

    @FXML
    public void onDeactivateTeam() {
        String code = extractCode(teamsList.getSelectionModel()
                .getSelectedItem());
        if (code == null) {
            statusLabel.setText("Select a team first.");
            return;
        }
        if (!SceneNavigator.confirm("Deactivate team " + code + "?")) {
            return;
        }
        sendDeactivate(OperationType.DEACTIVATE_TEAM, "teamCode", code,
                "Team deactivated.");
    }

    @FXML
    public void onDeactivateDepartment() {
        String code = extractCode(departmentsList.getSelectionModel()
                .getSelectedItem());
        if (code == null) {
            statusLabel.setText("Select a department first.");
            return;
        }
        if (!SceneNavigator.confirm(
                "Deactivate department " + code + "?")) {
            return;
        }
        sendDeactivate(OperationType.DEACTIVATE_DEPARTMENT,
                "departmentCode", code, "Department deactivated.");
    }

    @FXML
    public void onDeactivateLocation() {
        String code = extractCode(locationsList.getSelectionModel()
                .getSelectedItem());
        if (code == null) {
            statusLabel.setText("Select a location first.");
            return;
        }
        if (!SceneNavigator.confirm("Deactivate location " + code + "?")) {
            return;
        }
        sendDeactivate(OperationType.DEACTIVATE_LOCATION,
                "locationCode", code, "Location deactivated.");
    }

    /*   * Extract the leading "CODE" from a "CODE | name | ..." row.
     * Returns null if no row is selected or the row is malformed.
     */
    private String extractCode(String row) {
        if (row == null || row.isBlank()) {
            return null;
        }
        int pipe = row.indexOf('|');
        if (pipe < 0) {
            return row.trim();
        }
        return row.substring(0, pipe).trim();
    }

    /*   * Dispatch a single-field deactivate op and refresh the panel on
     * success.
     */
    private void sendDeactivate(OperationType op, String fieldName,
                                String code, String successMessage) {
        try {
            Request request = new Request(op,
                    ClientSession.instance().getSessionToken());
            request.with(fieldName, code);
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                // Success: auto-clear after a few seconds so the
                // status bar doesn't accumulate stale messages.
                StatusFeedback.show(statusLabel, successMessage);
                loadAll();
            } else {
                StatusFeedback.sticky(statusLabel,
                        response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            StatusFeedback.sticky(statusLabel, "● Server offline.");
        }
    }

    @FXML
    public void onVerifyChain() {
        try {
            Response r = send(OperationType.VERIFY_AUDIT_CHAIN);
            if (r.isSuccess()) {
                Map<String, Object> payload = r.dataAs();
                boolean valid = Boolean.TRUE.equals(payload.get("valid"));
                String stamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("HH:mm:ss"));
                verifyChainStatusLabel.setText(valid
                        ? "✓ Audit chain intact (verified at " + stamp + ")"
                        : "✗ Audit chain BROKEN (verified at " + stamp + ")");
                // Swap style class based on result
                verifyChainStatusLabel.getStyleClass().removeAll(
                        "verify-ok", "verify-fail", "lead");
                verifyChainStatusLabel.getStyleClass().add(
                        valid ? "verify-ok" : "verify-fail");
            } else {
                statusLabel.setText(r.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onSignOut() {
        try {
            send(OperationType.LOGOUT);
        } catch (Exception ignored) { /* No-op */ }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }

    private Response send(OperationType op) {
        Request request = new Request(op,
                ClientSession.instance().getSessionToken());
        return ClientSession.instance().getDrsClient().send(request);
    }
}
