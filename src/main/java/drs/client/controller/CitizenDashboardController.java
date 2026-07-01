package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.Incident;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Citizen dashboard - list of own reports + button to file a new one.
  
 */
public class CitizenDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button withdrawButton;
    @FXML private TableView<Incident> myReportsTable;
    @FXML private TableColumn<Incident, String> codeColumn;
    @FXML private TableColumn<Incident, String> typeColumn;
    @FXML private TableColumn<Incident, String> reportedColumn;
    @FXML private TableColumn<Incident, String> statusColumn;

    @FXML
    public void initialize() {
        if (myReportsTable != null) {
            myReportsTable.setPlaceholder(new javafx.scene.control.Label("No reports yet. Click \"Report Incident\" to file your first one."));
        }
        if (myReportsTable != null) {
            myReportsTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        User user = ClientSession.instance().getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome back, "
                    + user.getFullName().split(" ")[0]);
            // Populate header user info from active session
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole() == null
                        ? "" : user.getRole().name());
            }
        }
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("incidentCode"));
        typeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getDisasterType().displayName()));
        statusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));
        reportedColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.relative(c.getValue().getReportedAt())));
        onRefresh();
    }

    @FXML
    public void onReportNew() {
        SceneNavigator.showView("/fxml/report-incident-view.fxml");
    }

    @FXML
    public void onRefresh() {
        try {
            Request request = new Request(OperationType.LIST_MY_REPORTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Incident> list = response.dataAs();
            myReportsTable.setItems(FXCollections.observableArrayList(list));
            statusLabel.setText(list.size() + " report"
                    + (list.size() == 1 ? "" : "s") + " loaded.");
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline. Try again.");
        }
    }

    /*   * Citizen withdraws their own report. Only valid while the
     * report is still in REPORTED state; server enforces both that
     * and the ownership check.
     */
    @FXML
    public void onWithdraw() {
        Incident selected = myReportsTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select one of your reports first.");
            return;
        }
        // Client-side guard: only REPORTED reports can be withdrawn.
        // Server re-validates via IncidentStateRegistry.
        if (selected.getStatus() != drs.shared.enums.IncidentStatus.REPORTED) {
            statusLabel.setText("Only reports still in REPORTED status "
                    + "can be withdrawn (this one is "
                    + selected.getStatus().displayName() + ").");
            return;
        }

        // Ask for a brief reason; min 10 chars enforced both here
        // and server-side.
        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Withdraw report");
        dialog.setHeaderText("Withdraw " + selected.getIncidentCode() + "?");
        dialog.setContentText("Reason (min. 10 characters):");
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;     // user cancelled
        }
        String reason = result.get().trim();
        if (reason.length() < 10) {
            statusLabel.setText(
                    "Reason must be at least 10 characters.");
            return;
        }

        try {
            Request request = new Request(OperationType.WITHDRAW_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", selected.getIncidentCode());
            request.with("reason", reason);
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Report withdrawn.");
                onRefresh();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline. Try again.");
        }
    }

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
