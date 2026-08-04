package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.OldDocGroupsRemovingService.ActionType;
import sk.iway.iwcm.test.BaseWebjetTest;

class OldDocGroupsRemovingServiceTest extends BaseWebjetTest {

    @BeforeAll
    static void initializeDatabase() {
        DBPool.getInstance();
        DocDB.getInstance();
        GroupsDB.getInstance();
    }

    @Test
    void defaultCreatedRangeUsesEpochToRetentionCutoff() {
        Date fixedNow = new Date(1_700_000_000_000L);
        int retentionDays = 30;

        Date[] createdRange = OldDocGroupsRemovingService.getDefaultCreatedRange(fixedNow, retentionDays);

        Calendar cal = Calendar.getInstance();
        cal.setTime(fixedNow);
        cal.add(Calendar.DAY_OF_YEAR, -retentionDays);

        assertEquals(0L, createdRange[0].getTime());
        assertEquals(cal.getTimeInMillis(), createdRange[1].getTime());
    }

    @Test
    void timestampBoundaryAtCutoffIsIncludedAndNewerIsExcluded() {
        Date fixedNow = new Date(1_700_000_000_000L);
        int retentionDays = 30;

        Date[] createdRange = OldDocGroupsRemovingService.getDefaultCreatedRange(fixedNow, retentionDays);
        long createdFrom = createdRange[0].getTime();
        long cutoff = createdRange[1].getTime();

        assertTrue(OldDocGroupsRemovingService.isTimestampInCreatedRange(cutoff, createdFrom, cutoff));
        assertTrue(OldDocGroupsRemovingService.isTimestampInCreatedRange(cutoff - 1, createdFrom, cutoff));
        assertFalse(OldDocGroupsRemovingService.isTimestampInCreatedRange(cutoff + 1, createdFrom, cutoff));
        assertFalse(OldDocGroupsRemovingService.isTimestampInCreatedRange(0, createdFrom, cutoff));
    }

    @Test
    void rangeKeepsOlderAndExcludesRecentTimestamps() {
        Date fixedNow = new Date(1_700_000_000_000L);
        int retentionDays = 186;

        Date[] createdRange = OldDocGroupsRemovingService.getDefaultCreatedRange(fixedNow, retentionDays);
        long createdFrom = createdRange[0].getTime();
        long cutoff = createdRange[1].getTime();

        Calendar older = Calendar.getInstance();
        older.setTime(fixedNow);
        older.add(Calendar.DAY_OF_YEAR, -(retentionDays + 1));

        Calendar recent = Calendar.getInstance();
        recent.setTime(fixedNow);
        recent.add(Calendar.DAY_OF_YEAR, -1);

        assertTrue(OldDocGroupsRemovingService.isTimestampInCreatedRange(older.getTimeInMillis(), createdFrom, cutoff));
        assertFalse(OldDocGroupsRemovingService.isTimestampInCreatedRange(recent.getTimeInMillis(), createdFrom, cutoff));
    }

    @Test
    void invalidDateRangeReturnsZero() {
        Date createdFrom = new Date(2_000);
        Date createdTo = new Date(1_000);

        int count = OldDocGroupsRemovingService.getCountOfDocAndGroups(createdFrom, createdTo, ActionType.ALL);

        assertEquals(0, count);
    }
}
