package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.CapCertainty;
import drs.shared.enums.CapSeverity;
import drs.shared.enums.CapUrgency;
import drs.shared.enums.UserRole;
import drs.shared.model.Incident;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;

/**
 * Incident details view. Shows full incident info, lets coordinator
 * assess / reject / close, and launches feature flows.
  
 */
public class IncidentDetailsController {

    @FXML private Label incidentTitle;
    @FXML private Label disasterTypeBadge;
    @FXML private Label priorityBadge;
    @FXML private Label statusBadge;
    @FXML private Label reporterLabel;
    @FXML private Label reportedAtLabel;
    @FXML private Label locationLabel;
    @FXML private Label peopleAffectedLabel;
    @FXML private Label propertyRiskLabel;
    @FXML private TextArea descriptionDisplay;
    @FXML private ComboBox<CapSeverity> severityCombo;
    @FXML private ComboBox<CapUrgency> urgencyCombo;
    @FXML private ComboBox<CapCertainty> certaintyCombo;
    @FXML private Button assessButton;
    @FXML private Button rejectButton;
    @FXML private Button assignTeamButton;
    @FXML private Button allocateResourceButton;
    @FXML private Button damageAssessmentButton;
    @FXML private Button recoveryTasksButton;
    @FXML private Button closeButton;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML private Label activityUpdatesLabel;
    @FXML private Label activityAllocationsLabel;
    @FXML private Label activityAssessmentsLabel;
    @FXML private Label activityTasksLabel;

    // Wizard progress controls
    @FXML private Label wizardStepLabel;
    @FXML private Label step1Dot;
    @FXML private Label step2Dot;
    @FXML private Label step3Dot;
    @FXML private Label step4Dot;
    @FXML private Label step5Dot;
    @FXML private Label step6Dot;
    @FXML private javafx.scene.control.TitledPane step1Pane;
    @FXML private javafx.scene.control.TitledPane step2Pane;
    @FXML private javafx.scene.control.TitledPane step3Pane;
    @FXML private javafx.scene.control.TitledPane step4Pane;
    @FXML private javafx.scene.control.TitledPane step5Pane;
    @FXML private javafx.scene.control.TitledPane step6Pane;

    // Assigned-team panel (shown once incident is ASSIGNED or later)
    @FXML private javafx.scene.layout.HBox assignedTeamBox;
    @FXML private Label assignedTeamLabel;
    @FXML private Label assignedLeaderLabel;
    @FXML private Label contactPhoneLabel;

    private Incident current;

