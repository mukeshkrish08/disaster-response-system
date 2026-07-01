package drs.client.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for navigation between the login and signup screens.
 *
 * The "Create account" button on the login screen should open the
 * citizen signup view; the "Back to sign in" button on the signup
 * screen should return to the login view. Both navigations go through
 * {@link drs.client.SceneNavigator}, which is initialised in
 * {@link BaseUiTest#start}.
  
 */
class NavigationTest extends BaseUiTest {

    @Override
    protected String fxmlPath() {
        return "/fxml/login-view.fxml";
    }

    @Test
    @DisplayName("Clicking 'Create account' on login opens the signup screen")
    void createAccountOpensSignupScreen() {
        // Verify we start on login - signInButton is unique to login.
        assertTrue(lookup("#signInButton").tryQuery().isPresent(),
                "Should start on login screen");

        // Click the Create account button (the link button on login,
        // not the Sign in primary button).
        clickOn("Create account");

        // Now we should be on signup - the fullName field is unique
        // to signup and should be present.
        boolean fullNameFieldPresent = lookup("#fullNameField")
                .tryQuery().isPresent();
        assertTrue(fullNameFieldPresent,
                "Signup screen should be visible after click");
    }

    @Test
    @DisplayName("Round trip: login → signup → back to login")
    void signupBackToLoginNavigation() {
        // Login → signup
        clickOn("Create account");
        assertTrue(lookup("#fullNameField").tryQuery().isPresent(),
                "Should be on signup screen after first navigation");

        // Signup → back to login
        clickOn("Back to sign in");

        // The signup-specific fullName field should be gone, and
        // the login signInButton should be present again.
        boolean fullNameGone = !lookup("#fullNameField")
                .tryQuery().isPresent();
        boolean signInButtonBack = lookup("#signInButton")
                .tryQuery().isPresent();
        assertTrue(fullNameGone,
                "Signup fullName field should not be visible");
        assertTrue(signInButtonBack,
                "Login signInButton should be visible after returning");
    }
}
