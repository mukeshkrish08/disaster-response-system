package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.UserRole;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Map;

/**
 * Login view controller. Validates fields client-side, then sends a
 * LOGIN request to the server. On success, stores the token and routes
 * to the role's home view.
  
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusLabel;
    @FXML private Button signInButton;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        connectionStatusLabel.setText("● Ready");
        connectionStatusLabel.getStyleClass().add("net-online");
    }

    @FXML
    public void onSignIn(ActionEvent event) {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email address.");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            return;
        }
        errorLabel.setText("");
        try {
            Request request = new Request(OperationType.LOGIN);
            request.with("email", email).with("password", password);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                errorLabel.setText(response.getErrorMessage());
                return;
            }
            Map<String, Object> payload = response.dataAs();
            String token = (String) payload.get("token");
            User user = (User) payload.get("user");
            ClientSession.instance().setSession(token, user);
            routeToHomeView(user.getRole());
        } catch (ServerOfflineException e) {
            connectionStatusLabel.setText("● Server offline");
            connectionStatusLabel.getStyleClass().clear();
            connectionStatusLabel.getStyleClass().add("net-offline");
            errorLabel.setText("Cannot reach the server. Please try again.");
        } catch (Exception e) {
            errorLabel.setText("Unexpected error: " + e.getMessage());
        }
    }

    private void routeToHomeView(UserRole role) {
        switch (role) {
            case CITIZEN:
                SceneNavigator.showView("/fxml/citizen-dashboard-view.fxml");
                break;
            case COORDINATOR:
                SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
                break;
            case TEAM_LEADER:
                SceneNavigator.showView("/fxml/team-leader-panel-view.fxml");
                break;
            case AGENCY_REP:
                SceneNavigator.showView("/fxml/agency-inbox-view.fxml");
                break;
            case ADMIN:
                SceneNavigator.showView("/fxml/admin-panel-view.fxml");
                break;
            default:
                errorLabel.setText("Unknown role: " + role);
        }
    }

    /*   * Navigate to the citizen self-registration screen. Staff accounts
     * (coordinator / team leader / agency rep / admin) cannot be
     * created from here - they are admin-provisioned.
     */
    @FXML
    public void onCreateAccount() {
        SceneNavigator.showView("/fxml/signup-view.fxml");
    }
}
