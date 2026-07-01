package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Admin: add a new location.
  
 */
public class AdminAddLocationController {

    @FXML private TextField suburbField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private TextField postcodeField;
    @FXML private ComboBox<String> riskZoneCombo;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private javafx.scene.control.Button addLocationButton;

    @FXML
    public void initialize() {
        populateUserHeader();
        // Australian state codes
        stateCombo.setItems(FXCollections.observableArrayList(
                "NSW", "VIC", "QLD", "WA", "SA", "TAS", "ACT", "NT"));
        riskZoneCombo.setItems(FXCollections.observableArrayList(
                "LOW", "MEDIUM", "HIGH"));
    }

    @FXML
    public void onAdd() {
        String suburb = suburbField.getText();
        if (suburb == null || suburb.trim().length() < 2) {
            statusLabel.setText("Suburb is required.");
            return;
        }
        String state = stateCombo.getValue();
        if (state == null) {
            statusLabel.setText("Choose a state.");
            return;
        }
        String postcode = postcodeField.getText();
        if (postcode == null || postcode.trim().length() < 3) {
            statusLabel.setText("Postcode is required.");
            return;
        }
        double lat, lon;
        try {
            lat = Double.parseDouble(latitudeField.getText().trim());
            lon = Double.parseDouble(longitudeField.getText().trim());
        } catch (Exception e) {
            statusLabel.setText("Latitude and longitude must be numbers.");
            return;
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            statusLabel.setText("Coordinates are out of valid range.");
            return;
        }

        if (addLocationButton != null) addLocationButton.setDisable(true);
        try {
            Request request = new Request(OperationType.ADD_LOCATION,
                    ClientSession.instance().getSessionToken());
            request.with("suburb", suburb.trim());
            request.with("state", state);
            request.with("postcode", postcode.trim());
            String riskZone = riskZoneCombo.getValue();
            if (riskZone != null) {
                request.with("riskZone", riskZone);
            }
            request.with("latitude", lat);
            request.with("longitude", lon);
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Location added.");
                SceneNavigator.showView("/fxml/admin-panel-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
                if (addLocationButton != null) addLocationButton.setDisable(false);
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
            if (addLocationButton != null) addLocationButton.setDisable(false);
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/admin-panel-view.fxml");
    }

    private void populateUserHeader() {
        User u = ClientSession.instance().getCurrentUser();
        if (u == null) return;
        if (userNameLabel != null) userNameLabel.setText(u.getFullName());
        if (userRoleLabel != null) {
            userRoleLabel.setText(u.getRole() == null
                    ? "" : u.getRole().name());
        }
    }

    @FXML
    public void onSignOut() {
        try {
            Request request = new Request(OperationType.LOGOUT,
                    ClientSession.instance().getSessionToken());
            ClientSession.instance().getDrsClient().send(request);
        } catch (Exception ignored) { }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }
}
