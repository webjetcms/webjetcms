package sk.iway.iwcm.system.elfinder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.Map;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sk.iway.iwcm.*;
import sk.iway.iwcm.io.IwcmFile;

class FolderSizeServiceTest {
    @TempDir Path webapp;
    private final ServletContext original = Constants.getServletContext();
    private final Map<String, Boolean> flags = Map.of("fbrowserShowOnlyWritableFolders", Constants.getBoolean("fbrowserShowOnlyWritableFolders"),
            "elfinderUseFastLoading", Constants.getBoolean("elfinderUseFastLoading"));
    private final FolderSizeService.Owner owner = new FolderSizeService.Owner("session", 7, "example");
    private final FolderSizeService.Scan scan = new FolderSizeService.Scan(owner);
    private final FolderSizeService service = new FolderSizeService();
    private final cn.bluejoe.elfinder.service.FsService fs = mock(cn.bluejoe.elfinder.service.FsService.class, RETURNS_DEEP_STUBS);
    @BeforeEach void mountWebapp() {
        Constants.setBoolean("fbrowserShowOnlyWritableFolders", false);
        ServletContext context = mock(ServletContext.class);
        when(context.getRealPath(anyString())).thenAnswer(call -> webapp.resolve(call.<String>getArgument(0).substring(1)).toString());
        Constants.setServletContext(context);
    }
    @AfterEach void restoreContext() {
        service.destroy();
        Constants.setServletContext(original); flags.forEach(Constants::setBoolean);
    }
    @Test void countsEmptyAndDeepFoldersDespiteFastLoadingAndSkipsHiddenEntriesAndLinks() throws Exception {
        IwcmFile root = new IwcmFile(webapp.toFile());
        FolderSizeService.walk(root, file -> true, scan);
        assertEquals(new FolderSizeService.Progress(scan.id, "running", "0", 1), scan.progress());
        Path nested = webapp;
        for (int i = 0; i < 100; i++) nested = Files.createDirectory(nested.resolve("d"));
        Files.write(nested.resolve("image.png"), new byte[13]);
        Files.write(webapp.resolve("notes.txt"), new byte[7]);
        Files.write(Files.createDirectory(webapp.resolve("hidden")).resolve("draft.txt"), new byte[23]);
        Files.createSymbolicLink(nested.resolve("link"), webapp);
        Files.createSymbolicLink(webapp.resolve("alias"), webapp.resolve("notes.txt"));
        Constants.setBoolean("elfinderUseFastLoading", true);
        IwcmFsVolume volume = new IwcmFsVolume("files", root);
        assertEquals(0, volume.getSize(new IwcmFsItem(volume, root)));
        scan.folders = 0;
        FolderSizeService.walk(root, file -> !file.getName().equals("hidden"), scan);
        assertEquals(new FolderSizeService.Progress(scan.id, "running", "20", 101), scan.progress());
    }
    @ParameterizedTest @CsvSource({"IwcmFsVolume, true, true", "IwcmLibraryFsVolume, false, true", "IwcmActualPageFsVolume, false, true",
            "IwcmLibraryFsVolume, true, false", "IwcmActualPageFsVolume, true, false",
            "IwcmDocGroupFsVolume, false, false", "IwcmArchivFsVolume, false, false"})
    void offersSizeOnlyForConcreteFileFolders(String type, boolean root, boolean supported) throws Exception {
        var volume = mock(Class.forName(getClass().getPackageName() + "." + type).asSubclass(cn.bluejoe.elfinder.service.FsVolume.class));
        assertEquals(supported, FolderSizeService.supports(volume, root));
    }
    @Test void keepsExactStorageLengthsAndPropagatesMetadataFailures() throws Exception {
        IwcmFile directory = mock(IwcmFile.class), file = mock(IwcmFile.class);
        when(directory.listFiles()).thenReturn(new IwcmFile[]{file});
        when(file.isFile()).thenReturn(true);
        when(file.getLength()).thenReturn(9007199254740993L, -1L);
        FolderSizeService.walk(directory, entry -> true, scan);
        assertEquals("9007199254740993", scan.progress().bytes());
        assertThrows(IOException.class, () -> FolderSizeService.walk(directory, entry -> true, scan));
        when(directory.listFiles()).thenReturn(null);
        assertThrows(IOException.class, () -> FolderSizeService.walk(directory, entry -> true, scan));
    }
    @ParameterizedTest @CsvSource({"*, 42/images/products, 20, 2", "/images/products/, 42/images/products, 7, 1",
            "/images/products/*, 42/images/products, 20, 2", "/images/products/, 42/images/other, -1, 0",
            "/images/products/, 42, -1, 0", "/images/products/, 42/images/products/.svn/child, -1, 0",
            "/images/products/, 43/images/products, -1, 0", "*, link/products, -1, 0"})
    void checksDestinationPermissionsMountedRootsAndSymlinkAncestors(String writable, String target, long bytes, long folders) throws Exception {
        String base = "/files/protected/planned-changes/", mounted = base + "42/images/products";
        Path directory = Files.createDirectories(webapp.resolve((base + target).substring(1)));
        Files.write(directory.resolve("photo.jpg"), new byte[7]);
        Files.write(Files.createDirectory(directory.resolve("nested")).resolve("detail.jpg"), new byte[13]);
        Files.createDirectories(webapp.resolve(mounted.substring(1)));
        if (target.startsWith("link/")) Files.createSymbolicLink(webapp.resolve("images"), directory.getParent());
        IwcmFsVolume volume = bytes >= 0 || target.startsWith("43/")
                ? new IwcmActualPageFsVolume("page", "/", mounted) : new IwcmFsVolume("files", "/");
        Identity user = new Identity();
        user.setAdmin(true); user.setWritableFolders(writable);
        when(fs.getSecurityChecker()).thenReturn(new FsSecurityCheckForAll());
        try (var ignored = SetCharacterEncodingFilter.openIsolatedDataContext(null)) {
            RequestBean.setAttribute(FsService.BACKGROUND_USER, user);
            var item = volume.fromPath(target.startsWith("link/") ? "/images/products" : base + target);
            if (bytes < 0) assertThrows(IOException.class, () -> service.start(owner, user, fs, item));
            else {
                String id = service.start(owner, user, fs, item).scanId();
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                    while ("running".equals(service.status(owner, id).state())) TimeUnit.MILLISECONDS.sleep(10);
                });
                assertEquals(new FolderSizeService.Progress(id, "complete", Long.toString(bytes), folders), service.status(owner, id));
            }
        }
    }
    @Test void onlyOwnerCanPollOrCancelAnActiveScan() throws Exception {
        Files.write(webapp.resolve("notes.txt"), new byte[7]);
        IwcmFsVolume volume = mock(IwcmFsVolume.class);
        when(fs.getSecurityChecker().isReadable(any(), any())).thenReturn(true);
        CountDownLatch scanning = new CountDownLatch(1), cancelled = new CountDownLatch(1);
        when(volume.visibleEntries(any())).thenReturn(file -> {
            scanning.countDown();
            try { new CountDownLatch(1).await(10, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { cancelled.countDown(); Thread.currentThread().interrupt(); }
            return true;
        });
        String id = service.start(owner, mock(Identity.class), fs, new IwcmFsItem(volume, new IwcmFile(webapp.toFile()))).scanId();
        assertTrue(scanning.await(5, TimeUnit.SECONDS), "Scan must reach the blocking file");
        for (var other : new FolderSizeService.Owner[] {new FolderSizeService.Owner("other", 7, "example"),
                new FolderSizeService.Owner("session", 8, "example"), new FolderSizeService.Owner("session", 7, "other")}) {
            assertNull(service.status(other, id));
            service.cancel(other, id);
        }
        assertEquals("running", service.status(owner, id).state());
        service.cancel(owner, id);
        assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        assertNull(service.status(owner, id));
    }
}
