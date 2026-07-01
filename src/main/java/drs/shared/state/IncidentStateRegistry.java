package drs.shared.state;

import drs.shared.enums.IncidentStatus;
import drs.shared.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the valid state transitions of the incident lifecycle.
 * See the static initializer for the authoritative transition list.
  
 */
public final class IncidentStateRegistry {

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS;

    /*   * Valid transitions:
     *  REPORTED   -> ASSESSED, REJECTED, WITHDRAWN
     *  ASSESSED   -> ASSIGNED, REJECTED
     *  ASSIGNED   -> RESPONDING
     *  RESPONDING -> RESOLVED
     *  RESOLVED   -> CLOSED
     *  CLOSED, REJECTED, WITHDRAWN -> (terminal)
         * Note: only the original reporter can move REPORTED -> WITHDRAWN;
     * that ownership check lives in IncidentService.withdrawIncident,
     * not here.
     */
    static {
        TRANSITIONS = new EnumMap<>(IncidentStatus.class);
        TRANSITIONS.put(IncidentStatus.REPORTED,
                EnumSet.of(IncidentStatus.ASSESSED,
                        IncidentStatus.REJECTED,
                        IncidentStatus.WITHDRAWN));
        TRANSITIONS.put(IncidentStatus.ASSESSED,
                EnumSet.of(IncidentStatus.ASSIGNED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.ASSIGNED,
                EnumSet.of(IncidentStatus.RESPONDING));
        TRANSITIONS.put(IncidentStatus.RESPONDING,
                EnumSet.of(IncidentStatus.RESOLVED));
        TRANSITIONS.put(IncidentStatus.RESOLVED,
                EnumSet.of(IncidentStatus.CLOSED));
        TRANSITIONS.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
        TRANSITIONS.put(IncidentStatus.REJECTED, EnumSet.noneOf(IncidentStatus.class));
        TRANSITIONS.put(IncidentStatus.WITHDRAWN, EnumSet.noneOf(IncidentStatus.class));
    }

    private IncidentStateRegistry() {
        // Static utility
    }

    /*   * Verify a proposed transition is legal.
         * @param from current status
     * @param to   proposed next status
     * @throws InvalidStateTransitionException if not allowed
     */
    public static void validateTransition(IncidentStatus from, IncidentStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(getFriendlyError(from, to));
        }
    }

    /*   * @param from current status (nullable)
     * @param to   proposed next status (nullable)
     * @return true if the transition is allowed
     */
    public static boolean canTransition(IncidentStatus from, IncidentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<IncidentStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /*   * @param from current status
     * @return set of statuses reachable in one step
     */
    public static Set<IncidentStatus> nextStates(IncidentStatus from) {
        if (from == null) {
            return EnumSet.noneOf(IncidentStatus.class);
        }
        Set<IncidentStatus> set = TRANSITIONS.get(from);
        return set == null ? EnumSet.noneOf(IncidentStatus.class) : EnumSet.copyOf(set);
    }

    /*   * Build a user-friendly error message for a denied transition.
         * @param from current status
     * @param to   proposed next status
     * @return short explanation
     */
    public static String getFriendlyError(IncidentStatus from, IncidentStatus to) {
        Set<IncidentStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || allowed.isEmpty()) {
            return "Incident is " + from.displayName() + " - no further changes allowed.";
        }
        return "Cannot move from " + from.displayName() + " to " + to.displayName()
             + ". Allowed next states: " + allowed;
    }
}
