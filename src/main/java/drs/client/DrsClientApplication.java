package drs.client;

import drs.client.net.ClientSession;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX entry point for the Disaster Response System client.
 *
 * Opens the login screen via {@link SceneNavigator}. Subsequent
 * navigation is driven by the controllers calling
 * {@link SceneNavigator#showView(String)}.
  
 */
public class DrsClientApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(DrsClientApplication.class);

    @Override
    public void start(Stage primaryStage) {
        LOG.info("Disaster Response System client starting...");
        SceneNavigator.init(primaryStage);
        primaryStage.setTitle("Disaster Response System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(720);
        // Set an explicit initial size so showView's size-restore has
        // sensible values from the very first frame.
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        SceneNavigator.showView("/fxml/login-view.fxml");
        primaryStage.show();
    }

    @Override
    public void stop() {
        LOG.info("Client shutting down");
        try {
            if (ClientSession.instance().isAuthenticated()) {
                ClientSession.instance().getDrsClient().disconnect();
            }
        } catch (Exception ignored) {
            // No-op
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
