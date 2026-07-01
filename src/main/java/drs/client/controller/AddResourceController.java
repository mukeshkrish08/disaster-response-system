package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.shared.model.User;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.ResourceType;
import drs.shared.model.Location;
import drs.shared.model.Resource;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Add a new resource to inventory (admin only). Feature 1 completion.
  
 */
public class AddResourceController {

    @FXML private TextField nameField;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private ComboBox<ResourceType> typeCombo;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private ComboBox<Location> locationCombo;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        populateUserHeader();
        typeCombo.setItems(FXCollections.observableArrayList(
                ResourceType.values()));
        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 10000, 1));
        loadLocations();
    }

    private void loadLocations() {
        try {
            Request request = new Request(OperationType.LIST_LOCATIONS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient()
                    .send(request);
            if (response.isSuccess()) {
                List<Location> list = response.dataAs();
                locationCombo.setItems(FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("Could not load locations from server. "
                    + "Click Refresh to retry.");
        }
    }

    @FXML
    public void onAdd() {
        String name = nameField.getText() == null ? ""
                : nameField.getText().trim();
        ResourceType type = typeCombo.getValue();
        Integer qty = quantitySpinner.getValue();
        Location location = locationCombo.getValue();

        if (name.isEmpty()) {
            statusLabel.setText("Resource name is required.");
            return;
        }
        if (type == null) {
            statusLabel.setText("Choose a resource type.");
            return;
        }
        if (qty == null || qty <= 0) {
            statusLabel.setText("Quantity must be at least 1.");
            return;
        }

        try {
            Request request = new Request(OperationType.ADD_RESOURCE,
                    ClientSession.instance().getSessionToken());
            request.with("resourceName", name)
                    .with("resourceType", type.name())
                    .with("quantityTotal", qty);
            if (location != null) {
                request.with("homeLocationCode", location.getLocationCode());
            }
            Response response = ClientSession.instance().getDrsClient()
                    .send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            Resource saved = response.dataAs();
            SceneNavigator.showInfo("Resource " + saved.getResourceCode()
                    + " added.");
            SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
        } catch (ServerOfflineException e) {
            statusLabel.setText("Server offline. Try again.");
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
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
