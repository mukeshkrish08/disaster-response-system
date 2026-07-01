package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.Department;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Admin: add a new response team.
 *
 * A team must be linked to a department and has home-base
 * coordinates that drive distance-based dispatch suggestions.
  
 */
public class AdminAddTeamController {

    @FXML private TextField teamNameField;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private javafx.scene.control.Button addTeamButton;

    @FXML
    public void initialize() {
        populateUserHeader();
        loadDepartments();
    }

    @SuppressWarnings("unchecked")
    private void loadDepartments() {
        try {
            Request request = new Request(OperationType.LIST_DEPARTMENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                List<Department> depts = response.dataAs();
                departmentCombo.setItems(
                        FXCollections.observableArrayList(depts));
            } else {
                statusLabel.setText("Could not load departments: "
                        + response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onAdd() {
        String teamName = teamNameField.getText();
        if (teamName == null || teamName.trim().length() < 2) {
            statusLabel.setText("Team name is required.");
            return;
        }
        Department dept = departmentCombo.getValue();
        if (dept == null) {
            statusLabel.setText("Choose a department.");
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

        if (addTeamButton != null) addTeamButton.setDisable(true);
        try {
            Request request = new Request(OperationType.ADD_TEAM,
                    ClientSession.instance().getSessionToken());
            request.with("teamName", teamName.trim());
            request.with("departmentCode", dept.getDepartmentCode());
            request.with("latitude", lat);
            request.with("longitude", lon);
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Team added.");
                SceneNavigator.showView("/fxml/admin-panel-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
                if (addTeamButton != null) addTeamButton.setDisable(false);
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
            if (addTeamButton != null) addTeamButton.setDisable(false);
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
