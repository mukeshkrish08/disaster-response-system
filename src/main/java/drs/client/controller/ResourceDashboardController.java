package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.ui.StatusFeedback;
import drs.shared.model.User;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.ResourceType;
import drs.shared.enums.UserRole;
import drs.shared.model.Resource;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Arrays;
import java.util.List;

/**
 * Resource Dashboard view (Feature 1).
 *
 * Lists all resources with filterable type. Coordinators select a row
 * and click an action to allocate, view its allocations, return an
 * existing allocation, or (admin only) add a new resource.
  
 */
public class ResourceDashboardController {

    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, String> codeColumn;
    @FXML private TableColumn<Resource, String> nameColumn;
    @FXML private TableColumn<Resource, String> typeColumn;
    @FXML private TableColumn<Resource, Number> totalColumn;
    @FXML private TableColumn<Resource, Number> availableColumn;
    @FXML private TableColumn<Resource, String> statusColumn;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;

    @FXML
    public void initialize() {
        if (resourcesTable != null) {
            resourcesTable.setPlaceholder(new javafx.scene.control.Label("No resources in inventory. Click \"+ Add resource\" to seed."));
        }
        if (resourcesTable != null) {
            resourcesTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        populateUserHeader();
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("All");
        Arrays.stream(ResourceType.values()).forEach(t -> options.add(t.name()));
        typeFilterCombo.setItems(FXCollections.observableArrayList(options));
        typeFilterCombo.setValue("All");

        codeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getResourceCode()));
        nameColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getResourceName()));
        typeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getResourceType().displayName()));
        totalColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(
                        c.getValue().getQuantityTotal()));
        availableColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(
                        c.getValue().getQuantityAvailable()));
        statusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));

        onRefresh();
    }

    @FXML
    public void onRefresh() {
        try {
            Request request = new Request(OperationType.LIST_RESOURCES,
                    ClientSession.instance().getSessionToken());
            String filter = typeFilterCombo.getValue();
            if (filter != null && !"All".equals(filter)) {
                request.with("resourceType", filter);
            }
            Response response = ClientSession.instance().getDrsClient().send(request);
            markOnline();
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Resource> list = response.dataAs();
            resourcesTable.setItems(FXCollections.observableArrayList(list));
            statusLabel.setText(list.size() + " resource"
                    + (list.size() == 1 ? "" : "s") + " loaded.");
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline. Try again.");
        }
    }

    @FXML
    public void onAllocate() {
        Resource selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource first.");
            return;
        }
        if (selected.getQuantityAvailable() <= 0) {
            statusLabel.setText("Nothing available to allocate.");
            return;
        }
        SelectedIncidentHolder.selectedResourceCode = selected.getResourceCode();
        SceneNavigator.showView("/fxml/allocate-resource-view.fxml");
    }

    /*   * Open the Add Resource dialog. Admin only - server enforces this
     * even if the button is visible to others.
     */
    @FXML
    public void onAddResource() {
        UserRole role = ClientSession.instance().getCurrentUser().getRole();
        if (role != UserRole.ADMIN && role != UserRole.COORDINATOR) {
            statusLabel.setText("Only coordinators and administrators "
                    + "can add new resources.");
            return;
        }
        SceneNavigator.showView("/fxml/add-resource-view.fxml");
    }

    /*   * Navigate to the allocations view for the selected resource's
     * latest incident. The list of allocations across all incidents
     * is shown in the allocation list screen.
     */
    @FXML
    public void onViewAllocations() {
        Resource selected = resourcesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource first.");
            return;
        }
        SelectedIncidentHolder.selectedResourceCode = selected.getResourceCode();
        SceneNavigator.showView("/fxml/view-allocations-view.fxml");
    }

    /*   * Return an allocation back to the resource pool.
     */
    @FXML
    public void onReturnAllocation() {
        SceneNavigator.showView("/fxml/return-allocation-view.fxml");
    }

    @FXML
    public void onBack() {
        UserRole role = ClientSession.instance().getCurrentUser().getRole();
        if (role == UserRole.ADMIN) {
            SceneNavigator.showView("/fxml/admin-panel-view.fxml");
        } else {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
        }
    }

    // -----------------------------------------------------------------
    // Resource lifecycle actions: send to maintenance, return from
    // maintenance, retire. Each requires a selected row, confirms the
    // action, then sends the matching operation. Server-side
    // ResourceService validates status transitions and allocation
    // state, so the client trusts the server's error message.
    // -----------------------------------------------------------------

    @FXML
    public void onSendToMaintenance() {
        Resource selected = resourcesTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource first.");
            return;
        }
        if (!SceneNavigator.confirm(
                "Send " + selected.getResourceName() + " to maintenance?")) {
            return;
        }
        sendLifecycleOp(OperationType.SEND_RESOURCE_TO_MAINTENANCE,
                selected.getResourceCode(),
                "Resource sent to maintenance.");
    }

    @FXML
    public void onReturnFromMaintenance() {
        Resource selected = resourcesTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource first.");
            return;
        }
        if (!SceneNavigator.confirm(
                "Return " + selected.getResourceName() + " from maintenance?")) {
            return;
        }
        sendLifecycleOp(OperationType.RETURN_RESOURCE_FROM_MAINTENANCE,
                selected.getResourceCode(),
                "Resource is back in service.");
    }

    @FXML
    public void onRetireResource() {
        Resource selected = resourcesTable.getSelectionModel()
                .getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a resource first.");
            return;
        }
        if (!SceneNavigator.confirm("Retire " + selected.getResourceName()
                + " permanently? This cannot be undone.")) {
            return;
        }
        sendLifecycleOp(OperationType.RETIRE_RESOURCE,
                selected.getResourceCode(),
                "Resource retired.");
    }

    /*   * Common dispatch for the three single-field lifecycle operations
     * (resourceCode only). On success, the resource table is
     * reloaded so the row reflects its new state.
     */
    private void sendLifecycleOp(OperationType op, String resourceCode,
                                 String successMessage) {
        try {
            Request request = new Request(op,
                    ClientSession.instance().getSessionToken());
            request.with("resourceCode", resourceCode);
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                // Auto-clear after the linger so the status bar
                // doesn't accumulate stale messages from multiple
                // lifecycle actions in a row.
                StatusFeedback.show(statusLabel, successMessage);
                onRefresh();
            } else {
                StatusFeedback.sticky(statusLabel,
                        response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            StatusFeedback.sticky(statusLabel, "● Server offline.");
        }
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
