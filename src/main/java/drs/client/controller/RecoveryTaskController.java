package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;
import drs.shared.model.Department;
import drs.shared.model.RecoveryTask;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Recovery Task view (Feature 2).
 *
 * Lists existing tasks for an incident and lets coordinators create
 * new ones. Team leaders can update tasks assigned to them.
  
 */
public class RecoveryTaskController {

    @FXML private Label pageTitle;
    @FXML private TableView<RecoveryTask> existingTasksTable;
    @FXML private TableColumn<RecoveryTask, String> taskCodeColumn;
    @FXML private TableColumn<RecoveryTask, String> taskTypeColumn;
    @FXML private TableColumn<RecoveryTask, String> taskStatusColumn;
    @FXML private TableColumn<RecoveryTask, String> taskDepartmentColumn;

    @FXML private ComboBox<RecoveryTaskType> taskTypeCombo;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    private String incidentCode;

    @FXML
    public void initialize() {
        if (existingTasksTable != null) {
            existingTasksTable.setPlaceholder(new javafx.scene.control.Label("No recovery tasks for this incident yet."));
        }
        if (existingTasksTable != null) {
            existingTasksTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        populateUserHeader();
        incidentCode = SelectedIncidentHolder.selectedIncidentCode;
        if (incidentCode == null) {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            return;
        }
        pageTitle.setText("Recovery tasks for " + incidentCode);

        taskTypeCombo.setItems(FXCollections.observableArrayList(
                RecoveryTaskType.values()));

        taskCodeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getTaskCode()));
        taskTypeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getTaskType().displayName()));
        taskStatusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));
        // Department ownership replaces individual assignee. A recovery
        // task is owned by a responsible department/agency rather than
        // a specific user, which avoids the confusion of picking an
        // assignee from a different department than the task's owner.
        taskDepartmentColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getDepartmentCode() == null
                                ? ""
                                : c.getValue().getDepartmentCode()));

        loadDepartments();
        loadTasks();
    }

    private void loadDepartments() {
        try {
            Request request = new Request(OperationType.LIST_DEPARTMENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<Department> list = response.dataAs();
                departmentCombo.setItems(FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    private void loadTasks() {
        try {
            Request request = new Request(OperationType.LIST_RECOVERY_TASKS,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<RecoveryTask> list = response.dataAs();
                existingTasksTable.setItems(FXCollections.observableArrayList(list));

                // Auto-select the task the previous screen marked.
                String targetTaskCode = SelectedIncidentHolder.selectedTaskCode;
                if (targetTaskCode != null) {
                    for (RecoveryTask t : list) {
                        if (targetTaskCode.equals(t.getTaskCode())) {
                            existingTasksTable.getSelectionModel().select(t);
                            existingTasksTable.scrollTo(t);
                            statusLabel.setText("Task "
                                    + t.getTaskCode() + " selected.");
                            break;
                        }
                    }
                    // Clear hint so it doesn't persist next time
                    SelectedIncidentHolder.selectedTaskCode = null;
                }
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onCreateTask() {
        RecoveryTaskType type = taskTypeCombo.getValue();
        Department dept = departmentCombo.getValue();
        String desc = descriptionArea.getText() == null
                ? "" : descriptionArea.getText().trim();
        if (type == null) {
            statusLabel.setText("Choose a task type.");
            return;
        }
        if (dept == null) {
            statusLabel.setText("Choose a responsible department.");
            return;
        }
        if (desc.isEmpty()) {
            statusLabel.setText("Description is required.");
            return;
        }
        try {
            Request request = new Request(OperationType.CREATE_RECOVERY_TASK,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode)
                    .with("departmentCode", dept.getDepartmentCode())
                    .with("taskType", type.name())
                    .with("description", desc);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                RecoveryTask saved = response.dataAs();
                SceneNavigator.showInfo("Task " + saved.getTaskCode() + " created.");
                descriptionArea.clear();
                loadTasks();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onUpdateStatus() {
        RecoveryTask selected = existingTasksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a task first.");
            return;
        }
        ChoiceDialog<RecoveryTaskStatus> dialog = new ChoiceDialog<>(
                selected.getStatus(),
                Arrays.asList(RecoveryTaskStatus.values()));
        dialog.setHeaderText("New status for " + selected.getTaskCode());
        Optional<RecoveryTaskStatus> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        RecoveryTaskStatus newStatus = result.get();
        String blockedReason = null;
        if (newStatus == RecoveryTaskStatus.BLOCKED) {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setHeaderText("Reason for blocking");
            Optional<String> reason = reasonDialog.showAndWait();
            if (reason.isEmpty() || reason.get().trim().isEmpty()) {
                statusLabel.setText("A reason is required to block a task.");
                return;
            }
            blockedReason = reason.get();
        }

        try {
            Request request = new Request(OperationType.UPDATE_RECOVERY_TASK_STATUS,
                    ClientSession.instance().getSessionToken());
            request.with("taskCode", selected.getTaskCode())
                    .with("newStatus", newStatus.name());
            if (blockedReason != null) {
                request.with("blockedReason", blockedReason);
            }
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Task " + selected.getTaskCode()
                        + " updated to " + newStatus.displayName());
                loadTasks();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/incident-details-view.fxml");
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

    /*   * Sign the current user out, invalidating their server-side
     * session and returning to the login screen. Best-effort: still
     * signs out locally even if the server is unreachable.
     */
    @FXML
    public void onSignOut() {
        try {
            Request request = new Request(OperationType.LOGOUT,
                    ClientSession.instance().getSessionToken());
            ClientSession.instance().getDrsClient().send(request);
        } catch (Exception ignored) {
            // Best effort - sign out even if server is unreachable
        }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }

}
