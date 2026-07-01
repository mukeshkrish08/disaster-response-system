package drs.client.controller;

import drs.client.SceneNavigator;
import drs.client.net.ClientSession;
import drs.client.net.ServerOfflineException;
import drs.shared.enums.IncidentStatus;
import drs.shared.model.Incident;
import drs.shared.model.Resource;
import drs.shared.model.ResourceAllocation;
import drs.shared.model.User;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocate Resources (Feature 1) - multi-resource cart workflow.
 *
 * The coordinator picks an incident, then builds a list of
 * resources to allocate by repeatedly choosing a resource +
 * quantity + notes and clicking "Add to allocation list". When
 * happy with the list, they click "Allocate all N items" which
 * fires one {@code ALLOCATE_RESOURCE} request per cart item in
 * sequence.
 *
 * <b>Why one-request-per-item, not a bulk operation:</b> in
 * disaster response, partial fulfilment is the correct behaviour.
 * If a competing coordinator grabbed one of the resources between
 * cart submission and server processing, the coordinator should
 * keep the items that did allocate and only retry the failed ones.
 * Each request is its own database transaction; the loop tracks
 * successes and failures and reports them per-item.
 *
 * On full success, navigation returns to the incident wizard
 * (when launched from Step 4 of the wizard via
 * {@code ClientSession.wizardOriginIncidentCode}) or to the
 * resource dashboard otherwise.
  
 */
public class AllocateResourceController {

    // ----- Form ("choose what to allocate") -----
    @FXML private ComboBox<Incident> incidentCombo;
    @FXML private ComboBox<Resource> resourceCombo;
    @FXML private Label availableLabel;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private TextArea notesArea;
    @FXML private Button addToCartButton;
    @FXML private Label addHintLabel;

    // ----- Cart ("allocation list") -----
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> cartResourceColumn;
    @FXML private TableColumn<CartItem, String> cartQuantityColumn;
    @FXML private TableColumn<CartItem, String> cartNotesColumn;
    @FXML private TableColumn<CartItem, Void> cartActionColumn;
    @FXML private Label cartHeadingLabel;
    @FXML private Button clearCartButton;
    @FXML private Button allocateAllButton;

    // ----- Header + status -----
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;

