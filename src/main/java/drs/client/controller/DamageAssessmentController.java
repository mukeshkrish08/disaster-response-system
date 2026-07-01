package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;
import drs.shared.model.DamageAssessment;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.util.List;

/**
 * Damage Assessment view (Feature 2).
 *
 * Shows previous assessments and lets the user record a new one.
  
 */
public class DamageAssessmentController {

    @FXML private Label pageTitle;
    @FXML private TableView<DamageAssessment> previousAssessmentsTable;
    @FXML private TableColumn<DamageAssessment, String> timeColumn;
    @FXML private TableColumn<DamageAssessment, String> assessorColumn;
    @FXML private TableColumn<DamageAssessment, String> buildingColumn;
    @FXML private TableColumn<DamageAssessment, Number> casualtyColumn;

    @FXML private ComboBox<DamageLevel> buildingDamageCombo;
    @FXML private ComboBox<InfrastructureStatus> roadStatusCombo;
    @FXML private ComboBox<InfrastructureStatus> powerStatusCombo;
    @FXML private ComboBox<InfrastructureStatus> waterStatusCombo;
    @FXML private Spinner<Integer> casualtyEstimateSpinner;
    @FXML private TextArea notesArea;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    private String incidentCode;

    @FXML
    public void initialize() {
        if (previousAssessmentsTable != null) {
            previousAssessmentsTable.setPlaceholder(new javafx.scene.control.Label("No previous assessments. Be the first to record damage."));
        }
        if (previousAssessmentsTable != null) {
            previousAssessmentsTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
        populateUserHeader();
        incidentCode = SelectedIncidentHolder.selectedIncidentCode;
        if (incidentCode == null) {
            SceneNavigator.showView("/fxml/coordinator-dashboard-view.fxml");
            return;
        }
        pageTitle.setText("Damage assessment for " + incidentCode);

        buildingDamageCombo.setItems(FXCollections.observableArrayList(DamageLevel.values()));
        buildingDamageCombo.setValue(DamageLevel.NONE);
        roadStatusCombo.setItems(FXCollections.observableArrayList(InfrastructureStatus.values()));
        roadStatusCombo.setValue(InfrastructureStatus.OPERATIONAL);
        powerStatusCombo.setItems(FXCollections.observableArrayList(InfrastructureStatus.values()));
        powerStatusCombo.setValue(InfrastructureStatus.OPERATIONAL);
        waterStatusCombo.setItems(FXCollections.observableArrayList(InfrastructureStatus.values()));
        waterStatusCombo.setValue(InfrastructureStatus.OPERATIONAL);
        casualtyEstimateSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0));

        timeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.relative(c.getValue().getAssessedAt())));
        assessorColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getAssessedByUserCode()));
        buildingColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getBuildingDamageLevel().displayName()));
        casualtyColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(
                        c.getValue().getCasualtyEstimate()));

        loadPrevious();
    }

    private void loadPrevious() {
        try {
            Request request = new Request(OperationType.LIST_DAMAGE_ASSESSMENTS,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode);
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                List<DamageAssessment> list = response.dataAs();
                previousAssessmentsTable.setItems(
                        FXCollections.observableArrayList(list));
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onSave() {
        try {
            Request request = new Request(OperationType.RECORD_DAMAGE_ASSESSMENT,
                    ClientSession.instance().getSessionToken());
            request.with("incidentCode", incidentCode)
                    .with("buildingDamageLevel", buildingDamageCombo.getValue().name())
                    .with("roadStatus", roadStatusCombo.getValue().name())
                    .with("powerStatus", powerStatusCombo.getValue().name())
                    .with("waterStatus", waterStatusCombo.getValue().name())
                    .with("casualtyEstimate", casualtyEstimateSpinner.getValue())
                    .with("notes",
                            notesArea.getText() == null ? "" : notesArea.getText());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                DamageAssessment saved = response.dataAs();
                SceneNavigator.showInfo("Assessment "
                        + saved.getAssessmentCode() + " recorded.");
                notesArea.clear();
                loadPrevious();
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
