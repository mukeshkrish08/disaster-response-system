package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the citizen self-registration screen.
 *
 * Performs basic client-side validation for fast user feedback, then
 * sends a REGISTER_CITIZEN request to the server. The server is the
 * authoritative validator: it re-checks every field, hashes the
 * password with BCrypt, hardcodes the role to CITIZEN (so a malicious
 * client cannot register as a privileged user), enforces email
 * uniqueness, and writes an audit log entry.
  
 */
public class SignupController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Button createAccountButton;

    /*   * Handle the "Create account" button click. Validates input, sends
     * the REGISTER_CITIZEN request, and navigates back to login on
     * success so the new citizen can sign in immediately.
     */
    @FXML
    public void onCreateAccount() {
        errorLabel.setText("");

        String fullName = safe(fullNameField.getText());
        String email = safe(emailField.getText()).toLowerCase();
        String password = safe(passwordField.getText());
        String confirm = safe(confirmPasswordField.getText());

        // ----- Client-side checks (server re-validates) -------------
        if (fullName.length() < 2) {
            errorLabel.setText("Please enter your full name.");
            return;
        }
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            errorLabel.setText("Please enter a valid email address.");
            return;
        }
        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        // ----- Send to server ---------------------------------------
        try {
            Request request = new Request(OperationType.REGISTER_CITIZEN);
            request.with("fullName", fullName)
                    .with("email", email)
                    .with("password", password);
            Response response = ClientSession.instance().getDrsClient()
                    .send(request);

            if (!response.isSuccess()) {
                errorLabel.setText(response.getErrorMessage());
                return;
            }
            // Success: hand back to login, the user can sign in now.
            SceneNavigator.showInfo("Account created successfully.\n"
                    + "Please sign in with your new email and password.");
            SceneNavigator.showView("/fxml/login-view.fxml");
        } catch (ServerOfflineException e) {
            errorLabel.setText("Server is offline. Please try again.");
        }
    }

    /*   * Cancel signup and return to the login screen.
     */
    @FXML
    public void onBackToLogin() {
        SceneNavigator.showView("/fxml/login-view.fxml");
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
