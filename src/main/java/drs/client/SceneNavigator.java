package drs.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Owns the primary Stage and switches scenes between FXML views.
 * Controllers call {@link #showView(String)} to navigate.
  
 */
public final class SceneNavigator {

    private static final Logger LOG = LoggerFactory.getLogger(SceneNavigator.class);

    private static Stage primaryStage;

    private SceneNavigator() {
        // Static utility
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /** Default initial window size (used on first scene load). */
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;

    /*   * Replace the current scene with the FXML at the given classpath
     * path. Applies the global stylesheet. Preserves the current window
     * size across navigation so that the stage does not shrink to fit
     * each view's preferred size.
         * @param fxmlPath classpath resource path, e.g. "/fxml/login-view.fxml"
     */
    public static void showView(String fxmlPath) {
        if (primaryStage == null) {
            throw new IllegalStateException("SceneNavigator not initialised");
        }
        try {
            URL resource = SceneNavigator.class.getResource(fxmlPath);
            if (resource == null) {
                showError("View not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // Capture current size before swap (or fall back to defaults)
            double width = (primaryStage.getScene() != null
                    && primaryStage.getWidth() > 0)
                    ? primaryStage.getWidth() : DEFAULT_WIDTH;
            double height = (primaryStage.getScene() != null
                    && primaryStage.getHeight() > 0)
                    ? primaryStage.getHeight() : DEFAULT_HEIGHT;

            Scene scene = new Scene(root, width, height);
            URL css = SceneNavigator.class.getResource("/css/drs-theme.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            primaryStage.setScene(scene);

            // Restore size explicitly - setScene() can trigger an
            // auto-fit-to-content otherwise.
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
        } catch (Exception e) {
            LOG.error("Failed to load view " + fxmlPath, e);
            showError("Failed to load: " + e.getMessage());
        }
    }

    /*   * Show a modal error dialog with the message.
         * @param message non-null message
     */
    public static void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.setHeaderText("Something went wrong");
        alert.showAndWait();
    }

    /*   * Show a non-blocking info dialog with the message.
         * @param message non-null message
     */
    public static void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /*   * Ask the user to confirm an action.
         * @param question short question
     * @return true if the user clicked OK
     */
    public static boolean confirm(String question) {
        Alert alert = new Alert(AlertType.CONFIRMATION, question);
        alert.setHeaderText("Please confirm");
        return alert.showAndWait()
                .filter(b -> b == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
