package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupEditorField;
import sk.iway.iwcm.doc.GroupPublisher;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.approve.ApproveService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks the state an indexer can read synchronously during a folder edit. */
class GroupsAfterSaveTest extends BaseWebjetTest {

    /** Both directions of propagation and a parent-only edit must publish exactly one event pair. */
    @ParameterizedTest
    @CsvSource({"true,true", "false,true", "false,false"})
    void listenerReadsSavedSubtree(boolean internal, boolean recursive) {
        String name = "group-event-" + UUID.randomUUID();
        String childName = name + "-child";
        GroupsRestController controller = new GroupsRestController(null, null, null, mock(ApproveService.class), null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(Constants.USER_KEY, mock(Identity.class));
        ReflectionTestUtils.setField(controller, "request", request);
        Object originalGroups = Constants.getServletContext().getAttribute(Constants.A_GROUPS_DB);

        try (var cluster = mockStatic(ClusterDB.class); var audit = mockStatic(Adminlog.class);
             var history = mockStatic(GroupPublisher.class); var docs = mockStatic(DocDB.class, RETURNS_MOCKS);
             var events = mockStatic(WebjetEventPublisher.class)) {
            int parentId = insertGroup(name, 0, !internal);
            int childId = insertGroup(childName, parentId, !internal);
            GroupsDB.getInstance(true);
            GroupDetails parent = controller.getOneItem(parentId);
            GroupEditorField fields = parent.getEditorFields();
            fields.setForceInternalToSubgroups(recursive);
            parent.setInternal(internal);

            List<WebjetEventType> received = new ArrayList<>();
            events.when(() -> WebjetEventPublisher.publishEvent(any(WebjetEvent.class))).thenAnswer(call -> {
                WebjetEvent<GroupDetails> event = call.getArgument(0);
                received.add(event.getEventType());
                assertEquals(parentId, event.getSource().getGroupId());
                if (event.getEventType() == WebjetEventType.AFTER_SAVE) {
                    boolean expectedChild = recursive ? internal : !internal;
                    assertSame(fields, event.getSource().getEditorFields());
                    assertEquals(internal, event.getSource().isInternal());
                    assertEquals(internal, GroupsDB.getInstance().getGroup(parentId).isInternal());
                    assertEquals(expectedChild, GroupsDB.getInstance().getGroup(childId).isInternal());
                    assertEquals(expectedChild, query.forBoolean("SELECT internal FROM groups WHERE group_id=?", childId));
                } else {
                    assertEquals(!internal, query.forBoolean("SELECT internal FROM groups WHERE group_id=?", parentId));
                }
                return null;
            });
            controller.editItem(parent, parentId);
            assertEquals(List.of(WebjetEventType.ON_START, WebjetEventType.AFTER_SAVE), received);
        } finally {
            query.execute("DELETE FROM groups WHERE group_name IN (?, ?)", name, childName);
            Constants.getServletContext().setAttribute(Constants.A_GROUPS_DB, originalGroups);
        }
    }

    /** A failed bulk write must reach the controller before it can publish AFTER_SAVE. */
    @Test
    void propagatesSubgroupUpdateFailure() throws SQLException {
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getSubgroupsIds(513)).thenReturn("513,515");
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        SQLException failure = new SQLException("Bulk update failed");
        when(statement.execute()).thenThrow(failure);
        try (var pool = mockStatic(DBPool.class); var cache = mockStatic(GroupsDB.class); var audit = mockStatic(Adminlog.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            cache.when(GroupsDB::getInstance).thenReturn(groups);
            var error = assertThrows(IllegalStateException.class, () -> new WebpagesService().setAttributeToSubgroups(513, "internal", false));
            assertSame(failure, error.getCause());
            cache.verify(() -> GroupsDB.getInstance(true));
        }
    }

    private int insertGroup(String name, int parentId, boolean internal) {
        query.execute("INSERT INTO groups (group_name, parent_group_id, internal) VALUES (?, ?, ?)", name, parentId, internal);
        return query.forInt("SELECT group_id FROM groups WHERE group_name=?", name);
    }
}
