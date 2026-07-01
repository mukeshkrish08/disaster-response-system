package drs.client.controller;

/**
 * Tiny holder for passing the selected incident code between screens.
 * JavaFX SceneNavigator doesn't carry arguments by default; this avoids
 * the boilerplate of a custom navigator API.
  
 */
final class SelectedIncidentHolder {

    static volatile String selectedIncidentCode;
    static volatile String selectedResourceCode;
    static volatile String selectedTaskCode;

    private SelectedIncidentHolder() {
        // Static state holder
    }
}
