package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class BranchDecisionTest {
    @Test
    void automaticWholeObjectMatrix() {
        assertEquals(new BranchDecision.Result(BranchDecision.Source.LIVE, false, false), BranchDecision.resolve("B", "L", "B", null));
        assertEquals(new BranchDecision.Result(BranchDecision.Source.WORK, false, true), BranchDecision.resolve("B", "B", "W", null));
        assertEquals(new BranchDecision.Result(BranchDecision.Source.LIVE, false, false), BranchDecision.resolve("B", "W", "W", null));
        assertEquals(new BranchDecision.Result(BranchDecision.Source.WORK, true, false), BranchDecision.resolve("B", "L", "W", null));
    }

    @Test
    void storedChoicePrecedesAutomaticMatrixAndStaleChoicePrecedesNoop() {
        var live = new BranchDecision.StoredChoice(BranchDecision.Source.LIVE, "B", "W");
        assertEquals(BranchDecision.Source.LIVE, BranchDecision.resolve("B", "B", "W", live).source());
        var work = new BranchDecision.StoredChoice(BranchDecision.Source.WORK, "L", "B");
        assertTrue(BranchDecision.resolve("B", "L", "B", work).writeRequired());
        var same = new BranchDecision.StoredChoice(BranchDecision.Source.WORK, "W", "W");
        assertFalse(BranchDecision.resolve("B", "W", "W", same).writeRequired());
        assertThrows(BranchDecision.StaleChoiceException.class, () -> BranchDecision.resolve("B", "W", "W", work));
    }

    @Test
    void absentTrashAndNullContentRemainDistinctAcrossDecisions() {
        BranchSnapshot absent = BranchSnapshot.absent();
        Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("title", null);
        BranchSnapshot active = new BranchSnapshot(BranchSnapshot.State.ACTIVE, fields);
        BranchSnapshot trash = new BranchSnapshot(BranchSnapshot.State.TRASH, fields);
        assertNotEquals(absent.hash(), active.hash());
        assertNotEquals(trash.hash(), active.hash());
        assertTrue(BranchDecision.resolve(active.hash(), active.hash(), trash.hash(), null).writeRequired());
        assertTrue(BranchDecision.resolve(active.hash(), trash.hash(), new BranchSnapshot(BranchSnapshot.State.ACTIVE, Map.of("title", "changed")).hash(), null).conflict());
    }

    @Test
    void directoryMarkerHasNoBinaryContentAndCannotBeDeleted() {
        BranchSnapshot directory = BranchSnapshot.file(BranchSnapshot.State.ACTIVE, "/files/new", "DIRECTORY", null);
        assertNotEquals(directory.hash(), BranchSnapshot.absent().hash());
        assertEquals(directory.hash(), BranchSnapshot.fromJson(directory.json()).hash());
        assertThrows(IllegalArgumentException.class, () -> BranchSnapshot.file(BranchSnapshot.State.ACTIVE, "/files/new", "DIRECTORY", "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new BranchChange(1, 1, BranchType.FILE, "/files/new", BranchChange.Operation.DELETE, directory, BranchSnapshot.absent(), null));
    }

    @Test
    void stableIdentityAndNewObjectDeletionAreExplicit() {
        BranchSnapshot content = new BranchSnapshot(BranchSnapshot.State.ACTIVE, Map.of("title", "new"));
        BranchChange change = new BranchChange(1, 1, BranchType.DOCUMENT, "42", BranchChange.Operation.UPSERT, BranchSnapshot.absent(), content, null);
        assertEquals("document:42", change.identity());
        assertTrue(change.decide(BranchSnapshot.absent()).writeRequired());
        assertThrows(IllegalArgumentException.class, () -> new BranchChange(1, 1, BranchType.DOCUMENT, "042", BranchChange.Operation.UPSERT, BranchSnapshot.absent(), content, null));
        assertThrows(IllegalArgumentException.class, () -> new BranchChange(1, 1, BranchType.DOCUMENT, "42", BranchChange.Operation.DELETE, BranchSnapshot.absent(), BranchSnapshot.absent(), null));
    }
}
