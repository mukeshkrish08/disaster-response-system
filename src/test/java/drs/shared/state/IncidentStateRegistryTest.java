package drs.shared.state;

import drs.shared.enums.IncidentStatus;
import drs.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link IncidentStateRegistry}.
 *
 * Includes parameterized tests covering every valid and invalid transition
 * in the incident lifecycle state machine. This pattern is recommended by
 * the JUnit User Guide for systematic coverage of state-machine inputs
 * (JUnit Team, 2025).
  
 */
class IncidentStateRegistryTest {

    @Test
    void testReportedToAssessedAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.REPORTED, IncidentStatus.ASSESSED));
    }

    @Test
    void testReportedToRejectedAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.REPORTED, IncidentStatus.REJECTED));
    }

    @Test
    void testAssessedToAssignedAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.ASSESSED, IncidentStatus.ASSIGNED));
    }

    @Test
    void testAssignedToRespondingAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.ASSIGNED, IncidentStatus.RESPONDING));
    }

    @Test
    void testRespondingToResolvedAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.RESPONDING, IncidentStatus.RESOLVED));
    }

    @Test
    void testResolvedToClosedAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
    }

    @Test
    void testClosedTerminal() {
        assertFalse(IncidentStateRegistry.canTransition(
                IncidentStatus.CLOSED, IncidentStatus.REPORTED));
    }

    @Test
    void testRejectedTerminal() {
        assertFalse(IncidentStateRegistry.canTransition(
                IncidentStatus.REJECTED, IncidentStatus.ASSESSED));
    }

    @Test
    void testReportedToWithdrawnAllowed() {
        assertTrue(IncidentStateRegistry.canTransition(
                IncidentStatus.REPORTED, IncidentStatus.WITHDRAWN),
                "Citizens must be able to withdraw a report that "
                        + "hasn't yet been assessed");
    }

    @Test
    void testWithdrawnTerminal() {
        assertFalse(IncidentStateRegistry.canTransition(
                IncidentStatus.WITHDRAWN, IncidentStatus.ASSESSED),
                "WITHDRAWN must be a terminal state");
    }

    @Test
    void testAssessedCannotBeWithdrawn() {
        // Once a coordinator has assessed it, the citizen can't
        // walk it back - they would need to use REJECT.
        assertFalse(IncidentStateRegistry.canTransition(
                IncidentStatus.ASSESSED, IncidentStatus.WITHDRAWN));
    }

    @Test
    void testReportedCannotSkipToResolved() {
        assertFalse(IncidentStateRegistry.canTransition(
                IncidentStatus.REPORTED, IncidentStatus.RESOLVED));
    }

    @Test
    void testValidateTransitionThrowsOnInvalid() {
        assertThrows(InvalidStateTransitionException.class, () ->
                IncidentStateRegistry.validateTransition(
                        IncidentStatus.CLOSED, IncidentStatus.REPORTED));
    }

    /*   * Every legal transition documented in the state machine.
     * Each row is from-state, to-state.
     */
    @ParameterizedTest(name = "valid: {0} -> {1}")
    @CsvSource({
            "REPORTED,   ASSESSED",
            "REPORTED,   REJECTED",
            "REPORTED,   WITHDRAWN",
            "ASSESSED,   ASSIGNED",
            "ASSESSED,   REJECTED",
            "ASSIGNED,   RESPONDING",
            "RESPONDING, RESOLVED",
            "RESOLVED,   CLOSED"
    })
    void testParameterizedValidTransitions(IncidentStatus from,
                                           IncidentStatus to) {
        assertTrue(IncidentStateRegistry.canTransition(from, to),
                "Expected transition " + from + " -> " + to + " to be valid");
    }

    /*   * Transitions that must be rejected (skipping stages, reactivating
     * terminal states, or going backwards).
     */
    @ParameterizedTest(name = "invalid: {0} -> {1}")
    @CsvSource({
            // Cannot skip stages forward
            "REPORTED,   ASSIGNED",
            "REPORTED,   RESPONDING",
            "REPORTED,   RESOLVED",
            "REPORTED,   CLOSED",
            "ASSESSED,   RESPONDING",
            "ASSESSED,   RESOLVED",
            "ASSESSED,   CLOSED",
            "ASSESSED,   WITHDRAWN",
            "ASSIGNED,   RESOLVED",
            "ASSIGNED,   CLOSED",
            "ASSIGNED,   WITHDRAWN",
            "RESPONDING, CLOSED",
            "RESPONDING, WITHDRAWN",
            // Terminal states cannot transition out
            "CLOSED,     REPORTED",
            "CLOSED,     ASSESSED",
            "REJECTED,   ASSESSED",
            "REJECTED,   ASSIGNED",
            "WITHDRAWN,  ASSESSED",
            "WITHDRAWN,  REPORTED",
            // Cannot go backwards
            "ASSESSED,   REPORTED",
            "ASSIGNED,   ASSESSED",
            "RESPONDING, ASSIGNED",
            "RESOLVED,   RESPONDING"
    })
    void testParameterizedInvalidTransitions(IncidentStatus from,
                                             IncidentStatus to) {
        assertFalse(IncidentStateRegistry.canTransition(from, to),
                "Expected transition " + from + " -> " + to
                        + " to be rejected");
    }
}
