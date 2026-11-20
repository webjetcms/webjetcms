package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.branch.BranchContexts;
import sk.iway.iwcm.database.ComplexQuery;

/** Root folders are already present in the shared folder cache. */
class GroupsDBRootCacheTest {
    /** Root reads preserve ordering and detached results without another database query. */
    @Test
    void readsSortedRootCopiesFromCache() {
        GroupsDB groups = mock(GroupsDB.class);
        GroupDetails first = group(1, 0, 10, "Alpha");
        GroupDetails second = group(2, -1, 10, "Beta");
        GroupDetails last = group(3, 0, 20, "Last");
        when(groups.getGroupsAll()).thenReturn(List.of(last, second, group(4, 1, 1, "Child"), first));
        try (var db = mockStatic(GroupsDB.class, CALLS_REAL_METHODS);
             var contexts = mockStatic(BranchContexts.class);
             var queries = mockConstruction(ComplexQuery.class, (query, context) -> when(query.setSql(anyString())).thenReturn(query))) {
            db.when(GroupsDB::getInstance).thenReturn(groups);
            List<GroupDetails> roots = GroupsDB.getRootGroups();
            assertEquals(List.of(1, 2, 3), roots.stream().map(GroupDetails::getGroupId).toList());
            roots.get(0).setGroupName("Edited label");
            assertEquals("Alpha", first.getGroupName());
            roots.clear();
            assertEquals(4, groups.getGroupsAll().size());
            assertTrue(queries.constructed().isEmpty());
        }
    }

    private static GroupDetails group(int id, int parent, int priority, String name) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id); group.setParentGroupId(parent); group.setSortPriority(priority); group.setGroupName(name);
        return group;
    }
}
