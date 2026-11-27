package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doCallRealMethod;
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

    /** The event must expose the saved visibility and search paths for both recursive and parent-only edits. */
    @ParameterizedTest
    @CsvSource({"true,true", "false,true", "true,false", "false,false"})
    void listenerReadsSavedSubtree(boolean internal, boolean recursive) {
        String name = "group-event-" + UUID.randomUUID();
        String childName = name + "-child";
        GroupsRestController controller = createController();
        Object originalGroups = Constants.getServletContext().getAttribute(Constants.A_GROUPS_DB);

        try (var cluster = mockStatic(ClusterDB.class); var audit = mockStatic(Adminlog.class);
             var history = mockStatic(GroupPublisher.class); var docs = mockStatic(DocDB.class, RETURNS_MOCKS);
             var events = mockStatic(WebjetEventPublisher.class)) {
            int parentId = insertGroup(name, 0, !internal);
            int childId = insertGroup(childName, parentId, !internal);
            String parentPath = "/" + name;
            String childPath = parentPath + "/" + childName;
            insertPage(name, parentId, internal ? parentPath : null);
            insertPage(childName, childId, internal ? childPath : null);
            docs.when(() -> DocDB.updateFileNameField(parentId)).thenCallRealMethod();
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
                    assertEquals(internal ? null : parentPath, query.forString("SELECT file_name FROM documents WHERE group_id=?", parentId));
                    assertEquals(expectedChild ? null : childPath, query.forString("SELECT file_name FROM documents WHERE group_id=?", childId));
                } else {
                    assertEquals(!internal, query.forBoolean("SELECT internal FROM groups WHERE group_id=?", parentId));
                }
                return null;
            });
            controller.editItem(parent, parentId);
            assertEquals(List.of(WebjetEventType.ON_START, WebjetEventType.AFTER_SAVE), received);
            docs.verify(() -> DocDB.updateFileNameField(parentId));
        } finally {
            query.execute("DELETE FROM documents WHERE title IN (?, ?)", name, childName);
            query.execute("DELETE FROM groups WHERE group_name IN (?, ?)", name, childName);
            Constants.getServletContext().setAttribute(Constants.A_GROUPS_DB, originalGroups);
        }
    }

    /** A recursive template edit must refresh cached page metadata before publishing the parent's event. */
    @Test
    void listenerReadsNewPageTemplateInSubgroup() {
        String name = "group-template-" + UUID.randomUUID();
        String childName = name + "-child";
        int oldTemplateId = 10;
        int newTemplateId = 20;
        GroupsRestController controller = createController();
        Object originalGroups = Constants.getServletContext().getAttribute(Constants.A_GROUPS_DB);
        Object originalDocs = Constants.getServletContext().getAttribute(Constants.A_DOC_DB);

        try (var cluster = mockStatic(ClusterDB.class); var audit = mockStatic(Adminlog.class);
             var history = mockStatic(GroupPublisher.class); var events = mockStatic(WebjetEventPublisher.class)) {
            int parentId = insertGroup(name, 0, false);
            int childId = insertGroup(childName, parentId, false);
            insertPage(childName, childId, null);
            int pageId = query.forInt("SELECT doc_id FROM documents WHERE group_id=?", childId);
            query.execute("UPDATE documents SET temp_id=? WHERE doc_id=?", oldTemplateId, pageId);
            assertEquals(oldTemplateId, DocDB.getInstance(true).getBasicDocDetails(pageId, false).getTempId());
            GroupsDB.getInstance(true);
            GroupDetails parent = controller.getOneItem(parentId);
            parent.setTempId(newTemplateId);
            parent.getEditorFields().setForceTemplateToSubgroupsAndPages(true);

            List<WebjetEventType> received = new ArrayList<>();
            events.when(() -> WebjetEventPublisher.publishEvent(any(WebjetEvent.class))).thenAnswer(call -> {
                WebjetEvent<GroupDetails> event = call.getArgument(0);
                received.add(event.getEventType());
                if (event.getEventType() == WebjetEventType.AFTER_SAVE) {
                    assertEquals(parentId, event.getSource().getGroupId());
                    assertEquals(newTemplateId, query.forInt("SELECT temp_id FROM documents WHERE doc_id=?", pageId));
                    assertEquals(newTemplateId, DocDB.getInstance().getBasicDocDetails(pageId, false).getTempId());
                }
                return null;
            });
            controller.editItem(parent, parentId);
            assertEquals(List.of(WebjetEventType.ON_START, WebjetEventType.AFTER_SAVE), received);
        } finally {
            query.execute("DELETE FROM documents WHERE title=?", childName);
            query.execute("DELETE FROM groups WHERE group_name IN (?, ?)", name, childName);
            Constants.getServletContext().setAttribute(Constants.A_DOC_DB, originalDocs);
            Constants.getServletContext().setAttribute(Constants.A_GROUPS_DB, originalGroups);
        }
    }

    /** A failed bulk write must reach the controller before it can publish AFTER_SAVE. */
    @Test
    void propagatesSubgroupUpdateFailure() throws SQLException {
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getSubgroupsIds(513)).thenReturn("513,515");
        doCallRealMethod().when(groups).setAttributeToSubgroups(513, "internal", false);
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

    private GroupsRestController createController() {
        GroupsRestController controller = new GroupsRestController(null, null, null, mock(ApproveService.class), null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(Constants.USER_KEY, mock(Identity.class));
        ReflectionTestUtils.setField(controller, "request", request);
        return controller;
    }

    private int insertGroup(String name, int parentId, boolean internal) {
        query.execute("INSERT INTO groups (group_name, parent_group_id, internal) VALUES (?, ?, ?)", name, parentId, internal);
        return query.forInt("SELECT group_id FROM groups WHERE group_name=?", name);
    }

    private void insertPage(String title, int groupId, String fileName) {
        query.execute("INSERT INTO documents (title, data, data_asc, navbar, group_id, file_name) VALUES (?, ?, ?, ?, ?, ?)",
            title, title, title, title, groupId, fileName);
    }
}
