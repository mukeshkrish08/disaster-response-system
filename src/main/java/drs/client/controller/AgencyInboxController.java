package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.NotificationStatus;
import drs.shared.model.Notification;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import drs.shared.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Agency rep inbox - pending + acknowledged notifications.
  
 */
public class AgencyInboxController {

    @FXML private Label pendingCountLabel;
    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, String> timeColumn;
    @FXML private TableColumn<Notification, String> incidentColumn;
    @FXML private TableColumn<Notification, String> messageColumn;
    @FXML private TableColumn<Notification, String> statusColumn;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    @FXML
    public void initialize() {
        if (notificationsTable != null) {
            notificationsTable.setPlaceholder(new javafx.scene.control.Label("No notifications for your department."));
        }
        if (notificationsTable != null) {
            notificationsTable.setColumnResizePolicy(
                    javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        }
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
        timeColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.relative(c.getValue().getCreatedAt())));
        incidentColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getIncidentCode()));
        messageColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getMessage()));
        statusColumn.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().displayName()));
        onRefresh();
    }

    @FXML
    public void onRefresh() {
        try {
            Request request = new Request(OperationType.LIST_NOTIFICATIONS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Notification> list = response.dataAs();
            long pending = list.stream().filter(Notification::isPending).count();
            pendingCountLabel.setText("Pending: " + pending);
            notificationsTable.setItems(FXCollections.observableArrayList(list));
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onAcknowledge() {
        Notification selected = notificationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a notification first.");
            return;
        }
        if (selected.getStatus() != NotificationStatus.PENDING) {
            statusLabel.setText("This notification is already acknowledged.");
            return;
        }
        try {
            Request request = new Request(OperationType.ACKNOWLEDGE_NOTIFICATION,
                    ClientSession.instance().getSessionToken());
            request.with("notificationCode", selected.getNotificationCode());
            Response response = ClientSession.instance().getDrsClient().send(request);
            if (response.isSuccess()) {
                statusLabel.setText("Acknowledged " + selected.getNotificationCode());
                onRefresh();
            } else {
                statusLabel.setText(response.getErrorMessage());
            }
        } catch (ServerOfflineException e) {
            statusLabel.setText("● Server offline.");
        }
    }

    @FXML
    public void onSignOut() {
        try {
            ClientSession.instance().getDrsClient().send(
                    new Request(OperationType.LOGOUT,
                            ClientSession.instance().getSessionToken()));
        } catch (Exception ignored) { /* No-op */ }
        ClientSession.instance().clearSession();
        SceneNavigator.showView("/fxml/login-view.fxml");
    }
}
