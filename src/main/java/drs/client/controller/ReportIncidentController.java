package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.DisasterType;
import drs.shared.model.Incident;
import drs.shared.model.User;
import drs.shared.model.Location;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;

/**
 * Report-incident form. Citizen submits a new disaster report.
  
 */
public class ReportIncidentController {

    @FXML private ComboBox<DisasterType> disasterTypeCombo;
    @FXML private ComboBox<Location> locationCombo;
    @FXML private Spinner<Integer> peopleAffectedSpinner;
    @FXML private ComboBox<String> propertyRiskCombo;
    @FXML private TextArea descriptionArea;
    @FXML private javafx.scene.control.TextField contactPhoneField;
    @FXML private Label charCountLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        populateUserHeader();
        disasterTypeCombo.setItems(FXCollections.observableArrayList(
                DisasterType.values()));
        propertyRiskCombo.setItems(FXCollections.observableArrayList(
                "Low", "Moderate", "High"));
        peopleAffectedSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));
        descriptionArea.textProperty().addListener((obs, oldText, newText) ->
                charCountLabel.setText(newText.length() + " / 1000 characters"));
        loadLocations();
    }

    private void loadLocations() {
        try {
            Request request = new Request(OperationType.LIST_LOCATIONS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<Location> list = response.dataAs();
                locationCombo.setItems(FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("Could not load locations. Try refreshing.");
        }
    }

    @FXML
    public void onSubmit() {
        DisasterType type = disasterTypeCombo.getValue();
        Location location = locationCombo.getValue();
        Integer people = peopleAffectedSpinner.getValue();
        String propertyRisk = propertyRiskCombo.getValue();
        String description = descriptionArea.getText() == null
                ? "" : descriptionArea.getText().trim();
        String contactPhone = contactPhoneField.getText() == null
                ? "" : contactPhoneField.getText().trim();

        if (type == null) {
            statusLabel.setText("Please choose a disaster type.");
            return;
        }
        if (location == null) {
            statusLabel.setText("Please choose a location.");
            return;
        }
        if (description.length() < 5) {
            statusLabel.setText("Please describe what you're seeing (5+ chars).");
            return;
        }
        if (!drs.shared.util.InputValidator.isValidAuPhone(contactPhone)) {
            statusLabel.setText("Contact phone is required and must be a "
                    + "valid Australian number (e.g. 0412 345 678 or "
                    + "02 9876 5432).");
            contactPhoneField.requestFocus();
            return;
        }

        try {
            Request request = new Request(OperationType.REPORT_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("disasterType", type.name())
                    .with("locationCode", location.getLocationCode())
                    .with("latitude", location.getLatitude())
                    .with("longitude", location.getLongitude())
                    .with("description", description)
                    .with("contactPhone", contactPhone)
                    .with("peopleAffected", people == null ? 0 : people)
                    .with("propertyRiskLevel", propertyRisk);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            Incident incident = response.dataAs();
            SceneNavigator.showInfo("Report " + incident.getIncidentCode()
                    + " submitted. Coordinators have been notified.");
            SceneNavigator.showView("/fxml/citizen-dashboard-view.fxml");
        } catch (ServerOfflineException e) {
            statusLabel.setText("Cannot reach the server. Please try again.");
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/citizen-dashboard-view.fxml");
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
