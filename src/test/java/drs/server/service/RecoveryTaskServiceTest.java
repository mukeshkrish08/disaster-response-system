package drs.server.service;

import drs.shared.enums.RecoveryTaskStatus;
import drs.shared.enums.RecoveryTaskType;
import drs.shared.model.RecoveryTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link RecoveryTask} model defaults used by
 * {@link RecoveryTaskService}. Full integration testing of the state
 * machine requires a database.
  
 */
class RecoveryTaskServiceTest {

    @Test
    void testDefaultStatusIsOpen() {
        RecoveryTask t = new RecoveryTask();
        assertEquals(RecoveryTaskStatus.OPEN, t.getStatus());
    }

    @Test
    void testNewTaskHasNoAssigneeNorTimes() {
        RecoveryTask t = new RecoveryTask();
        assertNull(t.getAssignedToUserPk());
        assertNull(t.getAssignedAt());
        assertNull(t.getStartedAt());
        assertNull(t.getCompletedAt());
        assertNull(t.getBlockedReason());
    }

    @Test
    void testAllStatusesHaveDisplayName() {
        for (RecoveryTaskStatus s : RecoveryTaskStatus.values()) {
            assertNotNull(s.displayName());
        }
    }

    @Test
    void testAllTypesHaveDisplayName() {
        for (RecoveryTaskType t : RecoveryTaskType.values()) {
            assertNotNull(t.displayName());
        }
    }
}
