package sk.iway.iwcm.components.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/** Planned image writes use folder rights while reads still require access to the page. */
class GalleryPlannedPermissionsTest {
    private static final String FOLDER = "/files/protected/planned-changes/42/images/products/";
    private final GalleryRepository repository = mock(GalleryRepository.class);
    private final Identity user = mock(Identity.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private GalleryRestController controller;

    @BeforeEach
    void setUp() {
        when(user.isAdmin()).thenReturn(true);
        when(user.isFolderWritable("/images/products/")).thenReturn(true);
        controller = spy(new GalleryRestController(repository, mock(GalleryDimensionRepository.class), request));
        controller.setRequest(request);
        doReturn(user).when(controller).getUser();
        doReturn(false).when(controller).isExporting();
    }

    /** Write requests can load their source internally without granting public read access. */
    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    void allowsWritesAndInternalLoadsWithoutPagePermission(String method) throws Exception {
        request.setMethod(method);
        request.setParameter("isImageEditor", "true");
        request.setParameter("dir", FOLDER);
        request.setParameter("name", "photo.jpg");
        GalleryEntity image = image(FOLDER);
        image.setEditorFields(new GalleryEditorFields());
        when(repository.findByImagePathAndImageNameAndDomainId(eq(FOLDER), eq("photo.jpg"), anyInt())).thenReturn(image);
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.canWrite(anyString(), anyString(), eq(user))).thenCallRealMethod();
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));
            assertSame(image, controller.getOneItem(-1));
            assertTrue(controller.checkItemPerms(image, -1L));
            access.verify(() -> PlannedAccess.canAccess(anyString(), eq(user)), never());
        }
    }

    /** GET, HEAD and export requests must not inherit the more permissive write policy. */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST"})
    void deniesReadingWithoutPagePermission(String method) {
        request.setMethod(method);
        doReturn("POST".equals(method)).when(controller).isExporting();
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.canWrite(anyString(), anyString(), eq(user))).thenReturn(true);
            assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(FOLDER), -1L));
        }
    }

    /** A move must be authorized for both the stored source and the submitted destination. */
    @Test
    void checksBothFoldersWhenMovingImages() throws Exception {
        request.setMethod("POST");
        String destination = "/files/protected/planned-changes/42/images/other/";
        when(repository.findById(7L)).thenReturn(Optional.of(image(FOLDER)));
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.canWrite(anyString(), anyString(), eq(user))).thenCallRealMethod();
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));
            assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(destination), 7L));
            when(user.isFolderWritable("/images/other/")).thenReturn(true);
            assertTrue(controller.checkItemPerms(image(destination), 7L));
            when(user.isFolderWritable("/images/products/")).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(destination), 7L));
        }
    }

    /** Area-of-interest saves use the same folder policy for ordinary and planned images. */
    @ParameterizedTest
    @ValueSource(strings = {FOLDER, "/images/products/"})
    void savesAreaOfInterestOnlyInWritableFolders(String folder) throws Exception {
        ImageEditorRestController imageEditor = new ImageEditorRestController(repository);
        GalleryEntity image = image(folder);
        image.setSelectedWidth(120);
        try (var access = mockStatic(PlannedAccess.class); var users = mockStatic(UsersDB.class);
             var tools = mockStatic(Tools.class); var files = mockConstruction(IwcmFile.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            access.when(() -> PlannedAccess.canWrite(anyString(), anyString(), eq(user))).thenCallRealMethod();
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));
            assertEquals(200, imageEditor.saveImageAreaOfInterest(image, request).getStatusCode().value());
            verify(repository).save(argThat(saved -> Integer.valueOf(120).equals(saved.getSelectedWidth()) && folder.equals(saved.getImagePath())));
            clearInvocations(repository);
            when(user.isFolderWritable("/images/products/")).thenReturn(false);
            assertEquals(404, imageEditor.saveImageAreaOfInterest(image, request).getStatusCode().value());
            verifyNoInteractions(repository);
        }
    }

    private GalleryEntity image(String folder) {
        GalleryEntity image = new GalleryEntity();
        image.setImagePath(folder);
        image.setImageName("photo.jpg");
        return image;
    }
}
