package drs.client.ui;

import javafx.scene.control.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * UI tests for the citizen self-registration screen's client-side
 * form validation.
 *
 * Each test fills the form with deliberately bad input that should
 * be rejected by the controller before any server round-trip is
 * attempted, then inspects the error label to confirm the expected
 * message.
  
 */
class SignupFormValidationTest extends BaseUiTest {

    @Override
    protected String fxmlPath() {
        return "/fxml/signup-view.fxml";
    }

    @Test
    @DisplayName("Empty full name shows 'Please enter your full name.'")
    void emptyFullNameShowsError() {
        clickOn("#createAccountButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertNotNull(errorLabel);
        assertEquals("Please enter your full name.", errorLabel.getText());
    }

    @Test
    @DisplayName("Invalid email shows 'Please enter a valid email address.'")
    void invalidEmailShowsError() {
        clickOn("#fullNameField");
        write("Alex Murphy");
        clickOn("#emailField");
        write("not-an-email");  // no @ sign
        clickOn("#createAccountButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Please enter a valid email address.",
                errorLabel.getText());
    }

    @Test
    @DisplayName("Short password shows 'Password must be at least 8 characters.'")
    void shortPasswordShowsError() {
        clickOn("#fullNameField");
        write("Alex Murphy");
        clickOn("#emailField");
        write("alex@example.com");
        clickOn("#passwordField");
        write("short");  // < 8 chars
        clickOn("#createAccountButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Password must be at least 8 characters.",
                errorLabel.getText());
    }

    @Test
    @DisplayName("Mismatched passwords show 'Passwords do not match.'")
    void mismatchedPasswordsShowError() {
        clickOn("#fullNameField");
        write("Alex Murphy");
        clickOn("#emailField");
        write("alex@example.com");
        clickOn("#passwordField");
        write("Strong@Pass1");
        clickOn("#confirmPasswordField");
        write("Different@1");
        clickOn("#createAccountButton");

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Passwords do not match.", errorLabel.getText());
    }
}
