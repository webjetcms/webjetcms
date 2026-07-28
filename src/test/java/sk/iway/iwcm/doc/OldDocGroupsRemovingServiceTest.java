package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void allCountEqualsSumOfDocumentAndGroupCounts() {
        Date createdFrom = new Date(0);
        Date createdTo = new Date();

        int documentCount = OldDocGroupsRemovingService.getCountOfDocAndGroups(createdFrom, createdTo, ActionType.DOCS);
        int groupCount = OldDocGroupsRemovingService.getCountOfDocAndGroups(createdFrom, createdTo, ActionType.GROUPS);
        int allCount = OldDocGroupsRemovingService.getCountOfDocAndGroups(createdFrom, createdTo, ActionType.ALL);

        assertEquals(documentCount + groupCount, allCount);
    }
}
