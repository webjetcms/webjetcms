package sk.iway.iwcm.components.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.doc.PlannedChangesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaEditorFields;

/** Verifies media context and inherited write permissions without persisting test records. */
class MediaRestControllerTest {
    private final MediaRepository repository = mock(MediaRepository.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private MediaRestController controller;

    @BeforeEach
    void setUp() {
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(5);
        when(user.isEnabledItem("editor_edit_media_all")).thenReturn(true);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        controller = spy(new MediaRestController(repository, mock(MediaGroupRepository.class)));
        controller.setRequest(request);
        controller.setValidator(mock(Validator.class));
        doReturn(user).when(controller).getUser();
        doReturn(prop).when(controller).getProp();
        request.setParameter("docId", "42");
    }

    /** The inherited edit and delete paths reject IDs from another page or media set. */
    @ParameterizedTest
    @CsvSource({"documents,43", "planned_documents,42", "documents_temp,5"})
    void rejectsMismatchedStoredMediaOnEditAndDelete(String table, int owner) {
        Media stored = media();
        stored.setMediaFkTableName(table);
        stored.setMediaFkId(owner);
        when(repository.findFirstByIdAndDomainId(eq(7L), anyInt())).thenReturn(Optional.of(stored));

        try (var mappings = mockStatic(MultigroupMappingDB.class);
             var events = mockStatic(WebjetEventPublisher.class)) {
            assertThrows(ConstraintViolationException.class, () -> controller.edit(7L, media()));
            assertThrows(ConstraintViolationException.class, () -> controller.delete(7L, media()));
            verify(repository, never()).save(any());
            verify(repository, never()).deleteById(anyLong());
        }
    }

    /** Request context overrides submitted ownership, including new pages and multigroup slaves. */
    @ParameterizedTest
    @CsvSource({
        "42,false,documents,42",
        "43,false,documents,42",
        "-1,false,documents_temp,5",
        "42,true,planned_documents,42"
    })
    void derivesMediaSetAndOwnerFromPageContext(String docId, boolean planned, String table, int owner) {
        request.setParameter("docId", docId);
        request.setParameter("planned", Boolean.toString(planned));
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        when(documents.isPlannedMediaInitialized(42)).thenReturn(true);
        PlannedChangesService service = new PlannedChangesService(documents, mock(DocHistoryRepository.class), repository);

        try (var mappings = mockStatic(MultigroupMappingDB.class);
             var access = mockStatic(PlannedAccess.class);
             var services = mockStatic(PlannedChangesService.class, CALLS_REAL_METHODS)) {
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(43)).thenReturn(42);
            access.when(PlannedAccess::enabled).thenReturn(true);
            services.when(PlannedChangesService::getInstance).thenReturn(service);
            Media submitted = media();
            submitted.setMediaFkId(999);
            submitted.setMediaFkTableName("another_table");

            controller.beforeSave(submitted);

            assertEquals(owner, submitted.getMediaFkId());
            assertEquals(table, submitted.getMediaFkTableName());
        }
    }

    /** A missing page context must not silently create temporary media owned by a page ID. */
    @Test
    void rejectsCreateWithoutPageContext() {
        request.removeParameter("docId");
        Media submitted = media();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> controller.add(submitted));
        assertEquals("components.media.pageContextRequired", error.getMessage());
        verifyNoInteractions(repository);
    }

    /** All-media editing gets its target page from the tree selector without a docId parameter. */
    @Test
    void acceptsAllMediaPageSelection() {
        request.removeParameter("docId");
        request.setParameter("isCalledFromTable", "true");
        Media submitted = media();
        submitted.setMediaFkTableName("another_table");
        DocDetails selected = new DocDetails();
        selected.setDocId(43);
        submitted.getEditorFields().setDocDetails(selected);

        controller.beforeSave(submitted);

        assertEquals(43, submitted.getMediaFkId());
        assertEquals("documents", submitted.getMediaFkTableName());
    }

    /** Individual media validation still rejects cancelled plans and unsupported private references. */
    @Test
    void validatesPlannedStateAndMediaFields() {
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        PlannedChangesService service = new PlannedChangesService(documents, mock(DocHistoryRepository.class), repository);
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(PlannedAccess::enabled).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> service.validateMedia(media(), 42));
            when(documents.isPlannedMediaInitialized(42)).thenReturn(true);
            Media submitted = media();
            submitted.setMediaLink("/files/protected/planned-changes/42/files/future.pdf");
            assertDoesNotThrow(() -> service.validateMedia(submitted, 42));
            submitted.setMediaTitleSk(submitted.getMediaLink());
            assertThrows(IllegalArgumentException.class, () -> service.validateMedia(submitted, 42));
        }
    }

    private Media media() {
        Media item = new Media();
        item.setId(7L);
        item.setMediaFkId(42);
        item.setMediaFkTableName("documents");
        item.setMediaTitleSk("Media");
        item.setMediaLink("/files/current.pdf");
        item.setMediaSortOrder(10);
        MediaEditorFields fields = new MediaEditorFields();
        fields.setGroups(new Integer[0]);
        item.setEditorFields(fields);
        return item;
    }
}
