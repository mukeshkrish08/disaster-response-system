package drs.client.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Centralised user-feedback helpers for the bottom status bar.
 *
 * JavaFX has no built-in toast/snackbar widget, so the DRS UI uses
 * a single {@code statusLabel} on each screen as the catch-all for
 * one-line feedback. To avoid messages sitting on screen forever
 * after the action they describe is long over, all writes route
 * through {@link #show} which auto-clears the label after a few
 * seconds.
 *
 * All calls are scheduled on the JavaFX application thread.
 * Calling from a background thread is therefore safe.
  
 */
public final class StatusFeedback {

    /** Default time a non-sticky message remains visible. */
    private static final Duration DEFAULT_LINGER = Duration.seconds(4);

    private StatusFeedback() {
        // Static helpers only.
    }

    /*   * Show a transient message and auto-clear after the default linger.
     */
    public static void show(Label statusLabel, String message) {
        show(statusLabel, message, DEFAULT_LINGER);
    }

    /*   * Show a transient message with a custom linger.
     */
    public static void show(Label statusLabel, String message,
                            Duration linger) {
        if (statusLabel == null) {
            return;
        }
        Platform.runLater(() -> {
            statusLabel.setText(message == null ? "" : message);
            PauseTransition delay = new PauseTransition(linger);
            delay.setOnFinished(e -> {
                // Only clear if we still own this message - if a later
                // call has replaced our text in the meantime, leave it.
                if (message != null && message.equals(statusLabel.getText())) {
                    statusLabel.setText("");
                }
            });
            delay.play();
        });
    }

    /*   * Show a message that does NOT auto-clear. Use for errors the
     * user must act on (e.g. validation prompts before submit).
     */
    public static void sticky(Label statusLabel, String message) {
        if (statusLabel == null) {
            return;
        }
        Platform.runLater(() ->
                statusLabel.setText(message == null ? "" : message));
    }

    /*   * Clear the status label immediately.
     */
    public static void clear(Label statusLabel) {
        if (statusLabel == null) {
            return;
        }
        Platform.runLater(() -> statusLabel.setText(""));
    }
}
