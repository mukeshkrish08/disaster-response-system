package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.model.ResponseTeam;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Map;

/**
 * Lets a coordinator confirm geo-dispatch suggestions for a primary
 * and (optional) secondary team and submit the assignment.
  
 */
public class AssignTeamController {

    @FXML private Label pageTitle;
    @FXML private Label incidentSummary;
    @FXML private Label suggestedPrimaryName;
    @FXML private Label primaryDistanceLabel;
    @FXML private Label suggestedSecondaryName;
    @FXML private Label secondaryDistanceLabel;
    @FXML private ComboBox<ResponseTeam> overrideTeamCombo;
    @FXML private Label overrideHintLabel;
    @FXML private Label finalAssignmentLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    private String incidentCode;
    private ResponseTeam primaryTeam;
    private double primaryDistance;
    private ResponseTeam secondaryTeam;
    private double secondaryDistance;
    private boolean primaryConfirmed;
    private boolean secondarySelected;

    @FXML
    public void initialize() {
        populateUserHeader();
        incidentCode = SelectedIncidentHolder.selectedIncidentCode;
        if (incidentCode == null) {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            return;
        }
        pageTitle.setText("Assign team for " + incidentCode);
        loadSuggestions();
        loadAllTeamsForOverride();
    }

    /*   * Fetch every AVAILABLE response team and put them in the
     * override ComboBox. The user can leave the selection empty
     * (meaning "use suggested primary"), or pick a different team
     * which will be used as the primary on assignment.
     */
    private void loadAllTeamsForOverride() {
        if (overrideTeamCombo == null) {
            return;
        }
        // Render each team as: "<code> - <name>  ·  Led by <leader>"
        overrideTeamCombo.setConverter(new StringConverter<ResponseTeam>() {
            @Override
            public String toString(ResponseTeam t) {
                if (t == null) {
                    return "";
                }
                String leader = t.getLeaderName();
                String suffix = leader != null && !leader.isEmpty()
                        ? "  ·  Led by " + leader
                        : "";
                return t.getTeamCode() + " - " + t.getTeamName() + suffix;
            }
            @Override
            public ResponseTeam fromString(String s) {
                return null;     // not used (dropdown is selection-only)
            }
        });
        try {
            Request request = new Request(OperationType.LIST_TEAMS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                if (overrideHintLabel != null) {
                    overrideHintLabel.setText(
                            "Could not load teams: " + response.getErrorMessage());
                }
                return;
            }
            List<ResponseTeam> all = response.dataAs();
            // Show only AVAILABLE teams (BUSY/OUT_OF_SERVICE are not
            // dispatchable). If the team has no availability set we
            // include it defensively to match server behaviour.
            List<ResponseTeam> available = all.stream()
                    .filter(t -> t.isActive() && (t.getAvailability() == null
                            || "AVAILABLE".equals(t.getAvailability().name())))
                    .toList();
            overrideTeamCombo.setItems(
                    FXCollections.observableArrayList(available));
            if (overrideHintLabel != null) {
                overrideHintLabel.setText(available.size()
                        + " AVAILABLE team(s) to choose from.");
            }
            // Live-preview: as the coordinator changes the override
            // dropdown, the Final-assignment panel updates immediately
            // so they always see the team that will be sent on Assign.
            overrideTeamCombo.valueProperty().addListener(
                    (obs, oldV, newV) -> updateFinalAssignmentPreview());
        } catch (ServerOfflineException e) {
            if (overrideHintLabel != null) {
                overrideHintLabel.setText("● Server offline.");
            }
        }
    }

