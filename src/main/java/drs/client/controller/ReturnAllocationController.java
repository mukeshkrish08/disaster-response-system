package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.shared.model.User;
import drs.client.net.ServerOfflineException;
import drs.shared.model.Incident;
import drs.shared.model.ResourceAllocation;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Return a resource allocation back to inventory. Feature 1 completion.
  
 */
public class ReturnAllocationController {

    @FXML private ComboBox<Incident> incidentCombo;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private TableView<ResourceAllocation> allocationsTable;
    @FXML private TableColumn<ResourceAllocation, String> codeColumn;
    @FXML private TableColumn<ResourceAllocation, String> resourceColumn;
    @FXML private TableColumn<ResourceAllocation, Number> quantityColumn;
    @FXML private TableColumn<ResourceAllocation, String> allocatedAtColumn;
    @FXML private TableColumn<ResourceAllocation, String> returnedColumn;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;

    @FXML
    public void initialize() {
        populateUserHeader();
        codeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getAllocationCode()));
        resourceColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getResourceCode() + " - "
                                + c.getValue().getResourceName()));
        quantityColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(
                        c.getValue().getQuantityAllocated()));
        allocatedAtColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.relative(c.getValue().getAllocatedAt())));
        returnedColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().isReturned() ? "Yes" : "No"));
        loadIncidents();
    }

    private void loadIncidents() {
        try {
            Request request = new Request(OperationType.LIST_INCIDENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            markOnline();
            if (response.isSuccess()) {
                List<Incident> list = response.dataAs();
                incidentCombo.setItems(FXCollections.observableArrayList(list));
                if (!list.isEmpty()) {
                    incidentCombo.setValue(list.get(0));
                    onIncidentChange();
                }
            }
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline.");
        }
    }

    @FXML
    public void onIncidentChange() {
        Incident selected = incidentCombo.getValue();
        if (selected == null) {
            return;
        }
        try {
            Request request = new Request(
                    OperationType.LIST_ALLOCATIONS_FOR_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", selected.getIncidentCode());
            Response response = ClientSession.instance().getDrsClient().send(request);
            markOnline();
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<ResourceAllocation> list = response.dataAs();
            allocationsTable.setItems(FXCollections.observableArrayList(list));
            statusLabel.setText(list.size() + " allocation"
                    + (list.size() == 1 ? "" : "s") + " for "
                    + selected.getIncidentCode());
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline.");
        }
    }

    @FXML
    public void onReturn() {
        ResourceAllocation selected = allocationsTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an allocation first.");
            return;
        }
        if (selected.isReturned()) {
            statusLabel.setText("This allocation has already been returned.");
            return;
        }
        if (!SceneNavigator.confirm("Return " + selected.getQuantityAllocated()
                + " unit(s) of " + selected.getResourceName()
                + " back to inventory?")) {
            return;
        }
        try {
            Request request = new Request(OperationType.RETURN_ALLOCATION,
                    ClientSession.instance().getSessionToken());
            request.with("allocationCode", selected.getAllocationCode());
            Response response = ClientSession.instance().getDrsClient().send(request);
            markOnline();
            if (response.isSuccess()) {
                statusLabel.setText("Allocation "
                        + selected.getAllocationCode() + " returned.");
                // Re-fetch allocations so the table shows the updated
                // "returned" state immediately, without waiting for
                // the user to navigate away and back.
                onRefresh();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline.");
        }
    }

    /*   * Manual refresh action. Re-fetches the allocations for the
     * currently selected incident. Wired to the "↻ Refresh" button
     * on the form so users can force a reload after returning an
     * allocation rather than waiting for navigation to pick it up.
     */
    @FXML
    public void onRefresh() {
        onIncidentChange();
    }

    @FXML
    public void onBack() {
        SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
    }

    private void markOnline() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("● Connected");
            connectionStatusLabel.getStyleClass().removeAll(
                    "net-offline", "net-reconnecting");
            if (!connectionStatusLabel.getStyleClass().contains("net-online")) {
                connectionStatusLabel.getStyleClass().add("net-online");
            }
        }
    }

    private void markOffline() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("● Offline");
            connectionStatusLabel.getStyleClass().removeAll(
                    "net-online", "net-reconnecting");
            if (!connectionStatusLabel.getStyleClass().contains("net-offline")) {
                connectionStatusLabel.getStyleClass().add("net-offline");
            }
        }
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
            drs.shared.protocol.Request request =
                new drs.shared.protocol.Request(
                    drs.shared.protocol.OperationType.LOGOUT,
                    ClientSession.instance().getSessionToken());
            ClientSession.instance().getDrsClient().send(request);
        } catch (Exception ignored) {
            // Best effort - sign out even if server is unreachable
        }
        ClientSession.instance().clearSession();
        drs.client.SceneNavigator.showView("/fxml/login-view.fxml");
    }

}
