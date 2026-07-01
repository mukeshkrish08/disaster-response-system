package drs.client.ui;

import javafx.scene.control.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the login screen's client-side form validation.
 *
 * These tests exercise the JavaFX controller's input checking only
 * - they never reach the network. The "Sign in" button is clicked
 * with deliberately bad input so the controller's pre-network
 * validation is what we observe.
  
 */
class LoginFormValidationTest extends BaseUiTest {

    @Override
    protected String fxmlPath() {
        return "/fxml/login-view.fxml";
    }

    @Test
    @DisplayName("Empty email shows 'Please enter your email address.'")
    void emptyEmailShowsError() {
        // Don't type anything; click Sign in directly.
        clickOn("#signInButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertNotNull(errorLabel, "errorLabel should be in the scene");
        assertEquals("Please enter your email address.",
                errorLabel.getText());
    }

    @Test
    @DisplayName("Email filled but empty password shows 'Please enter your password.'")
    void emptyPasswordShowsError() {
        clickOn("#emailField");
        write("citizen@drs.local");

        clickOn("#signInButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Please enter your password.", errorLabel.getText());
    }

    @Test
    @DisplayName("Validation message clears once both fields are filled and submit attempted")
    void errorLabelClearsAfterFillingFields() {
        // First trigger the empty-email error
        clickOn("#signInButton");
        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Please enter your email address.",
                errorLabel.getText());

        // Fill both fields and click again.
        // The controller will clear the validation error before
        // attempting the (nonexistent) server call. We assert the
        // previous validation message is no longer shown.
        clickOn("#emailField");
        write("citizen@drs.local");
        clickOn("#passwordField");
        write("Demo@123");
        clickOn("#signInButton");

        String text = errorLabel.getText();
        boolean validationMessageGone =
                !"Please enter your email address.".equals(text)
                && !"Please enter your password.".equals(text);
        assertTrue(validationMessageGone,
                "After valid input, validation messages should be cleared "
                + "(actual: '" + text + "')");
    }
}
