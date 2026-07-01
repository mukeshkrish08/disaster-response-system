package drs.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;

/**
 * Centralised helpers for the bottom-right connection indicator dot.
 *
 * Every authenticated screen has a {@code connectionStatusLabel}
 * styled with one of three CSS classes:
 *
 * <ul>
 *  <li>{@code net-online} - server reachable;</li>
 *  <li>{@code net-offline} - server not reachable;</li>
 *  <li>{@code net-reconnecting} - retry in flight.</li>
 * </ul>
 *
 * Updates marshal to the JavaFX application thread so background
 * threads can call these methods safely.
  
 */
public final class ConnectionIndicator {

    private ConnectionIndicator() {
        // Static helpers only.
    }

    /** Show the indicator as green/connected. */
    public static void online(Label label) {
        set(label, "● Connected", "net-online",
                "net-offline", "net-reconnecting");
    }

    /** Show the indicator as red/offline. */
    public static void offline(Label label) {
        set(label, "● Offline", "net-offline",
                "net-online", "net-reconnecting");
    }

    /*   * Show the indicator as amber/reconnecting, optionally with an
     * attempt count like "attempt 2/5".
     */
    public static void reconnecting(Label label, int attempt, int max) {
        String text = max > 0
                ? "● Reconnecting (" + attempt + "/" + max + ")"
                : "● Reconnecting…";
        set(label, text, "net-reconnecting",
                "net-online", "net-offline");
    }

    private static void set(Label label, String text,
                            String addClass, String... removeClasses) {
        if (label == null) {
            return;
        }
        Platform.runLater(() -> {
            label.setText(text);
            label.getStyleClass().removeAll(removeClasses);
            if (!label.getStyleClass().contains(addClass)) {
                label.getStyleClass().add(addClass);
            }
        });
    }
}
