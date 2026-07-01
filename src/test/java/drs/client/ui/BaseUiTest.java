package drs.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;
import java.net.URL;

/**
 * Base class for TestFX UI tests in Disaster Response System.
 *
 * Loads a single FXML view onto a fresh Stage. Subclasses override
 * {@link #fxmlPath()} to choose which screen they want to verify.
 *
 * The tests run in headless mode under Monocle (configured via
 * Surefire system properties in pom.xml) so they can be executed on
 * the marker's machine, on a CI server, or in any environment that
 * does not have a physical display attached.
 *
 * These tests deliberately exercise UI behaviour ONLY: form
 * validation, error messages, and navigation. They do NOT send
 * requests to a server, so they remain fast, deterministic, and free
 * of network/database dependencies.
  
 */
public abstract class BaseUiTest extends ApplicationTest {

    /*   * @return the classpath-relative FXML to load for this test,
     *        e.g. "/fxml/login-view.fxml".
     */
    protected abstract String fxmlPath();

    /*   * Apply the project stylesheet so style classes referenced by
     * controllers (.field-error, .net-online, etc.) resolve.
     */
    private void applyStylesheet(Scene scene) {
        URL css = getClass().getResource("/css/drs-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Wire SceneNavigator to this test's Stage so any controller
        // that calls SceneNavigator.showView(...) does not blow up
        // with IllegalStateException. This is what the real
        // DrsClientApplication does on startup.
        drs.client.SceneNavigator.init(stage);

        URL fxml = getClass().getResource(fxmlPath());
        if (fxml == null) {
            throw new IllegalStateException(
                    "FXML not found on classpath: " + fxmlPath());
        }
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600);
        applyStylesheet(scene);
        stage.setScene(scene);
        stage.show();
        stage.toFront();
    }
}
