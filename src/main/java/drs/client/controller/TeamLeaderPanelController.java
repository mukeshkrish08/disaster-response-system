package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.IncidentAssignment;
import drs.shared.model.RecoveryTask;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Team leader panel. Shows assignments + own recovery tasks; allows
 * starting / completing response and opening incidents.
  
 */
public class TeamLeaderPanelController {

    @FXML private Label teamHeading;
    @FXML private TableView<IncidentAssignment> assignedIncidentsTable;
    @FXML private TableColumn<IncidentAssignment, String> assnIncidentColumn;
    @FXML private TableColumn<IncidentAssignment, String> assnRoleColumn;
    @FXML private TableColumn<IncidentAssignment, String> assnStatusColumn;

    @FXML private TableView<RecoveryTask> myTasksTable;
    @FXML private TableColumn<RecoveryTask, String> taskCodeColumn;
    @FXML private TableColumn<RecoveryTask, String> taskIncidentColumn;
    @FXML private TableColumn<RecoveryTask, String> taskStatusColumn;

    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        if (myTasksTable != null) {
            myTasksTable.setPlaceholder(new javafx.scene.control.Label("No recovery tasks assigned to you."));
        }
        if (myTasksTable != null) {
            myTasksTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        if (assignedIncidentsTable != null) {
            assignedIncidentsTable.setPlaceholder(new javafx.scene.control.Label("No incidents assigned to your team yet."));
        }
        if (assignedIncidentsTable != null) {
            assignedIncidentsTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        User user = ClientSession.instance().getCurrentUser();
        // Populate header user info from the active session
        if (user != null) {
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole() == null
                        ? "" : user.getRole().name());
            }
        }
        teamHeading.setText("Team leader panel - "
                + (user == null ? "Unknown" : user.getFullName()));

        assnIncidentColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getIncidentCode()));
        assnRoleColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getRole().displayName()));
        assnStatusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getAssignmentStatus().displayName()));

        taskCodeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getTaskCode()));
        taskIncidentColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getIncidentCode()));
        taskStatusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));

        loadAssignments();
        loadMyTasks();
    }

    private void loadAssignments() {
        try {
            Request request = new Request(OperationType.LIST_INCIDENTS_BY_TEAM_LEADER,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<IncidentAssignment> list = response.dataAs();
                assignedIncidentsTable.setItems(FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    private void loadMyTasks() {
        try {
            Request request = new Request(OperationType.LIST_MY_RECOVERY_TASKS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<RecoveryTask> list = response.dataAs();
                myTasksTable.setItems(FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            // Already shown
        }
    }

    @FXML
    public void onStartResponse() {
        IncidentAssignment selected =
                assignedIncidentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an assignment first.");
            return;
        }
        if (!SceneNavigator.confirm("Start response for "
                + selected.getIncidentCode() + "?")) {
            return;
        }
        try {
            Request request = new Request(OperationType.START_RESPONSE,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", selected.getIncidentCode());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Response started for "
                        + selected.getIncidentCode());
                loadAssignments();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onCompleteResponse() {
        IncidentAssignment selected =
                assignedIncidentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an assignment first.");
            return;
        }
        if (!SceneNavigator.confirm("Mark "
                + selected.getIncidentCode() + " as resolved?")) {
            return;
        }

        // Resolution note is mandatory (≥10 chars). The coordinator
        // closing the incident later needs to see what your team
        // actually did on scene, and the audit chain needs it for
        // accountability.
        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Resolution note");
        dialog.setHeaderText("What did your team accomplish at "
                + selected.getIncidentCode() + "?");
        dialog.setContentText("Include status of casualties, damage, "
                + "and scene safety (min. 10 characters):");
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;     // user cancelled
        }
        String note = result.get().trim();
        if (note.length() < 10) {
            statusLabel.setText(
                    "Resolution note must be at least 10 characters.");
            return;
        }

        try {
            Request request = new Request(OperationType.COMPLETE_RESPONSE,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", selected.getIncidentCode());
            request.with("notes", note);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText(selected.getIncidentCode() + " resolved.");
                loadAssignments();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onViewIncident() {
        IncidentAssignment selected =
                assignedIncidentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an assignment first.");
            return;
        }
        SelectedIncidentHolder.selectedIncidentCode = selected.getIncidentCode();
        SceneNavigator.showView("/fxml/incident-details-view.fxml");
    }

    /*   * Reload assigned incidents and recovery tasks from the server.
     * Use this after the coordinator has updated something (e.g.
     * allocated more resources) so the team leader sees the latest
     * state without having to sign out and back in. The complete-
     * response flow already calls loadAssignments() so the manual
     * refresh is for the cases the controller cannot anticipate.
     */
    @FXML
    public void onRefresh() {
        loadAssignments();
        loadMyTasks();
        statusLabel.setText("Refreshed.");
    }

    @FXML
    public void onUpdateTask() {
        RecoveryTask selected = myTasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a task first.");
            return;
        }
        SelectedIncidentHolder.selectedIncidentCode = selected.getIncidentCode();
        SelectedIncidentHolder.selectedTaskCode = selected.getTaskCode();
        SceneNavigator.showView("/fxml/recovery-task-view.fxml");
    }

    @FXML
    public void onSignOut() {
        try {
            ClientSession.instance().getDrsClient().send(
                    new Request(OperationType.LOGOUT,
                            ClientSession.instance().getSessionToken()));
        } catch (Exception ignored) { /* No-op */ }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }
}
