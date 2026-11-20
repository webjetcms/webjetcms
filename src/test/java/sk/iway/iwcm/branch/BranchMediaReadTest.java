package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaGroupRepository;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.components.media.MediaRestController;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.model.Media;

/** Guards the query scope of branch media reads independently of the domain's media count. */
class BranchMediaReadTest {
    /** Loading and rechecking one editor record must not materialize every medium in the domain. */
    @Test
    void repeatedDetailReadsDoNotLoadTheDomainMediaList() {
        try (Fixture f = new Fixture()) {
            Media displayed=BranchMedia.one(7);
            displayed.setMediaTitleSk("Unsaved edit"); displayed.setMediaFkId(999);
            assertEquals("Attachment", BranchMedia.one(7).getMediaTitleSk());
            assertEquals(42, BranchMedia.one(7).getMediaFkId());
            verify(f.repository, never()).findAllByMediaFkTableNameAndDomainId(anyString(), anyInt());
            verify(f.repository, times(1)).findFirstByIdAndDomainId(7L, 1);
            verify(f.groups, atMostOnce()).findAllByOrderByIdAsc();
        }
    }

    /** The real GET lifecycle performs its permission check without querying the record again. */
    @Test
    void editorGetReusesDetailDuringPermissionCheck() {
        try (Fixture f = new Fixture();
             var constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
             var users = mockStatic(UsersDB.class);
             var cloud = mockStatic(CloudToolsForCore.class)) {
            constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            Identity user = mock(Identity.class);
            when(user.isAdmin()).thenReturn(true);
            when(user.isEnabledItem(BranchAccess.PERMISSION)).thenReturn(true);
            when(user.getUserId()).thenReturn(5);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("docId", "42");
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            f.contexts.when(() -> BranchContexts.current(request)).thenReturn(f.context);
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            MediaRestController controller = spy(new MediaRestController(f.repository, f.groups));
            controller.setRequest(request);
            // Custom-field definitions are independent of the media read and permission lifecycle.
            doAnswer(call -> call.getArgument(0)).when(controller).processFromEntity(any(), eq(ProcessItemAction.GETONE), eq(1));

            assertEquals(7, controller.getOne(7).getMediaId());

            verify(f.repository, times(1)).findFirstByIdAndDomainId(7L, 1);
            verify(f.repository, never()).findAllByMediaFkTableNameAndDomainId(anyString(), anyInt());
        }
    }

    /** Embedded page media are filtered in SQL, and a page's metadata is read only once. */
    @Test
    void ownerListDoesNotReadUnrelatedPages() {
        try (Fixture f = new Fixture()) {
            Media second = media(8, 42);
            when(f.repository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(42, "documents", 1)).thenReturn(List.of(f.stored, second));
            assertEquals(List.of(7, 8), BranchMedia.list(42, null).stream().map(Media::getMediaId).toList());
            verify(f.repository, never()).findAllByMediaFkTableNameAndDomainId(anyString(), anyInt());
            verify(f.view, times(1)).document(42, true);
        }
    }

    /** WORK-only changes and deletion remain visible, while a new view discards cached records. */
    @Test
    void overlaysWorkAndInvalidatesDetailsWithTheView() {
        try (Fixture f = new Fixture()) {
            Map<String,Object> work = new LinkedHashMap<>(BranchMediaMapper.initialValues(9, 42, 1));
            work.put("operation", "UPSERT"); work.put("media_title_sk", "WORK attachment");
            work.put("group_ids_snapshot", List.of());
            doReturn(List.of(work)).when(f.view).workRows(BranchType.MEDIA);
            doReturn(true).when(f.view).selectWork(eq(BranchType.MEDIA), same(work), any());
            assertEquals("WORK attachment", BranchMedia.one(9).getMediaTitleSk());

            BranchView next = mock(BranchView.class, CALLS_REAL_METHODS);
            doReturn(f.context).when(next).context();
            work = new LinkedHashMap<>(work); work.put("operation", "DELETE");
            doReturn(List.of(work)).when(next).workRows(BranchType.MEDIA);
            doReturn(true).when(next).selectWork(eq(BranchType.MEDIA), same(work), any());
            f.views.when(BranchView::current).thenReturn(next);
            assertNull(BranchMedia.one(9));
            assertNull(BranchMedia.one(9));
            verify(f.repository, times(2)).findFirstByIdAndDomainId(9L, 1);
        }
    }

    /** Temporary/planned records and media whose effective owner disappeared are not exposed. */
    @Test
    void rejectsOtherMediaSetsAndMissingOwners() {
        try (Fixture f = new Fixture()) {
            f.stored.setMediaFkTableName("planned_documents");
            assertNull(BranchMedia.one(7));
            Media orphan = media(8, 999);
            when(f.repository.findFirstByIdAndDomainId(8L, 1)).thenReturn(Optional.of(orphan));
            assertNull(BranchMedia.one(8));
            verifyNoInteractions(f.groups);
        }
    }

    private static Media media(int id, int owner) {
        Media stored = new Media();
        stored.setMediaId(id); stored.setMediaFkId(owner); stored.setDomainId(1);
        stored.setMediaFkTableName("documents"); stored.setMediaTitleSk("Attachment");
        stored.setMediaSortOrder(10); stored.setLastUpdate(new Date(1000)); stored.setGroups(List.of());
        return stored;
    }

    private static final class Fixture implements AutoCloseable {
        final BranchView view = mock(BranchView.class, CALLS_REAL_METHODS);
        final MediaRepository repository = mock(MediaRepository.class);
        final MediaGroupRepository groups = mock(MediaGroupRepository.class);
        final BranchContext context = new BranchContext(1, "media.example", 5, BranchContext.Mode.ADMIN);
        final Media stored = media(7, 42);
        final MockedStatic<BranchView> views = mockStatic(BranchView.class);
        final MockedStatic<BranchContexts> contexts = mockStatic(BranchContexts.class);
        final MockedStatic<BranchDomains> domains = mockStatic(BranchDomains.class);
        final MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);

        Fixture() {
            DocDetails page = new DocDetails(); page.setDocId(42); page.setDateCreated(2000);
            doReturn(context).when(view).context();
            doReturn(List.of()).when(view).workRows(BranchType.MEDIA);
            doReturn(null).when(view).document(anyInt(), eq(true));
            doReturn(page).when(view).document(42, true);
            when(repository.findAllByMediaFkTableNameAndDomainId("documents", 1)).thenReturn(List.of(stored));
            when(repository.findFirstByIdAndDomainId(7L, 1)).thenReturn(Optional.of(stored));
            views.when(BranchView::current).thenReturn(view);
            contexts.when(BranchContexts::current).thenReturn(context);
            domains.when(() -> BranchDomains.id("media.example")).thenReturn(1);
            tools.when(() -> Tools.getSpringBean("mediaRepository", MediaRepository.class)).thenReturn(repository);
            tools.when(() -> Tools.getSpringBean("mediaGroupRepository", MediaGroupRepository.class)).thenReturn(groups);
        }

        @Override public void close() { tools.close(); domains.close(); contexts.close(); views.close(); }
    }
}
