package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.DepartmentType;
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
 * Admin: add a new department.
  
 */
public class AdminAddDepartmentController {

    @FXML private TextField nameField;
    @FXML private ComboBox<DepartmentType> typeCombo;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private javafx.scene.control.Button addDepartmentButton;

    @FXML
    public void initialize() {
        populateUserHeader();
        typeCombo.setItems(FXCollections.observableArrayList(
                DepartmentType.values()));
    }

    @FXML
    public void onAdd() {
        String name = nameField.getText();
        if (name == null || name.trim().length() < 2) {
            statusLabel.setText("Department name is required.");
            return;
        }
        DepartmentType type = typeCombo.getValue();
        if (type == null) {
            statusLabel.setText("Choose a department type.");
            return;
        }

        if (addDepartmentButton != null) addDepartmentButton.setDisable(true);
        try {
            Request request = new Request(OperationType.ADD_DEPARTMENT,
                    ClientSession.instance().getSessionToken());
            request.with("name", name.trim());
            request.with("departmentType", type.name());
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Department added.");
                SceneNavigator.showView("/fxml/admin-panel-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
                if (addDepartmentButton != null) addDepartmentButton.setDisable(false);
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
            if (addDepartmentButton != null) addDepartmentButton.setDisable(false);
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
