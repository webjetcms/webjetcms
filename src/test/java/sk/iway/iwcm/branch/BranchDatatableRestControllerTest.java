package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRequest;

/** Verifies standard numeric domain isolation and the metadata-only editor boundary. */
class BranchDatatableRestControllerTest {
    private final BranchRepository repository = mock(BranchRepository.class);
    private final BranchService branches = mock(BranchService.class);
    private final BranchAccess access = mock(BranchAccess.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private BranchDatatableRestController controller;

    @BeforeEach
    void setUp() {
        controller = new BranchDatatableRestController(repository, branches, access);
        controller.setRequest(request);
        ReflectionTestUtils.setField(controller, "checkDomainId", true);
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(access.requireUser(request)).thenReturn(user);
    }

    @Test
    void defaultsAndDirectReadsUseTheStandardDomainScope() {
        assertEquals(1, controller.getOneItem(-1).getDomainId());
        when(repository.existsById(42L)).thenReturn(true);
        when(repository.findFirstByIdAndDomainId(42L, 1)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> controller.getOneItem(42)).getStatusCode());
        verify(repository).findFirstByIdAndDomainId(42L, 1);
        verify(repository, never()).findById(any());
        verifyNoInteractions(branches);
    }

    @Test
    void editOnlyPassesPresentationMetadataToTheService() {
        BranchEntity input = new BranchEntity();
        input.setDomainId(1);
        input.setName("Campaign"); input.setColor("lavender"); input.setDescription("Note");
        input.setState("MERGED"); input.setRevision(999L);
        when(repository.existsById(42L)).thenReturn(true);
        when(repository.findFirstByIdAndDomainId(42L, 1)).thenReturn(Optional.of(input));
        BranchEntity saved = new BranchEntity(); saved.setId(42L); saved.setDomainId(1);
        when(branches.updateMetadata(42, 1, "Campaign", "Note", "lavender", 7)).thenReturn(saved);
        try (var audit = mockStatic(Adminlog.class); var domains = mockStatic(BranchDomains.class)) {
            domains.when(() -> BranchDomains.name(1)).thenReturn("a.example.sk");
            assertSame(saved, controller.editItem(input, 42));
        }
        verify(access).requireCsrf(request);
        verify(repository, never()).save(any());
    }

    @Test
    void constructorRecognizesDomainIdRepository() {
        try (var constants = mockStatic(Constants.class); var cloud = mockStatic(CloudToolsForCore.class)) {
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(7);
            controller = new BranchDatatableRestController(repository, branches, access);
            controller.setRequest(request);
            assertEquals(true, ReflectionTestUtils.getField(controller, "checkDomainId"));
            assertEquals(7, controller.getOneItem(-1).getDomainId());
        }
    }

    @Test
    void standardEditorAndDirectWritesRejectForgedDomainId() {
        controller = spy(controller);
        Prop prop = mock(Prop.class); doReturn(prop).when(controller).getProp();
        when(prop.getText("datatables.error.domainId")).thenReturn("Domain mismatch");
        BranchEntity input = new BranchEntity(); input.setDomainId(2);
        var edit = new DatatableRequest<Long, BranchEntity>(); edit.setAction("edit"); edit.setData(Map.of(42L, input));
        assertEquals("Domain mismatch", assertThrows(RuntimeException.class,
            () -> controller.handleEditor(request, edit)).getMessage());
        assertEquals("Domain mismatch", assertThrows(RuntimeException.class,
            () -> controller.editItem(input, 42)).getMessage());
        verifyNoInteractions(repository, branches);
    }

    @Test
    void unsupportedOperationsCannotWrite() {
        BranchEntity input = new BranchEntity();
        assertThrows(ResponseStatusException.class, () -> controller.beforeDelete(input));
        assertThrows(ResponseStatusException.class, () -> controller.beforeDuplicate(input));
        verifyNoInteractions(branches, repository);
    }
}