    private void loadSuggestions() {
        try {
            Request request = new Request(OperationType.SUGGEST_TEAMS,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            Map<String, Object> payload = response.dataAs();
            primaryTeam = (ResponseTeam) payload.get("primaryTeam");
            Number pd = (Number) payload.get("primaryDistanceKm");
            primaryDistance = pd == null ? 0.0 : pd.doubleValue();
            if (primaryTeam != null) {
                String leaderSuffix = primaryTeam.getLeaderName() != null
                        && !primaryTeam.getLeaderName().isEmpty()
                        ? "  ·  Led by " + primaryTeam.getLeaderName()
                        : "";
                suggestedPrimaryName.setText(primaryTeam.getTeamCode() + " - "
                        + primaryTeam.getTeamName() + leaderSuffix);
                primaryDistanceLabel.setText(String.format("%.2f km away",
                        primaryDistance));
            }
            secondaryTeam = (ResponseTeam) payload.get("secondaryTeam");
            Number sd = (Number) payload.get("secondaryDistanceKm");
            secondaryDistance = sd == null ? 0.0 : sd.doubleValue();
            if (secondaryTeam != null) {
                String leaderSuffix = secondaryTeam.getLeaderName() != null
                        && !secondaryTeam.getLeaderName().isEmpty()
                        ? "  ·  Led by " + secondaryTeam.getLeaderName()
                        : "";
                suggestedSecondaryName.setText(secondaryTeam.getTeamCode() + " - "
                        + secondaryTeam.getTeamName() + leaderSuffix);
                secondaryDistanceLabel.setText(String.format("%.2f km away",
                        secondaryDistance));
            } else {
                suggestedSecondaryName.setText("(no secondary suggestion)");
                secondaryDistanceLabel.setText("");
            }
            // Seed the preview panel with the initial state once
            // suggestions arrive so the coordinator sees the default
            // assignment immediately, before clicking anything.
            updateFinalAssignmentPreview();
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onConfirmPrimary() {
        primaryConfirmed = true;
        statusLabel.setText("Primary confirmed. Now confirm or skip secondary.");
        updateFinalAssignmentPreview();
    }

    @FXML
    public void onConfirmSecondary() {
        secondarySelected = true;
        statusLabel.setText("Secondary confirmed. Click Assign to finalise.");
        updateFinalAssignmentPreview();
    }

    @FXML
    public void onSkipSecondary() {
        secondarySelected = false;
        statusLabel.setText("Will not assign a secondary. Click Assign to finalise.");
        updateFinalAssignmentPreview();
    }

    /*   * Refresh the "Final assignment preview" panel. This is the source
     * of truth the coordinator looks at to know which teams are about
     * to be sent: it shows the suggested primary (or its manual
     * override) and the suggested secondary if confirmed. Called from
     * onConfirmPrimary, onConfirmSecondary, onSkipSecondary, after
     * suggestions load, and whenever the override dropdown value
     * changes.
     */
    private void updateFinalAssignmentPreview() {
        if (finalAssignmentLabel == null) {
            return;
        }
        ResponseTeam override = overrideTeamCombo == null
                ? null : overrideTeamCombo.getValue();
        ResponseTeam effectivePrimary = override != null
                ? override : (primaryConfirmed ? primaryTeam : null);
        String primaryText;
        if (effectivePrimary == null) {
            primaryText = "Primary team: (confirm a suggestion or pick an override)";
        } else {
            primaryText = "Primary team: " + effectivePrimary.getTeamCode()
                    + " - " + effectivePrimary.getTeamName()
                    + (override != null ? "  (manual override)" : "");
        }
        String secondaryText;
        if (secondarySelected && secondaryTeam != null
                && effectivePrimary != null
                && !secondaryTeam.getTeamCode().equals(
                        effectivePrimary.getTeamCode())) {
            secondaryText = "Secondary team: " + secondaryTeam.getTeamCode()
                    + " - " + secondaryTeam.getTeamName();
        } else {
            secondaryText = "Secondary team: (none - skipped or duplicates primary)";
        }
        finalAssignmentLabel.setText(primaryText + "\n" + secondaryText);
    }

    @FXML
    public void onAssign() {
        // Determine the primary team. The coordinator's manual
        // override (if any) wins over the Haversine-suggested team.
        // This lets them route around the closest-team default when
        // they have local knowledge the algorithm doesn't (fuel,
        // crew rotation, etc.). Server validates the team_code
        // independently so this is safe.
        ResponseTeam override = overrideTeamCombo == null
                ? null : overrideTeamCombo.getValue();
        ResponseTeam effectivePrimary;
        double effectiveDistance;
        boolean usedOverride;
        if (override != null) {
            effectivePrimary = override;
            // Distance is unknown for an override team - pass 0.0 and
            // let the server log it. The Haversine-ranked suggestion
            // is no longer relevant once the coordinator overrides.
            effectiveDistance = 0.0;
            usedOverride = true;
        } else {
            if (!primaryConfirmed || primaryTeam == null) {
                statusLabel.setText("Confirm the suggested primary "
                        + "team, or pick a different team from the "
                        + "override dropdown.");
                return;
            }
            effectivePrimary = primaryTeam;
            effectiveDistance = primaryDistance;
            usedOverride = false;
        }

        try {
            Request request = new Request(OperationType.ASSIGN_TEAMS,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode)
                    .with("primaryTeamCode", effectivePrimary.getTeamCode())
                    .with("primaryDistanceKm", effectiveDistance);
            if (secondarySelected && secondaryTeam != null
                    && !secondaryTeam.getTeamCode().equals(
                            effectivePrimary.getTeamCode())) {
                request.with("secondaryTeamCode", secondaryTeam.getTeamCode())
                        .with("secondaryDistanceKm", secondaryDistance);
            }
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                String msg = "Teams assigned to " + incidentCode
                        + (usedOverride ? " (manual override: "
                                + effectivePrimary.getTeamName() + ")" : "");
                SceneNavigator.showInfo(msg);
                SceneNavigator.showView("/fxml/incident-details-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onCancel() {
        SceneNavigator.showView("/fxml/incident-details-view.fxml");
    }

    /*   * Fill the header right-side labels with the currently logged-in
     * user's name and role.
     */
    private void populateUserHeader() {
        User u = ClientSession.instance().getCurrentUser();
        if (u == null) {
            return;
        }
        if (userNameLabel != null) {
            userNameLabel.setText(u.getFullName());
        }
        if (userRoleLabel != null) {
            userRoleLabel.setText(u.getRole() == null
                    ? "" : u.getRole().name());
        }
    }

    /*   * Sign the current user out, invalidating their server-side
     * session and returning to the login screen. Best-effort: still
     * signs out locally even if the server is unreachable.
     */
    @FXML
    public void onSignOut() {
        try {
            Request request = new Request(OperationType.LOGOUT,
                    ClientSession.instance().getSessionToken());
            ClientSession.instance().getDrsClient().send(request);
        } catch (Exception ignored) {
            // Best effort - sign out even if server is unreachable
        }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }

}
