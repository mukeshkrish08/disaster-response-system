package drs.client.net;

import drs.shared.model.User;

/**
 * Singleton holding the active session token, current user, and shared
 * {@link DrsClient}. Used by every controller to talk to the server.
  
 */
public final class ClientSession {

    private static volatile ClientSession instance;

    private DrsClient drsClient;
    private String sessionToken;
    private User currentUser;

    /*   * When a controller launches a "child" screen (Assign Team, Allocate
     * Resource, Damage Assessment, Recovery Tasks) from the incident
     * wizard, it stashes the originating incident's code here so the
     * child screen can return the user back to the wizard on success
     * rather than dumping them at a dashboard.
         * Cleared when:
     *  1. The user navigates back to a dashboard manually, or
     *  2. The wizard origin has been honoured by a child screen.
     */
    private String wizardOriginIncidentCode;

    private ClientSession() {
        // Singleton
    }

    /*   * @return the singleton instance, created lazily
     */
    public static ClientSession instance() {
        ClientSession local = instance;
        if (local == null) {
            synchronized (ClientSession.class) {
                local = instance;
                if (local == null) {
                    local = new ClientSession();
                    instance = local;
                }
            }
        }
        return local;
    }

    /*   * @return the shared DrsClient, lazily connecting on first use
     */
    public synchronized DrsClient getDrsClient() {
        if (drsClient == null) {
            drsClient = new DrsClient(ClientConfiguration.serverHost(),
                    ClientConfiguration.serverPort());
        }
        return drsClient;
    }

    public synchronized void setSession(String token, User user) {
        this.sessionToken = token;
        this.currentUser = user;
    }

    public synchronized void clearSession() {
        this.sessionToken = null;
        this.currentUser = null;
    }

    public synchronized boolean isAuthenticated() {
        return sessionToken != null && currentUser != null;
    }

    public synchronized String getSessionToken() {
        return sessionToken;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /*   * Record that the next child screen was launched from the
     * incident wizard for the given incident code. On success,
     * that child screen should call {@link #consumeWizardOrigin()}
     * which returns the code (if any) and clears it.
     */
    public synchronized void setWizardOriginIncidentCode(String code) {
        this.wizardOriginIncidentCode = code;
    }

    /*   * Consume the wizard origin: returns the stashed incident code
     * (may be null) and clears it so back-navigation only fires once.
     */
    public synchronized String consumeWizardOrigin() {
        String code = this.wizardOriginIncidentCode;
        this.wizardOriginIncidentCode = null;
        return code;
    }
}