    @FXML
    public void initialize() {
        // Populate header user info from active session
        User user = ClientSession.instance().getCurrentUser();
        if (user != null) {
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole() == null
                        ? "" : user.getRole().name());
            }
        }
        severityCombo.setItems(FXCollections.observableArrayList(CapSeverity.values()));
        urgencyCombo.setItems(FXCollections.observableArrayList(CapUrgency.values()));
        certaintyCombo.setItems(FXCollections.observableArrayList(CapCertainty.values()));
        loadIncident();
    }

    private void loadIncident() {
        String code = SelectedIncidentHolder.selectedIncidentCode;
        if (code == null) {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            return;
        }
        try {
            Request request = new Request(OperationType.GET_INCIDENT_DETAILS,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", code);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            current = response.dataAs();
            populateFields();
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    private void populateFields() {
        if (current == null) return;
        incidentTitle.setText("Incident " + current.getIncidentCode());
        disasterTypeBadge.setText(current.getDisasterType().displayName());
        priorityBadge.setText("Score " + current.getPriorityScore());
        statusBadge.setText(current.getStatus().displayName());
        String reporterName = current.getReportedByUserName();
        String reporterCode = current.getReportedByUserCode();
        if (reporterName == null || reporterName.isBlank()) {
            reporterLabel.setText("Reported by " + reporterCode);
        } else {
            reporterLabel.setText("Reported by " + reporterName
                    + " (" + reporterCode + ")");
        }
        reportedAtLabel.setText("Reported " + DateTimeUtil.relative(current.getReportedAt()));
        // Show the citizen's contact phone (mandatory at report time)
        // so the coordinator can call back about this report.
        if (contactPhoneLabel != null) {
            String phone = current.getContactPhone();
            contactPhoneLabel.setText("📞 Callback phone: "
                    + (phone == null || phone.isEmpty() ? "(not provided)" : phone));
        }

        // Show the assigned-team panel only when the incident has a
        // team attached. The repository hydrates assignedTeamName as
        // null for unassigned incidents.
        String teamName = current.getAssignedTeamName();
        String leaderName = current.getAssignedLeaderName();
        boolean hasTeam = teamName != null && !teamName.isEmpty();
        if (assignedTeamBox != null) {
            assignedTeamBox.setVisible(hasTeam);
            assignedTeamBox.setManaged(hasTeam);
        }
        if (hasTeam) {
            if (assignedTeamLabel != null) {
                assignedTeamLabel.setText("Assigned team: " + teamName);
            }
            if (assignedLeaderLabel != null) {
                assignedLeaderLabel.setText("Team leader: "
                        + (leaderName != null && !leaderName.isEmpty()
                                ? leaderName : "(not set)"));
            }
        }
        locationLabel.setText(current.getLocationDisplayName());
        peopleAffectedLabel.setText("People affected: " + current.getPeopleAffected());
        propertyRiskLabel.setText("Property risk: "
                + (current.getPropertyRiskLevel() == null ? "-"
                        : current.getPropertyRiskLevel()));
        descriptionDisplay.setText(current.getDescription());
        severityCombo.setValue(current.getCapSeverity());
        urgencyCombo.setValue(current.getCapUrgency());
        certaintyCombo.setValue(current.getCapCertainty());

        UserRole role = ClientSession.instance().getCurrentUser().getRole();
        boolean isCoordOrAdmin = role == UserRole.COORDINATOR || role == UserRole.ADMIN;

        // Enable buttons based on status + role
        assessButton.setDisable(!isCoordOrAdmin
                || current.getStatus() != drs.shared.enums.IncidentStatus.REPORTED);
        rejectButton.setDisable(!isCoordOrAdmin
                || (current.getStatus() != drs.shared.enums.IncidentStatus.REPORTED
                    && current.getStatus() != drs.shared.enums.IncidentStatus.ASSESSED));
        assignTeamButton.setDisable(!isCoordOrAdmin
                || current.getStatus() != drs.shared.enums.IncidentStatus.ASSESSED);
        closeButton.setDisable(!isCoordOrAdmin
                || current.getStatus() != drs.shared.enums.IncidentStatus.RESOLVED);
        allocateResourceButton.setDisable(!isCoordOrAdmin);
        damageAssessmentButton.setDisable(!isCoordOrAdmin
                && role != UserRole.TEAM_LEADER);
        recoveryTasksButton.setDisable(!isCoordOrAdmin
                && role != UserRole.TEAM_LEADER);

        // Update wizard sections based on incident status
        applyWizardState(current.getStatus());

        loadActivity();
    }

    /*   * Apply state-driven visibility/expansion logic to the 6 wizard
     * sections.
         * For each step we set three properties: <em>expanded</em>
     * (controls whether the section's body is visible), <em>dot
     * style</em> (●=done, ◉=current, ○=pending), and a friendly
     * "Step N of 6" label.
         * Mapping from incident status to active step:
     * <ul>
     *  <li>REPORTED   → step 2 active (1 done, 3-6 pending)</li>
     *  <li>ASSESSED   → step 3 active (1-2 done, 4-6 pending)</li>
     *  <li>ASSIGNED   → step 4 active (1-3 done, 5-6 pending)</li>
     *  <li>RESPONDING → step 5 active (1-4 done, 6 pending)</li>
     *  <li>RESOLVED   → step 6 active (1-5 done)</li>
     *  <li>CLOSED, REJECTED, WITHDRAWN → terminal; all steps done</li>
     * </ul>
     */
    private void applyWizardState(drs.shared.enums.IncidentStatus status) {
        if (status == null) {
            return;
        }
        // Decide which step is "current" (1-indexed)
        int currentStep;
        boolean terminal;
        switch (status) {
            case REPORTED:   currentStep = 2; terminal = false; break;
            case ASSESSED:   currentStep = 3; terminal = false; break;
            case ASSIGNED:   currentStep = 4; terminal = false; break;
            case RESPONDING: currentStep = 5; terminal = false; break;
            case RESOLVED:   currentStep = 6; terminal = false; break;
            case CLOSED:
            case REJECTED:
            case WITHDRAWN:
                currentStep = 7;  // past the end
                terminal = true;
                break;
            default:
                currentStep = 1;
                terminal = false;
        }

        javafx.scene.control.TitledPane[] panes = {
                step1Pane, step2Pane, step3Pane,
                step4Pane, step5Pane, step6Pane};
        Label[] dots = {step1Dot, step2Dot, step3Dot,
                step4Dot, step5Dot, step6Dot};

        for (int i = 0; i < 6; i++) {
            int stepNum = i + 1;
            javafx.scene.control.TitledPane pane = panes[i];
            Label dot = dots[i];
            if (pane == null || dot == null) continue;

            // Reset dot style classes
            dot.getStyleClass().removeAll(
                    "dot-done", "dot-current", "dot-pending");

            if (terminal || stepNum < currentStep) {
                // Past step - show as done; sections stay expanded
                // so context is visible; pane is disabled.
                pane.setExpanded(true);
                pane.setDisable(true);
                dot.setText("●");
                dot.getStyleClass().add("dot-done");
            } else if (stepNum == currentStep) {
                // Current step - expanded and enabled.
                pane.setExpanded(true);
                pane.setDisable(false);
                dot.setText("◉");
                dot.getStyleClass().add("dot-current");
            } else {
                // Future step - collapsed and disabled.
                pane.setExpanded(false);
                pane.setDisable(true);
                dot.setText("○");
                dot.getStyleClass().add("dot-pending");
            }
        }

        // Friendly label below the dots. For active non-terminal
        // stages, we tell the coordinator WHO is responsible for
        // moving the incident forward, so a greyed-out wizard never
        // looks "stuck".
        if (wizardStepLabel != null) {
            if (terminal) {
                wizardStepLabel.setText("Incident is "
                        + status.displayName() + " - wizard complete.");
            } else {
                String stageLabel = "Step " + currentStep + " of 6";
                String waitingFor = describeWhoActsNext(status);
                wizardStepLabel.setText(
                        stageLabel + "  ·  " + waitingFor);
            }
        }
    }

    /*   * Describe who needs to act next, given the current incident
     * status. The message is shown next to the wizard step number so
     * coordinators understand whether they need to do something or
     * wait for another role.
     */
    private String describeWhoActsNext(drs.shared.enums.IncidentStatus status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case REPORTED:
                return "⏳ Coordinator action: triage and assess "
                        + "(or reject).";
            case ASSESSED:
                return "⏳ Coordinator action: assign a response team.";
            case ASSIGNED:
                return "⏳ Waiting for team leader to allocate "
                        + "resources and start response.";
            case RESPONDING:
                return "⏳ Waiting for team leader to complete "
                        + "response (record damage, log recovery "
                        + "tasks, then mark resolved).";
            case RESOLVED:
                return "⏳ Coordinator action: review the resolution "
                        + "note and close the incident.";
            default:
                return "";
        }
    }

    /*   * Populate the inline activity panel: counts of updates,
     * allocations, damage assessments, and recovery tasks for this
     * incident. Each is fetched with a short list call and counted.
     * Errors are non-fatal - labels just show "-" if a call fails.
     */
    private void loadActivity() {
        if (current == null) {
            return;
        }
        String code = current.getIncidentCode();
        String token = ClientSession.instance().getSessionToken();

        activityUpdatesLabel.setText("Updates: "
                + safeCount(OperationType.GET_INCIDENT_HISTORY, code, token));
        activityAllocationsLabel.setText("Resource allocations: "
                + safeCount(OperationType.LIST_ALLOCATIONS_FOR_INCIDENT,
                        code, token));
        activityAssessmentsLabel.setText("Damage assessments: "
                + safeCount(OperationType.LIST_DAMAGE_ASSESSMENTS, code, token));
        activityTasksLabel.setText("Recovery tasks: "
                + safeCount(OperationType.LIST_RECOVERY_TASKS, code, token));
    }

    private String safeCount(OperationType op, String code, String token) {
        try {
            Request request = new Request(op, token);
            request.with("incidentCode", code);
            Response response = ClientSession.instance().getDrsClient()
                    .send(request);
            if (!response.isSuccess()) {
                return "-";
            }
            List<?> list = response.dataAs();
            return list == null ? "0" : String.valueOf(list.size());
        } catch (Exception e) {
            return "-";
        }
    }

    @FXML
    public void onAssess() {
        try {
            Request request = new Request(OperationType.ASSESS_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", current.getIncidentCode())
                    .with("severity", severityCombo.getValue().name())
                    .with("urgency", urgencyCombo.getValue().name())
                    .with("certainty", certaintyCombo.getValue().name());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            current = response.dataAs();
            populateFields();
            statusLabel.setText("Assessed. New score: " + current.getPriorityScore());
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onReject() {
        if (!SceneNavigator.confirm("Reject this incident? It cannot be reopened.")) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Reason for rejection");
        dialog.setContentText("Why is this report being rejected? "
                + "(min. 10 characters)");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        String reason = result.get().trim();
        if (reason.length() < 10) {
            // Client-side guard avoids the round-trip; server also
            // re-validates in IncidentService.rejectIncident.
            statusLabel.setText(
                    "Rejection reason must be at least 10 characters.");
            return;
        }
        try {
            Request request = new Request(OperationType.REJECT_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", current.getIncidentCode())
                    .with("reason", reason);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onAssignTeam() {
        ClientSession.instance().setWizardOriginIncidentCode(
                current.getIncidentCode());
        SceneNavigator.showView("/fxml/assign-team-view.fxml");
    }

    @FXML
    public void onAllocateResource() {
        ClientSession.instance().setWizardOriginIncidentCode(
                current.getIncidentCode());
        SceneNavigator.showView("/fxml/allocate-resource-view.fxml");
    }

    @FXML
    public void onDamageAssessment() {
        ClientSession.instance().setWizardOriginIncidentCode(
                current.getIncidentCode());
        SceneNavigator.showView("/fxml/damage-assessment-view.fxml");
    }

    @FXML
    public void onRecoveryTasks() {
        ClientSession.instance().setWizardOriginIncidentCode(
                current.getIncidentCode());
        SceneNavigator.showView("/fxml/recovery-task-view.fxml");
    }

    @FXML
    public void onClose() {
        if (!SceneNavigator.confirm(
                "Close incident " + current.getIncidentCode() + "?")) {
            return;
        }
        try {
            Request request = new Request(OperationType.CLOSE_INCIDENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", current.getIncidentCode());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onBack() {
        UserRole role = ClientSession.instance().getCurrentUser().getRole();
        if (role == UserRole.TEAM_LEADER) {
            SceneNavigator.showView("/fxml/team-leader-panel-view.fxml");
        } else {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
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
