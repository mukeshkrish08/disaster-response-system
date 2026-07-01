package drs.shared.util;

import drs.shared.model.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HashChain}.
  
 */
class HashChainTest {

    @Test
    void testHashIsDeterministicForSameInputs() {
        String h1 = HashChain.computeHash("prev", "content");
        String h2 = HashChain.computeHash("prev", "content");
        assertEquals(h1, h2);
        assertEquals(64, h1.length()); // 64 hex chars = 256-bit SHA
    }

    @Test
    void testDifferentPrevHashChangesOutput() {
        String h1 = HashChain.computeHash("prev1", "content");
        String h2 = HashChain.computeHash("prev2", "content");
        assertNotEquals(h1, h2);
    }

    @Test
    void testVerifyChainOnValidSequence() {
        AuditLog a = buildEntry("AUD-2026-0001", "LOGIN_SUCCESS", null);
        AuditLog b = buildEntry("AUD-2026-0002", "REPORT_INCIDENT", a.getCurrentHash());
        AuditLog c = buildEntry("AUD-2026-0003", "ASSESS_INCIDENT", b.getCurrentHash());
        a.setAuditPk(1); b.setAuditPk(2); c.setAuditPk(3);
        // Compute their currentHash properly
        a.setCurrentHash(HashChain.computeHash(a.getPrevHash(), HashChain.buildContent(a)));
        b.setPrevHash(a.getCurrentHash());
        b.setCurrentHash(HashChain.computeHash(b.getPrevHash(), HashChain.buildContent(b)));
        c.setPrevHash(b.getCurrentHash());
        c.setCurrentHash(HashChain.computeHash(c.getPrevHash(), HashChain.buildContent(c)));

        assertTrue(HashChain.verifyChain(Arrays.asList(a, b, c)));
    }

    @Test
    void testVerifyChainDetectsTamper() {
        AuditLog a = buildEntry("AUD-2026-0001", "LOGIN_SUCCESS", null);
        a.setAuditPk(1);
        a.setCurrentHash(HashChain.computeHash(a.getPrevHash(), HashChain.buildContent(a)));

        AuditLog b = buildEntry("AUD-2026-0002", "REPORT_INCIDENT", a.getCurrentHash());
        b.setAuditPk(2);
        b.setCurrentHash(HashChain.computeHash(b.getPrevHash(), HashChain.buildContent(b)));

        // Tamper: change the action without recomputing the hash
        b.setAction("LOGOUT");
        List<AuditLog> entries = new ArrayList<>(Arrays.asList(a, b));
        assertFalse(HashChain.verifyChain(entries));
    }

    @Test
    void testEmptyChainIsValid() {
        assertTrue(HashChain.verifyChain(new ArrayList<>()));
    }

    private AuditLog buildEntry(String code, String action, String prev) {
        AuditLog a = new AuditLog();
        a.setAuditCode(code);
        a.setAction(action);
        a.setSuccess(true);
        a.setPrevHash(prev);
        a.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        return a;
    }
}
