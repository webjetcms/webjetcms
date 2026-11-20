package sk.iway.iwcm.components.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContext;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks gallery folder permissions against a temporary web root and rolled-back settings records. */
class GalleryDimensionFolderTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;
    private static ValidatorFactory validation;
    private final String gallery = "/images/gallery/" + UUID.randomUUID();
    private final Identity user = new Identity();
    @TempDir
    Path root;
    private ServletContext previousContext;
    private EntityManager manager;
    private GalleryDimensionRepository repository;
    private GalleryDimenstionRestController controller;
    private MockedStatic<Adminlog> audit;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("gallery-folders-test");
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(GalleryDimension.class.getName()));
        factory.setJpaPropertyMap(Map.of("eclipselink.weaving", "false", "eclipselink.ddl-generation", "none"));
        factory.afterPropertiesSet();
        validation = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closePersistence() {
        if (validation != null) validation.close();
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void setUp() throws Exception {
        manager = factory.getObject().createEntityManager();
        manager.getTransaction().begin();
        repository = new JpaRepositoryFactory(manager).getRepository(GalleryDimensionRepository.class);
        audit = mockStatic(Adminlog.class);
        previousContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return onDisk(path).toString();
            }
        });
        Files.createDirectories(onDisk(gallery));
        MockHttpServletRequest request = new MockHttpServletRequest(Constants.getServletContext());
        request.getSession().setAttribute(Constants.USER_KEY, user);
        user.setWritableFolders(gallery + "/*");
        controller = new GalleryDimenstionRestController(repository, new GalleryTreeService(repository, request));
        controller.setRequest(request);
        controller.setValidator(validation.getValidator());
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(previousContext);
        audit.close();
        manager.getTransaction().rollback();
        manager.close();
    }

    /** A rejected create must leave both the submitted parent and the final path untouched. */
    @Test
    void deniedCreateLeavesNoDirectoriesOrSettings() {
        String parent = gallery + "/missing";
        user.setWritableFolders(gallery + "/");
        assertDenied(() -> controller.add(settings(parent, "Album")));
        assertFalse(Files.exists(onDisk(parent)));
        assertMissingRow(parent + "/album");

        // An exact grant on the parent does not grant access to its children.
        assertDenied(() -> controller.add(settings(gallery, "Album")));
        assertFalse(Files.exists(onDisk(gallery + "/album")));
        assertMissingRow(gallery + "/album");
    }

    /** An allowed leaf cannot cause mkdirs to create an unauthorized ancestor. */
    @Test
    void checksMissingAncestorsBeforeCreatingAnyOfThem() {
        String missing = gallery + "/missing";
        String parent = missing + "/allowed";
        user.setWritableFolders(parent + "/*");
        assertDenied(() -> controller.add(settings(parent, "Album")));
        assertFalse(Files.exists(onDisk(missing)));
        assertMissingRow(parent + "/album");
    }

    /** A protected domain folder stays protected even when the parent path ends in a slash. */
    @Test
    void domainFolderCannotBeCreatedWithAParentGrant() {
        String domainName = "gallery-test-domain";
        user.setWritableFolders("/images/*");
        try (var adminTools = mockStatic(AdminTools.class)) {
            adminTools.when(AdminTools::getDomainNameFileAliasAppend).thenReturn("/" + domainName);
            assertDenied(() -> controller.add(settings("/images/", domainName)));
        }
        String target = "/images/" + domainName;
        assertFalse(Files.exists(onDisk(target)));
        assertMissingRow(target);
    }

    /** Saving settings for an existing directory without a row must preserve its contents. */
    @Test
    void savesSettingsForAnExistingDirectory() throws Exception {
        String path = gallery + "/album";
        Path directory = Files.createDirectory(onDisk(path));
        Path photo = Files.writeString(directory.resolve("photo.txt"), "original");
        controller.edit(-1, settings(path, "album"));
        manager.flush();
        manager.clear();
        assertEquals("album", repository.findFirstByPathAndDomainId(path, CloudToolsForCore.getDomainId()).orElseThrow().getName());
        assertEquals("original", Files.readString(photo));
    }

    /** A move requires access to both the stored source and the submitted destination. */
    @Test
    void deniedMovePreservesSourceFilesAndSettings() throws Exception {
        String source = gallery + "/album";
        long id = createAlbum();
        Path image = Files.writeString(onDisk(source).resolve("photo.txt"), "original");
        String target = gallery + "/other/album";
        GalleryDimension change = settings(target, "Changed");
        user.setWritableFolders(source + "/");
        assertDenied(() -> controller.edit(id, change));
        user.setWritableFolders(gallery + "/other/*");
        assertDenied(() -> controller.edit(id, change));
        assertFalse(Files.exists(onDisk(gallery + "/other")));
        assertEquals("original", Files.readString(image));
        GalleryDimension stored = reload(id);
        assertEquals(source, stored.getPath());
        assertEquals("Album", stored.getName());
        assertMissingRow(target);
    }

    /** An authorized edit can restore a missing folder belonging to an existing settings row. */
    @Test
    void authorizedEditRestoresTheStoredFolder() throws Exception {
        String path = gallery + "/album";
        long id = createAlbum();
        Files.delete(onDisk(path));
        user.setWritableFolders(gallery + "/");
        assertDenied(() -> controller.edit(id, settings(path, "Changed")));
        assertFalse(Files.exists(onDisk(path)));
        user.setWritableFolders(path + "/");
        controller.edit(id, settings(path, "Changed"));
        assertTrue(Files.isDirectory(onDisk(path)));
        assertEquals("Changed", reload(id).getName());
    }

    /** A permitted move carries the original files and stores the new path with the edited settings. */
    @Test
    void authorizedMoveTransfersContentsAndSettings() throws Exception {
        long id = createAlbum();
        String source = gallery + "/album";
        Files.writeString(onDisk(source).resolve("photo.txt"), "original");
        Files.createDirectory(onDisk(gallery + "/other"));
        String target = gallery + "/other/album";
        try (var redirects = mockStatic(UrlRedirectDB.class)) {
            controller.edit(id, settings(target, "Moved"));
        }
        assertFalse(Files.exists(onDisk(source)));
        assertEquals("original", Files.readString(onDisk(target).resolve("photo.txt")));
        GalleryDimension stored = reload(id);
        assertEquals(target, stored.getPath());
        assertEquals("Moved", stored.getName());
    }

    private long createAlbum() {
        GalleryDimension album = controller.add(settings(gallery, "Album")).getBody();
        manager.flush();
        manager.clear();
        return album.getId();
    }

    private GalleryDimension settings(String path, String name) {
        GalleryDimension entity = new GalleryDimension();
        entity.setPath(path);
        entity.setName(name);
        entity.setResizeMode("S");
        entity.setUpdateInDoc(false);
        return entity;
    }

    private Path onDisk(String virtualPath) {
        return root.resolve(virtualPath.substring(1));
    }

    private void assertDenied(Runnable operation) {
        RuntimeException exception = assertThrows(RuntimeException.class, operation::run);
        assertEquals(controller.getProp().getText("user.rights.no_folder_rights"), exception.getMessage());
    }

    private void assertMissingRow(String path) {
        manager.flush();
        manager.clear();
        assertTrue(repository.findFirstByPathAndDomainId(path, CloudToolsForCore.getDomainId()).isEmpty());
    }

    private GalleryDimension reload(long id) {
        manager.flush();
        manager.clear();
        return repository.findById(id).orElseThrow();
    }
}
