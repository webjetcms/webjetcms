package sk.iway.iwcm.components.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/** Planned image operations use the ordinary target folder permissions. */
class GalleryPlannedPermissionsTest {
    private static final String FOLDER = "/files/protected/planned-changes/42/images/products/";
    private final GalleryRepository repository = mock(GalleryRepository.class);
    private final Identity user = spy(new Identity());
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private GalleryRestController controller;

    @BeforeEach
    void setUp() {
        user.setAdmin(true);
        user.setWritableFolders("/images/products/");
        controller = spy(new GalleryRestController(repository, mock(GalleryDimensionRepository.class), request));
        controller.setRequest(request);
        doReturn(user).when(controller).getUser();
    }

    /** Write requests can load their source without requiring page edit permission. */
    @ParameterizedTest
    @ValueSource(strings = {"POST", "DELETE"})
    void allowsWritesAndInternalLoadsWithoutPagePermission(String method) {
        user.setDisabledItemsTable(java.util.Map.of("pageSave", "pageSave"));
        doReturn("/images/products/").when(user).getWritableFolders();
        assertFalse(user.isEnabledItem("pageSave"));
        request.setMethod(method);
        request.setParameter("isImageEditor", "true");
        request.setParameter("dir", FOLDER);
        request.setParameter("name", "photo.jpg");
        GalleryEntity image = image(FOLDER);
        image.setEditorFields(new GalleryEditorFields());
        when(repository.findByImagePathAndImageNameAndDomainId(eq(FOLDER), eq("photo.jpg"), anyInt())).thenReturn(image);
        assertSame(image, controller.getOneItem(-1));
        assertTrue(controller.checkItemPerms(image, -1L));
    }

    /** Reads and writes apply the same folder permissions. */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST"})
    void checksFolderPermissionsForReadsAndWrites(String method) {
        request.setMethod(method);
        assertTrue(controller.checkItemPerms(image(FOLDER), -1L));
        user.setWritableFolders("/images/other/");
        assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(FOLDER), -1L));
    }

    /** A move must be authorized for both the stored source and the submitted destination. */
    @Test
    void checksBothFoldersWhenMovingImages() {
        request.setMethod("POST");
        String destination = "/files/protected/planned-changes/42/images/other/";
        when(repository.findById(7L)).thenReturn(Optional.of(image(FOLDER)));
        assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(destination), 7L));
        user.setWritableFolders("/images/products/\n/images/other/");
        assertTrue(controller.checkItemPerms(image(destination), 7L));
        user.setWritableFolders("/images/other/");
        assertThrows(IllegalArgumentException.class, () -> controller.checkItemPerms(image(destination), 7L));
    }

    /** Area-of-interest saves use the same folder policy for ordinary and planned images. */
    @ParameterizedTest
    @ValueSource(strings = {FOLDER, "/images/products/"})
    void savesAreaOfInterestOnlyInWritableFolders(String folder) throws Exception {
        ImageEditorRestController imageEditor = new ImageEditorRestController(repository);
        GalleryEntity image = image(folder);
        image.setSelectedWidth(120);
        try (var users = mockStatic(UsersDB.class, CALLS_REAL_METHODS);
             var tools = mockStatic(Tools.class, CALLS_REAL_METHODS); var files = mockConstruction(IwcmFile.class)) {
            tools.when(() -> Tools.getRealPath(anyString())).thenReturn("/private/tmp/planned-test.jpg");
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertEquals(200, imageEditor.saveImageAreaOfInterest(image, request).getStatusCode().value());
            verify(repository).save(argThat(saved -> Integer.valueOf(120).equals(saved.getSelectedWidth()) && folder.equals(saved.getImagePath())));
            clearInvocations(repository);
            user.setWritableFolders("/images/other/");
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
