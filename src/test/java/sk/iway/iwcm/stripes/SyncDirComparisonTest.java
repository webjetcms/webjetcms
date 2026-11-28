package sk.iway.iwcm.stripes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

/** Covers XML import comparison when the document cache predates the save. */
class SyncDirComparisonTest {
    @Test
    void usesTheSelectedDomainForThePreviewAndTheImport() {
        GroupDetails root = new GroupDetails();
        root.setDomainName("target.example");
        GroupDetails remote = new GroupDetails();
        GroupDetails target = new GroupDetails();
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(42)).thenReturn(root);
        when(groups.getGroupByPathAndDomain("/import", "target.example")).thenReturn(target);
        SyncDirAction action = spy(new SyncDirAction());
        doReturn("/import").when(action).getLocalPath(remote);
        try (var groupCache = mockStatic(GroupsDB.class)) {
            groupCache.when(GroupsDB::getInstance).thenReturn(groups);
            action.setLocalGroupId(42);
            assertSame(target, action.getLocalGroup("/import"));
            assertSame(target, action.getLocalGroup(remote));
        }
    }

    @Test
    void readsTheNewlyStoredPageOnTheNextComparison() throws Exception {
        GroupDetails target = new GroupDetails();
        target.setGroupId(42);
        SyncDirAction action = new SyncDirAction();
        Connection connection = mock(Connection.class);
        PreparedStatement query = mock(PreparedStatement.class);
        ResultSet before = mock(ResultSet.class);
        ResultSet after = mock(ResultSet.class);
        when(connection.prepareStatement(contains("WHERE group_id = ?"))).thenReturn(query);
        when(query.executeQuery()).thenReturn(before, after);
        when(after.next()).thenReturn(true, false);
        when(after.getInt("doc_id")).thenReturn(73);
        when(after.getString("title")).thenReturn("Second");
        when(after.getString("virtual_path")).thenReturn("import/second.html");
        when(after.getTimestamp("date_created")).thenReturn(new Timestamp(123456L));
        when(after.getString("field_a")).thenReturn("A");
        when(after.getString("field_b")).thenReturn("B");
        when(after.getString("field_c")).thenReturn("C");

        try (var pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertTrue(action.getLocalDocs(target).isEmpty());
            List<DocDetails> stored = action.getLocalDocs(target);
            verify(query, times(2)).setInt(1, target.getGroupId());
            assertEquals(1, stored.size());
            DocDetails page = stored.get(0);
            assertEquals(73, page.getDocId());
            assertEquals(123456L, page.getDateCreated());

            DocDetails remote = new DocDetails();
            remote.setTitle("Second");
            assertSame(page, action.findLocalDoc(remote, stored));
            remote.setTitle("First");
            assertNull(action.findLocalDoc(remote, stored));
            remote.setVirtualPath("/import/second.html");
            assertSame(page, action.findLocalDoc(remote, stored));
            MockHttpServletRequest request = new MockHttpServletRequest();
            ActionBeanContext context = new ActionBeanContext();
            context.setRequest(request);
            for (String mode : List.of("url", "fieldA", "fieldB", "fieldC", "none")) {
                request.setParameter("compareBy", mode);
                action.setContext(context);
                DocDetails candidate = new DocDetails();
                switch (mode) {
                    case "url" -> candidate.setVirtualPath("/import/second.html");
                    case "fieldA" -> candidate.setFieldA("A");
                    case "fieldB" -> candidate.setFieldB("B");
                    case "fieldC" -> candidate.setFieldC("C");
                    case "none" -> candidate = page;
                }
                assertEquals("none".equals(mode) ? null : page, action.findLocalDoc(candidate, stored), mode);
                assertNull(action.findLocalDoc(new DocDetails(), stored), mode);
            }
        }
    }
}
