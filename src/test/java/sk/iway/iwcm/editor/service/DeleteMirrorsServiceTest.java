package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.components.structuremirroring.DocMirroringServiceV9;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.components.structuremirroring.MirroringService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.DeleteMirrorsResult;

/** Covers the core page and folder deletion preview without database access. */
class DeleteMirrorsServiceTest {
    private static final String DOMAIN = "autotest.example";
    private static final int SYNC_ID = 500;

    private final Identity user = mock(Identity.class);
    private final EditorService editor = mock(EditorService.class);
    private final DocDB documents = mock(DocDB.class);
    private final GroupsDB groups = mock(GroupsDB.class);
    private final DeleteMirrorsService service = new DeleteMirrorsService(editor);

    private MockedStatic<DocDB> documentAccess;
    private MockedStatic<GroupsDB> groupAccess;
    private MockedStatic<DocMirroringServiceV9> pageMirrors;
    private MockedStatic<GroupMirroringServiceV9> folderMirrors;
    private MockedStatic<MirroringService> mirroring;
    private MockedStatic<InitServlet> installation;

    @BeforeEach
    void setUp() {
        documentAccess = mockStatic(DocDB.class);
        groupAccess = mockStatic(GroupsDB.class);
        pageMirrors = mockStatic(DocMirroringServiceV9.class);
        folderMirrors = mockStatic(GroupMirroringServiceV9.class);
        mirroring = mockStatic(MirroringService.class);
        installation = mockStatic(InitServlet.class);
        documentAccess.when(DocDB::getInstance).thenReturn(documents);
        groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
    }

    @AfterEach
    void tearDown() {
        installation.close();
        mirroring.close();
        folderMirrors.close();
        pageMirrors.close();
        groupAccess.close();
        documentAccess.close();
    }

    /** Returns unique additional mirrors and warns about inaccessible targets without exposing their paths. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void previewsAdditionalMirrors(boolean folders) {
        addItem(10, true);
        addItem(11, true);
        addItem(12, true);
        addItem(13, false);
        setMirrors(10, 11, 12, 12, 13);
        setMirrors(11, 10, 12, 13);

        DeleteMirrorsResult result = preview(folders, List.of(10, 11));

        assertEquals(List.of(12), result.getItems().stream().map(DeleteMirrorsResult.Item::getId).toList());
        DeleteMirrorsResult.Item visible = result.getItems().get(0);
        assertEquals(folders ? "/autotest-folder-12" : "/autotest-folder-12/autotest-page-12", visible.getFullPath());
        assertEquals(DOMAIN, visible.getDomainName());
        assertTrue(result.isHiddenMirrors());
    }

    /** Disabled mirroring produces an empty preview without a warning. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void ignoresDisabledMirroring(boolean folders) {
        addItem(10, true);
        addItem(11, true);
        setMirrors(10, 11);
        mirroring.when(() -> MirroringService.isEnabled(10)).thenReturn(false);

        DeleteMirrorsResult result = preview(folders, List.of(10));

        assertTrue(result.getItems().isEmpty());
        assertFalse(result.isHiddenMirrors());
    }

    /** A user cannot preview deletion of a source they cannot edit. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectsInaccessibleSource(boolean folders) {
        addItem(10, false);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> preview(folders, List.of(10)));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private DeleteMirrorsResult preview(boolean folders, List<Integer> ids) {
        return folders ? service.getGroupMirrors(ids, user) : service.getPageMirrors(ids, user);
    }

    private void addItem(int id, boolean accessible) {
        GroupDetails folder = new GroupDetails();
        folder.setGroupId(id);
        folder.setDomainName(DOMAIN);
        folder.setFullPath("/autotest-folder-" + id);
        when(groups.getGroup(id)).thenReturn(folder);
        when(groups.getDomain(id)).thenReturn(DOMAIN);
        groupAccess.when(() -> GroupsDB.isGroupEditable(user, id)).thenReturn(accessible);

        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setGroupId(id);
        page.setFullPath(folder.getFullPath() + "/autotest-page-" + id);
        when(documents.getBasicDocDetails(id, false)).thenReturn(page);
        when(editor.isPageEditable(user, page, true)).thenReturn(accessible);
        pageMirrors.when(() -> DocMirroringServiceV9.getSyncId(id)).thenReturn(SYNC_ID);
        folderMirrors.when(() -> GroupMirroringServiceV9.getSyncId(id)).thenReturn(SYNC_ID);
        mirroring.when(() -> MirroringService.isEnabled(id)).thenReturn(true);
    }

    private void setMirrors(int sourceId, Integer... ids) {
        List<DocDetails> pages = Arrays.stream(ids).map(id -> documents.getBasicDocDetails(id, false)).toList();
        List<GroupDetails> folders = Arrays.stream(ids).map(groups::getGroup).toList();
        pageMirrors.when(() -> DocMirroringServiceV9.getDocBySyncId(SYNC_ID, sourceId)).thenReturn(pages);
        folderMirrors.when(() -> GroupMirroringServiceV9.getGroupsBySyncId(SYNC_ID, sourceId)).thenReturn(folders);
    }
}
