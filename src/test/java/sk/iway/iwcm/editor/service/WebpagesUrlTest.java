package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;

/** Covers folder URL propagation without entering the page publication workflow. */
class WebpagesUrlTest {
    /** Hidden pages and pending child versions inherit URLs, while wildcard and manual URL rules are preserved. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void regeneratesHiddenPagesAndPendingVersions(boolean onlyInherited) {
        DocDB documents = mock(DocDB.class);
        GroupsDB groups = mock(GroupsDB.class);
        DocHistoryRepository history = mock(DocHistoryRepository.class);
        DocDetails hidden = page(11, "/old/hidden.html");
        hidden.setAvailable(false);
        DocDetails inherited = page(12, "/old/custom.html");
        inherited.setUrlInheritGroup(true);
        inherited.setGenerateUrlFromTitle(false);
        inherited.setEditorVirtualPath("custom.html");
        DocDetails manual = page(13, "/manual.html");
        manual.setGenerateUrlFromTitle(false);
        DocDetails wildcard = page(14, "/old/*");
        when(documents.getDocByGroup(10, DocDB.ORDER_TITLE, true, -1, -1, true, false))
            .thenReturn(List.of(hidden, inherited, manual, wildcard));
        GroupDetails child = new GroupDetails();
        child.setGroupId(20);
        when(groups.getGroups(10)).thenReturn(List.of(child));
        DocHistory scheduled = pending(101L, 21, "/old/scheduled.html");
        DocHistory anotherSchedule = pending(102L, 21, "/old/later.html");
        anotherSchedule.setUrlInheritGroup(true);
        anotherSchedule.setEditorVirtualPath("later.html");
        DocHistory manualHistory = pending(103L, 22, "/manual-pending.html");
        manualHistory.setGenerateUrlFromTitle(false);
        DocHistory wildcardHistory = pending(104L, 23, "/old/*");
        when(history.findPendingByGroupId(20)).thenReturn(List.of(scheduled, anotherSchedule, manualHistory, wildcardHistory));
        MockHttpServletRequest request = new MockHttpServletRequest();

        try (var docAccess = mockStatic(DocDB.class); var groupAccess = mockStatic(GroupsDB.class);
             var tools = mockStatic(Tools.class, CALLS_REAL_METHODS); var editor = mockStatic(EditorDB.class);
             var constants = mockStatic(Constants.class, CALLS_REAL_METHODS); var audit = mockStatic(Adminlog.class);
             var queries = mockConstruction(SimpleQuery.class)) {
            docAccess.when(DocDB::getInstance).thenReturn(documents);
            docAccess.when(() -> DocDB.getRootGroupL(anyInt(), isNull(), eq(-1))).thenReturn(new int[] { 10, 20, 30 });
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(history);
            constants.when(() -> Constants.getInt("linkType")).thenReturn(Constants.LINK_TYPE_HTML);
            editor.when(() -> EditorDB.setVirtualPath(any(EditorForm.class))).thenAnswer(call -> {
                EditorForm form = call.getArgument(0);
                String file = form.getVirtualPath().isEmpty() ? form.getTitle() + ".html" : form.getVirtualPath();
                form.setVirtualPath("/new/" + file);
                return null;
            });

            WebpagesService.regenerateUrl(10, new Identity(), request, onlyInherited);

            verify(queries.constructed().get(0)).execute("UPDATE documents SET virtual_path=?, file_name=?, root_group_l1=?, root_group_l2=?, root_group_l3=?, sync_status=1 WHERE doc_id=?", "/new/autotest-11.html", null, 10, 20, 30, 11);
            verify(queries.constructed().get(1)).execute("UPDATE documents SET virtual_path=?, file_name=?, root_group_l1=?, root_group_l2=?, root_group_l3=?, sync_status=1 WHERE doc_id=?", "/new/custom.html", null, 10, 20, 30, 12);
            assertEquals(onlyInherited ? 2 : 3, queries.constructed().size());
            assertFalse(hidden.isAvailable());
            editor.verify(() -> EditorDB.saveEditorForm(any(), any()), never());
            editor.verify(() -> EditorDB.fixRenamedVirtualPath(request, 11, "/new/autotest-11.html"));
            verify(history).updateVirtualPath(101L, "/new/autotest-21.html");
            verify(history).updateVirtualPath(102L, "/new/later.html");
            verify(history, times(onlyInherited ? 0 : 1)).updateVirtualPath(103L, "/new/autotest-22.html");
            verify(history, never()).updateVirtualPath(eq(104L), anyString());
            verify(documents).updateInternalCaches(11);
            verify(documents).updateInternalCaches(12);
            verify(documents).updateInternalCaches(21);
            assertTrue(scheduled.getPublicable());
            assertEquals("7,9", scheduled.getAwaitingApprove());
        }
    }

    private static DocDetails page(int id, String path) {
        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setGroupId(10);
        page.setTitle("autotest-" + id);
        page.setNavbar(page.getTitle());
        page.setVirtualPath(path);
        return page;
    }

    private static DocHistory pending(long id, int docId, String path) {
        DocHistory history = new DocHistory();
        history.setId(id);
        history.setDocId(docId);
        history.setGroupId(20);
        history.setTitle("autotest-" + docId);
        history.setNavbar(history.getTitle());
        history.setVirtualPath(path);
        history.setPublicable(true);
        history.setAwaitingApprove("7,9");
        return history;
    }
}