    /** In-memory cart. Backs the cartTable; cleared on submit or cancel. */
    private final ObservableList<CartItem> cart =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        populateUserHeader();
        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));

        setupCartTable();
        cartTable.setItems(cart);
        cartTable.setPlaceholder(new Label(
                "No resources queued yet. Pick a resource above and click "
                + "\"+ Add to allocation list\"."));
        // Update the heading label + button counters whenever cart changes.
        cart.addListener((javafx.collections.ListChangeListener<CartItem>) ch -> {
            refreshCartCounters();
        });
        refreshCartCounters();

        loadIncidents();
        loadResources();
    }

    /*   * Configure cart table cell factories: three text columns and a
     * "✕ Remove" button column that deletes the row from the cart on
     * click. The Remove cell uses a TableCell subclass so the button
     * renders inside each row even when the row data changes.
     */
    private void setupCartTable() {
        cartResourceColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getResourceDisplay()));
        cartQuantityColumn.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getQuantity())));
        cartNotesColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNotes()));

        cartActionColumn.setCellFactory(col ->
                new TableCell<CartItem, Void>() {
            private final Button removeButton =
                    new Button("✕ Remove");
            {
                removeButton.getStyleClass().add("btn-secondary");
                removeButton.setOnAction(e -> {
                    CartItem item = getTableView().getItems()
                            .get(getIndex());
                    onRemoveFromCart(item);
                });
            }
            @Override
            protected void updateItem(Void unused, boolean empty) {
                super.updateItem(unused, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    HBox wrap = new HBox(removeButton);
                    wrap.setStyle("-fx-alignment: center;");
                    setGraphic(wrap);
                }
            }
        });
    }

    /** Update count-bearing labels + enable/disable submit buttons. */
    private void refreshCartCounters() {
        int n = cart.size();
        if (cartHeadingLabel != null) {
            cartHeadingLabel.setText("2. Allocation list ("
                    + n + " item" + (n == 1 ? "" : "s") + ")");
        }
        if (allocateAllButton != null) {
            allocateAllButton.setText("Allocate all " + n + " item"
                    + (n == 1 ? "" : "s"));
            allocateAllButton.setDisable(n == 0);
        }
        if (clearCartButton != null) {
            clearCartButton.setDisable(n == 0);
        }
    }

    // -------------------------------------------------------------
    // Load incidents and resources from server
    // -------------------------------------------------------------

    private void loadIncidents() {
        try {
            Request request = new Request(OperationType.LIST_INCIDENTS,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            markOnline();
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Incident> list = response.dataAs();
            // Hide CLOSED / REJECTED / WITHDRAWN - not actionable.
            list.removeIf(i -> i.getStatus() == IncidentStatus.CLOSED
                    || i.getStatus() == IncidentStatus.REJECTED
                    || i.getStatus() == IncidentStatus.WITHDRAWN);
            incidentCombo.setItems(FXCollections.observableArrayList(list));

            // Pre-select if a holder hint exists (came from wizard).
            String preselected = SelectedIncidentHolder.selectedIncidentCode;
            if (preselected != null) {
                for (Incident i : list) {
                    if (preselected.equals(i.getIncidentCode())) {
                        incidentCombo.setValue(i);
                        break;
                    }
                }
            } else if (!list.isEmpty()) {
                incidentCombo.setValue(list.get(0));
            }
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline.");
        }
    }

    private void loadResources() {
        try {
            Request request = new Request(OperationType.LIST_RESOURCES,
                    ClientSession.instance().getSessionToken());
            Response response = ClientSession.instance()
                    .getDrsClient().send(request);
            markOnline();
            if (!response.isSuccess()) {
                statusLabel.setText(response.getErrorMessage());
                return;
            }
            List<Resource> list = response.dataAs();
            resourceCombo.setItems(FXCollections.observableArrayList(list));

            String preselected = SelectedIncidentHolder.selectedResourceCode;
            if (preselected != null) {
                for (Resource r : list) {
                    if (preselected.equals(r.getResourceCode())) {
                        resourceCombo.setValue(r);
                        applyResourceConstraints(r);
                        break;
                    }
                }
            }
        } catch (ServerOfflineException e) {
            markOffline();
            statusLabel.setText("Server offline.");
        }
    }

    @FXML
    public void onIncidentChange() {
        // No-op for now; selection is captured on submit.
    }

    @FXML
    public void onResourceChange() {
        Resource selected = resourceCombo.getValue();
        if (selected != null) {
            applyResourceConstraints(selected);
        }
    }

    /*   * Refresh the quantity spinner bounds + available-units label
     * when the user picks a different resource. Caps the spinner at
     * (available − already-queued) so the user can't queue more than
     * inventory has left.
     */
    private void applyResourceConstraints(Resource r) {
        int available = r.getQuantityAvailable();
        int queued = totalQueuedFor(r.getResourceCode());
        int remaining = Math.max(0, available - queued);
        availableLabel.setText(available + " of " + r.getQuantityTotal()
                + " units"
                + (queued > 0 ? "  ·  " + queued + " already queued" : ""));
        int max = Math.max(1, remaining);
        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, max, Math.min(1, max)));
        quantitySpinner.setDisable(remaining == 0);
    }

    /** Total quantity already in the cart for the given resource. */
    private int totalQueuedFor(String resourceCode) {
        int sum = 0;
        for (CartItem item : cart) {
            if (resourceCode.equals(item.getResource().getResourceCode())) {
                sum += item.getQuantity();
            }
        }
        return sum;
    }

    // -------------------------------------------------------------
    // Cart actions
    // -------------------------------------------------------------

    /*   * Validate the form fields, then add a new line item to the cart.
     * Duplicate resources (same resource_code added twice) are merged
     * into a single line: quantities sum, notes are concatenated with
     * a " · " separator so the user keeps a trail of both intents.
     */
    @FXML
    public void onAddToCart() {
        Resource resource = resourceCombo.getValue();
        Integer quantity = quantitySpinner.getValue();

        if (resource == null) {
            statusLabel.setText("Pick a resource first.");
            return;
        }
        if (quantity == null || quantity <= 0) {
            statusLabel.setText("Quantity must be at least 1.");
            return;
        }
        int alreadyQueued = totalQueuedFor(resource.getResourceCode());
        if (alreadyQueued + quantity > resource.getQuantityAvailable()) {
            int remaining = resource.getQuantityAvailable() - alreadyQueued;
            statusLabel.setText("Only " + resource.getQuantityAvailable()
                    + " available; " + alreadyQueued
                    + " already queued. Can add at most " + remaining
                    + " more.");
            return;
        }

        String notes = notesArea.getText() == null
                ? "" : notesArea.getText().trim();

        // Merge if same resource is already in cart.
        CartItem existing = null;
        for (CartItem item : cart) {
            if (item.getResource().getResourceCode()
                    .equals(resource.getResourceCode())) {
                existing = item;
                break;
            }
        }
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            if (!notes.isEmpty()) {
                String merged = existing.getNotes() == null
                        || existing.getNotes().isEmpty()
                        ? notes
                        : existing.getNotes() + " · " + notes;
                existing.setNotes(merged);
            }
            cartTable.refresh();
            statusLabel.setText("Merged with existing entry: "
                    + resource.getResourceName()
                    + " is now " + existing.getQuantity() + " unit(s).");
        } else {
            cart.add(new CartItem(resource, quantity, notes));
            statusLabel.setText("Added " + quantity + " × "
                    + resource.getResourceName()
                    + ". Add more, or click Allocate all.");
        }

        // Clear form for next entry, refresh constraints because
        // queued count just changed.
        notesArea.clear();
        applyResourceConstraints(resource);
        refreshCartCounters();
    }

    /** Remove a single cart line. Called by the per-row ✕ button. */
    private void onRemoveFromCart(CartItem item) {
        cart.remove(item);
        statusLabel.setText("Removed " + item.getResource().getResourceName()
                + " from list.");
        // Re-apply constraints in case the same resource is selected.
        Resource current = resourceCombo.getValue();
        if (current != null
                && current.getResourceCode().equals(
                        item.getResource().getResourceCode())) {
            applyResourceConstraints(current);
        }
    }

    /** Empty the entire cart after user confirmation. */
    @FXML
    public void onClearCart() {
        if (cart.isEmpty()) {
            return;
        }
        if (!SceneNavigator.confirm("Clear all " + cart.size()
                + " queued allocations? They have not been allocated yet.")) {
            return;
        }
        cart.clear();
        statusLabel.setText("Allocation list cleared.");
        Resource current = resourceCombo.getValue();
        if (current != null) {
            applyResourceConstraints(current);
        }
    }

    // -------------------------------------------------------------
    // Submit cart - loop over items, fire one ALLOCATE_RESOURCE per
    // -------------------------------------------------------------

    /*   * Submit every line in the cart, sequentially, as independent
     * ALLOCATE_RESOURCE requests. Tracks per-item success vs failure
     * and shows a summary message at the end. On full success,
     * returns to the incident wizard (if launched from there) or the
     * resource dashboard.
     */
    @FXML
    public void onAllocate() {
        Incident incident = incidentCombo.getValue();
        if (incident == null) {
            statusLabel.setText("Please pick an incident first.");
            return;
        }
        if (cart.isEmpty()) {
            statusLabel.setText("Add at least one resource to the list.");
            return;
        }
        if (!SceneNavigator.confirm("Allocate " + cart.size()
                + " resource(s) to " + incident.getIncidentCode() + "?")) {
            return;
        }

        // Disable controls during the loop so the user can't double-submit.
        allocateAllButton.setDisable(true);
        addToCartButton.setDisable(true);
        clearCartButton.setDisable(true);

        int total = cart.size();
        int succeeded = 0;
        List<String> failures = new ArrayList<>();
        // Copy the cart so removals during loop don't break iteration.
        List<CartItem> work = new ArrayList<>(cart);

        for (int i = 0; i < work.size(); i++) {
            CartItem item = work.get(i);
            statusLabel.setText("Allocating " + (i + 1) + " of " + total
                    + ": " + item.getResource().getResourceName() + "…");
            try {
                Request request = new Request(OperationType.ALLOCATE_RESOURCE,
                        ClientSession.instance().getSessionToken());
                request.with("resourceCode",
                                item.getResource().getResourceCode())
                        .with("incidentCode", incident.getIncidentCode())
                        .with("quantity", item.getQuantity())
                        .with("notes", item.getNotes() == null
                                ? "" : item.getNotes());
                Response response = ClientSession.instance()
                        .getDrsClient().send(request);
                if (response.isSuccess()) {
                    succeeded++;
                    cart.remove(item);
                } else {
                    failures.add(item.getResource().getResourceName()
                            + ": " + response.getErrorMessage());
                }
            } catch (ServerOfflineException e) {
                // Server died mid-loop. Stop here; report partial.
                markOffline();
                int remaining = total - succeeded - failures.size();
                statusLabel.setText(succeeded + " of " + total
                        + " allocated before server went offline. "
                        + remaining + " not attempted. "
                        + "Reconnect and retry the remaining items.");
                allocateAllButton.setDisable(cart.isEmpty());
                addToCartButton.setDisable(false);
                clearCartButton.setDisable(cart.isEmpty());
                refreshCartCounters();
                return;
            }
        }

        // Re-enable controls.
        addToCartButton.setDisable(false);
        refreshCartCounters();

        if (failures.isEmpty()) {
            // Full success - show toast and navigate back.
            SceneNavigator.showInfo("Allocated " + succeeded + " resource(s) to "
                    + incident.getIncidentCode());
            String originCode = ClientSession.instance().consumeWizardOrigin();
            if (originCode != null) {
                SelectedIncidentHolder.selectedIncidentCode = originCode;
                SceneNavigator.showView("/fxml/incident-details-view.fxml");
            } else {
                SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
            }
        } else {
            // Partial success - stay on page so user can decide.
            StringBuilder summary = new StringBuilder();
            summary.append(succeeded).append(" of ").append(total)
                   .append(" allocated. Failed: ");
            for (int i = 0; i < failures.size(); i++) {
                if (i > 0) summary.append("; ");
                summary.append(failures.get(i));
            }
            statusLabel.setText(summary.toString());
        }
    }

    @FXML
    public void onCancel() {
        if (!cart.isEmpty()) {
            if (!SceneNavigator.confirm("Discard " + cart.size()
                    + " queued allocation(s) and leave this page?")) {
                return;
            }
        }
        // If we came from the incident wizard (Step 4), return there
        // so the coordinator stays in context. Otherwise (entry from
        // the resource inventory page), default to the resource
        // dashboard. consumeWizardOrigin both reads and clears the
        // flag, so a second click won't bounce again.
        String originCode = ClientSession.instance().consumeWizardOrigin();
        if (originCode != null) {
            SelectedIncidentHolder.selectedIncidentCode = originCode;
            SceneNavigator.showView("/fxml/incident-details-view.fxml");
        } else {
            SceneNavigator.showView("/fxml/resource-dashboard-view.fxml");
        }
    }

    // -------------------------------------------------------------
    // Connection indicator helpers
    // -------------------------------------------------------------

    private void markOnline() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("● Connected");
            connectionStatusLabel.getStyleClass().removeAll(
                    "net-offline", "net-reconnecting");
            if (!connectionStatusLabel.getStyleClass().contains("net-online")) {
                connectionStatusLabel.getStyleClass().add("net-online");
            }
        }
    }

    private void markOffline() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("● Offline");
            connectionStatusLabel.getStyleClass().removeAll(
                    "net-online", "net-reconnecting");
            if (!connectionStatusLabel.getStyleClass().contains("net-offline")) {
                connectionStatusLabel.getStyleClass().add("net-offline");
            }
        }
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
     * session and returning to the login screen.
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

    // -------------------------------------------------------------
    // Cart item model
    // -------------------------------------------------------------

    /*   * In-memory model for one line in the allocation cart. Not
     * persisted - exists only while this controller is alive.
     */
    public static class CartItem {
        private final Resource resource;
        private int quantity;
        private String notes;

        public CartItem(Resource resource, int quantity, String notes) {
            this.resource = resource;
            this.quantity = quantity;
            this.notes = notes;
        }
        public Resource getResource() { return resource; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        /** Display name for the resource column. */
        public String getResourceDisplay() {
            return resource.getResourceCode() + " - "
                    + resource.getResourceName();
        }
    }
}
