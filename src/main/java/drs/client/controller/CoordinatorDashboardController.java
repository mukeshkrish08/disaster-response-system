package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.IncidentStatus;
import drs.shared.model.Incident;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Coordinator dashboard - priority-sorted incident list, strategy
 * switcher, status filter, and action buttons.
  
 */
public class CoordinatorDashboardController {

    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> strategyCombo;
    @FXML private TableView<Incident> incidentTable;
    @FXML private TableColumn<Incident, String> codeColumn;
    @FXML private TableColumn<Incident, String> typeColumn;
    @FXML private TableColumn<Incident, Number> scoreColumn;
    @FXML private TableColumn<Incident, String> statusColumn;
    @FXML private TableColumn<Incident, String> locationColumn;
    @FXML private TableColumn<Incident, String> reportedColumn;
    @FXML private TableColumn<Incident, String> assignedTeamColumn;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        if (incidentTable != null) {
            incidentTable.setPlaceholder(new javafx.scene.control.Label("No incidents in this view. Try changing the filter."));
        }
        if (incidentTable != null) {
            incidentTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        // Populate header user info from the active session
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
        filterCombo.setItems(FXCollections.observableArrayList(
                "All statuses", "Reported", "Assessed", "Assigned",
                "Responding", "Resolved", "Closed", "Rejected"));
        filterCombo.setValue("All statuses");
        // Disaster type filter - built from the DisasterType enum
        java.util.List<String> types = new java.util.ArrayList<>();
        types.add("All types");
        for (drs.shared.enums.DisasterType dt : drs.shared.enums.DisasterType.values()) {
            types.add(dt.displayName());
        }
        typeFilterCombo.setItems(FXCollections.observableArrayList(types));
        typeFilterCombo.setValue("All types");
        strategyCombo.setItems(FXCollections.observableArrayList(
                "CAP weighted", "Life-risk first"));
        strategyCombo.setValue("CAP weighted");

        codeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getIncidentCode()));
        typeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getDisasterType().displayName()));
        scoreColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(
                        c.getValue().getPriorityScore()));
        statusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));
        locationColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getLocationDisplayName()));
        reportedColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.relative(c.getValue().getReportedAt())));
        // Show "Team Name - Leader" when a team is assigned;
        // "Not yet assigned" when the incident is still pre-assignment.
        if (assignedTeamColumn != null) {
            assignedTeamColumn.setCellValueFactory(c -> {
                Incident inc = c.getValue();
                String team = inc.getAssignedTeamName();
                if (team == null || team.isEmpty()) {
                    return new javafx.beans.property.SimpleStringProperty(
                            "Not yet assigned");
                }
                String leader = inc.getAssignedLeaderName();
                String display = leader != null && !leader.isEmpty()
                        ? team + " - " + leader
                        : team;
                return new javafx.beans.property.SimpleStringProperty(display);
            });
        }

        onRefresh();
    }

    @FXML
    public void onRefresh() {
        try {
            Request request = new Request(OperationType.LIST_INCIDENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Incident> list = response.dataAs();
            // Status filter
            String statusFilter = filterCombo.getValue();
            if (statusFilter != null && !"All statuses".equals(statusFilter)) {
                list.removeIf(i -> !i.getStatus().displayName().equals(statusFilter));
            }
            // Disaster type filter
            String typeFilter = typeFilterCombo == null ? null : typeFilterCombo.getValue();
            if (typeFilter != null && !"All types".equals(typeFilter)) {
                list.removeIf(i -> !i.getDisasterType().displayName().equals(typeFilter));
            }
            incidentTable.setItems(FXCollections.observableArrayList(list));
            statusLabel.setText(list.size() + " incident"
                    + (list.size() == 1 ? "" : "s") + " loaded.");
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onStrategyChange() {
        String selected = strategyCombo.getValue();
        if (selected == null) {
            return;
        }
        try {
            String key = selected.equalsIgnoreCase("Life-risk first")
                    ? "LIFE_RISK" : "CAP";
            Request request = new Request(OperationType.SWITCH_PRIORITY_STRATEGY,
                    ClientSession.instance().getSessionToken());
            request.with("strategyName", key);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Sorted by " + selected + ".");
                onRefresh();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onOpenDetails() {
        Incident selected = incidentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Pick an incident first.");
            return;
        }
        ClientSession.instance().getDrsClient();  // ensure connected
        SelectedIncidentHolder.selectedIncidentCode = selected.getIncidentCode();
        SceneNavigator.showView("/fxml/incident-details-view.fxml");
    }

    @FXML
    public void onResources() {
        SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
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
