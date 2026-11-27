package sk.iway.iwcm.io.s3;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.common.MultipartFileSender;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises real S3 storage through the public CMS file and HTTP APIs. */
@Tag("s3-integration")
class S3FileIntegrationTest extends BaseWebjetTest {
    @TempDir Path webroot;
    private final Map<String, String> previous = new HashMap<>();
    private IwcmFile directory;
    private MinioClient client;
    private String bucket;
    private String previousBase;
    private final java.util.List<IwcmFile> additionalDirectories = new java.util.ArrayList<>();

    @BeforeEach
    void configure() {
        previousBase = System.getProperty("webjetTestBasepath");
        System.setProperty("webjetTestBasepath", webroot.toString());
        Map<String, String> settings = Map.ofEntries(
                Map.entry("s3Enabled", "true"), Map.entry("s3UseForDirs", "/files,/images,/shared,/WEB-INF/imgcache,/WEB-INF/formfiles"),
                Map.entry("s3Url", env("S3_ENDPOINT", "http://localhost:9000")),
                Map.entry("s3AccessKey", env("S3_ACCESS_KEY", "webjet-local")),
                Map.entry("s3SecretKey", env("S3_SECRET_KEY", "webjet-local-secret")),
                Map.entry("s3BucketName", env("S3_BUCKET", "webjet-autotest")), Map.entry("s3AwsEnable", "false"),
                Map.entry("s3AwsRegion", "us-east-1"), Map.entry("s3MetaCacheTtlSec", "5"), Map.entry("s3MetaCacheMaxDirs", "500"),
                Map.entry("multiDomainEnabled", "false"), Map.entry("enableStaticFilesExternalDir", "false"),
                Map.entry("cloudStaticFilesDir", ""));
        settings.forEach((key, value) -> { previous.put(key, Constants.getString(key)); Constants.setString(key, value); });
        S3Storage.reset();
        bucket = Constants.getString("s3BucketName");
        client = MinioClient.builder().endpoint(Constants.getString("s3Url")).region("us-east-1")
                .credentials(Constants.getString("s3AccessKey"), Constants.getString("s3SecretKey")).build();
        directory = IwcmFile.fromVirtualPath("/files/autotest-s3-" + UUID.randomUUID());
        assertTrue(directory.mkdirs());
    }

    @AfterEach
    void cleanup() {
        try {
            for (IwcmFile extra : additionalDirectories) if (extra.exists()) assertTrue(FileTools.deleteDirTree(extra));
            if (directory != null && directory.exists()) assertTrue(FileTools.deleteDirTree(directory));
        }
        finally {
            S3Storage.reset();
            previous.forEach(Constants::setString);
            if (previousBase == null) System.clearProperty("webjetTestBasepath");
            else System.setProperty("webjetTestBasepath", previousBase);
        }
    }

    /** Uploads bytes, checks the actual bucket, overwrites them and verifies empty-file semantics. */
    @Test
    void createsReadsOverwritesAndDeletesUnicodeFile() throws Exception {
        IwcmFile file = new IwcmFile(directory, "žltý kôň + medzera.pdf");
        byte[] bytes = "S3 obsah — žľť".getBytes(StandardCharsets.UTF_8);
        write(file, bytes);
        assertTrue(file.isFile());
        assertEquals(bytes.length, file.length());
        assertTrue(file.lastModified() > 0);
        assertFalse(Files.exists(Path.of(file.getPath())));
        try (InputStream direct = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key(file)).build())) {
            assertArrayEquals(bytes, direct.readAllBytes());
        }
        assertFalse(file.createNewFile());
        assertArrayEquals(bytes, read(file));
        try (IwcmOutputStream output = new IwcmOutputStream(file)) { output.flush(); }
        assertEquals(0, file.length());
        assertEquals(0, read(file).length);
        assertTrue(file.delete());
        assertFalse(file.delete());
        assertThrows(FileNotFoundException.class, () -> new IwcmInputStream(file));
    }

    /** Direct S3 objects create implicit directories and cache invalidation exposes mutations. */
    @Test
    void listsDirectoriesAndPreservesNonEmptyDirectories() throws Exception {
        IwcmFile nested = new IwcmFile(directory, "nested");
        client.putObject(PutObjectArgs.builder().bucket(bucket).object(key(nested) + "/direct.txt")
                .stream(new ByteArrayInputStream(new byte[] {42}), 1L, 8L * 1024 * 1024).build());
        S3Storage.getInstance().clearCache();
        assertTrue(nested.isDirectory());
        assertEquals(1, nested.listFiles().length);
        assertFalse(nested.delete());
        IwcmFile empty = new IwcmFile(directory, "empty");
        assertTrue(empty.mkdirs());
        assertFalse(empty.mkdirs());
        assertEquals(2, directory.listFiles().length);
        assertTrue(empty.delete());
        assertEquals(1, directory.listFiles().length);
    }

    /** Directory listing must traverse S3 pagination beyond the first 1,000 objects. */
    @Test
    void listsMoreThanOneThousandObjects() throws Exception {
        for (int i = 0; i < 1005; i++) {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key(directory) + "/entry-" + i)
                    .stream(new ByteArrayInputStream(new byte[0]), 0L, 8L * 1024 * 1024).build());
        }
        S3Storage.getInstance().clearCache();
        assertEquals(1005, directory.listFiles().length);
    }

    /** Moves both files and complete trees between disk and S3 without leaving the source behind. */
    @Test
    void movesBetweenStorageBackends() throws Exception {
        Path local = webroot.resolve("local/source.txt");
        Files.createDirectories(local.getParent());
        Files.writeString(local, "local data");
        IwcmFile remote = new IwcmFile(directory, "remote.txt");
        assertTrue(FileTools.moveFile(new IwcmFile(local), remote));
        assertFalse(Files.exists(local));
        assertEquals("local data", new String(read(remote), StandardCharsets.UTF_8));
        assertTrue(FileTools.moveFile(remote, new IwcmFile(local)));
        assertFalse(remote.exists());
        assertEquals("local data", Files.readString(local));
        IwcmFile remoteTree = new IwcmFile(directory, "tree");
        assertTrue(new IwcmFile(local.getParent()).renameTo(remoteTree));
        assertFalse(Files.exists(local));
        IwcmFile renamed = new IwcmFile(directory, "renamed");
        assertTrue(remoteTree.renameTo(renamed));
        assertFalse(remoteTree.exists());
        assertTrue(new IwcmFile(renamed, "source.txt").exists());
        assertFalse(renamed.renameTo(new IwcmFile(renamed, "inside")));
        assertTrue(renamed.renameTo(new IwcmFile(local.getParent())));
        assertEquals("local data", Files.readString(local));
    }

    /** DB files can move to S3 and back; explicit historical IDs still read the original backend. */
    @Test
    void movesBetweenDatabaseAndS3() throws Exception {
        String dbPath = "/WEB-INF/" + directory.getName();
        Map<String, String> settings = new HashMap<>();
        for (String name : new String[] {"iwfs_useDB", "iwfs_dirsInDB", "iwfs_useVersioning", "s3UseForDirs"}) settings.put(name, Constants.getString(name));
        IwcmFile dbDirectory = null;
        try (java.sql.Connection realConnection = sk.iway.iwcm.DBPool.getConnection()) {
            java.sql.Connection connection = org.mockito.Mockito.mock(java.sql.Connection.class, org.mockito.AdditionalAnswers.delegatesTo(realConnection));
            org.mockito.Mockito.doNothing().when(connection).close();
            try (var statement = realConnection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE file_fat (file_fat_id INT PRIMARY KEY, virtual_path VARCHAR(1024), fsize BIGINT, depth INT, last_modified BIGINT, is_deleted BOOLEAN DEFAULT FALSE)");
                statement.execute("CREATE TEMPORARY TABLE file_data (node_id INT AUTO_INCREMENT PRIMARY KEY, file_fat_id INT, data LONGBLOB)");
            }
            try (var database = org.mockito.Mockito.mockStatic(sk.iway.iwcm.DBPool.class);
                    var keys = org.mockito.Mockito.mockStatic(sk.iway.iwcm.PkeyGenerator.class);
                    var cluster = org.mockito.Mockito.mockStatic(sk.iway.iwcm.system.cluster.ClusterDB.class);
                    var fileTables = org.mockito.Mockito.mockStatic(sk.iway.iwcm.io.IwcmFsDB.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
                database.when(sk.iway.iwcm.DBPool::getConnection).thenReturn(connection);
                java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger();
                keys.when(() -> sk.iway.iwcm.PkeyGenerator.getNextValue(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> nextId.incrementAndGet());
                fileTables.when(sk.iway.iwcm.io.IwcmFsDB::getFatIdTable).thenReturn(new HashMap<>());
                fileTables.when(sk.iway.iwcm.io.IwcmFsDB::getModifiedTable).thenReturn(new HashMap<>());
            Constants.setBoolean("iwfs_useDB", true);
            Constants.setBoolean("iwfs_useVersioning", false);
            Constants.setString("iwfs_dirsInDB", dbPath);
            sk.iway.iwcm.io.IwcmFsDB.init();
            dbDirectory = IwcmFile.fromVirtualPath(dbPath);
            assertTrue(dbDirectory.mkdirs());
            IwcmFile dbFile = new IwcmFile(dbDirectory, "source.txt");
            write(dbFile, new byte[] {3, 7});
            assertArrayEquals(new byte[] {3, 7}, read(dbFile));
            int fatId = sk.iway.iwcm.io.IwcmFsDB.getFatIdTable().get(dbFile.getVirtualPath());
            Constants.setString("s3UseForDirs", settings.get("s3UseForDirs") + "," + dbPath);
            try (IwcmInputStream history = new IwcmInputStream(dbFile.getVirtualPath(), fatId)) {
                assertArrayEquals(new byte[] {3, 7}, history.readAllBytes());
            }
            Constants.setString("s3UseForDirs", settings.get("s3UseForDirs"));
            IwcmFile remote = new IwcmFile(directory, "database.txt");
            assertTrue(dbFile.renameTo(remote));
            assertFalse(dbFile.exists());
            assertArrayEquals(new byte[] {3, 7}, read(remote));
            assertTrue(remote.renameTo(dbFile));
            assertFalse(remote.exists());
            assertArrayEquals(new byte[] {3, 7}, read(dbFile));
            assertTrue(FileTools.deleteDirTree(dbDirectory));
            } finally {
                try (var statement = realConnection.createStatement()) {
                    statement.execute("DROP TEMPORARY TABLE IF EXISTS file_data");
                    statement.execute("DROP TEMPORARY TABLE IF EXISTS file_fat");
                }
            }
        } finally {
            settings.forEach(Constants::setString);
            sk.iway.iwcm.io.IwcmFsDB.init();
        }
    }

    /** Existing external-directory flags do not require a dummy filesystem location for S3. */
    @Test
    void supportsExternalFlagWithoutDummyDirectory() throws Exception {
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setString("cloudStaticFilesDir", "");
        IwcmFile file = new IwcmFile(directory, "external.txt");
        write(file, new byte[] {6});
        assertArrayEquals(new byte[] {6}, read(IwcmFile.fromVirtualPath(file.getVirtualPath())));
    }

    /** A read failure must not publish a partial replacement. */
    @Test
    void failedCopyPreservesPreviousContent() throws Exception {
        IwcmFile target = new IwcmFile(directory, "existing.txt");
        write(target, new byte[] {5, 6});
        InputStream broken = new InputStream() {
            private int calls;
            @Override public int read() throws IOException {
                if (calls++ > 1) throw new IOException("Simulated source failure");
                return 1;
            }
        };
        assertFalse(FileTools.copyFile(broken, target));
        assertArrayEquals(new byte[] {5, 6}, read(target));
    }

    /** Real HTTP serving supports HEAD, cache validation and independent byte ranges. */
    @Test
    void servesHeadRangesAndChangedContent() throws Exception {
        IwcmFile file = new IwcmFile(directory, "download.txt");
        write(file, "0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse full = serve(file, "GET", null, null);
        assertEquals(200, full.getStatus());
        assertEquals("0123456789", full.getContentAsString());
        MockHttpServletResponse head = serve(file, "HEAD", null, null);
        assertEquals("10", head.getHeader("Content-Length"));
        assertEquals(0, head.getContentAsByteArray().length);
        MockHttpServletResponse range = serve(file, "GET", "bytes=3-5", null);
        assertEquals(206, range.getStatus());
        assertEquals("345", range.getContentAsString());
        MockHttpServletResponse multipart = serve(file, "GET", "bytes=5-7,1-2", null);
        assertEquals(206, multipart.getStatus());
        assertTrue(multipart.getContentAsString().contains("567"));
        assertTrue(multipart.getContentAsString().contains("12"));
        assertEquals(416, serve(file, "GET", "bytes=20-30", null).getStatus());
        String etag = full.getHeader("ETag");
        assertEquals(304, serve(file, "GET", null, etag).getStatus());
        write(file, new byte[] {42});
        assertEquals(200, serve(file, "GET", null, etag).getStatus());
    }

    /** An open stream and children keep their domain even after the request context changes. */
    @Test
    void isolatesDomainsAndCapturesStreamNamespace() throws Exception {
        sk.iway.iwcm.RequestBean previousRequest = sk.iway.iwcm.SetCharacterEncodingFilter.getCurrentRequestBean();
        Constants.setBoolean("multiDomainEnabled", true);
        try {
            sk.iway.iwcm.RequestBean request = new sk.iway.iwcm.RequestBean();
            request.setDomain("www.autotest-a.example");
            sk.iway.iwcm.SetCharacterEncodingFilter.setCurrentRequestBean(request);
            IwcmFile first = IwcmFile.fromVirtualPath(directory.getVirtualPath());
            additionalDirectories.add(first);
            first.mkdirs();
            IwcmFile original = new IwcmFile(first, "same.txt");
            IwcmOutputStream pending = new IwcmOutputStream(original);
            pending.write(new byte[] {1});
            IwcmFile shared = IwcmFile.fromVirtualPath("/shared/" + directory.getName());
            additionalDirectories.add(shared);
            shared.mkdirs();
            write(new IwcmFile(shared, "common.txt"), new byte[] {7});
            request.setDomain("autotest-b.example");
            IwcmFile second = IwcmFile.fromVirtualPath(directory.getVirtualPath());
            additionalDirectories.add(second);
            second.mkdirs();
            write(new IwcmFile(second, "same.txt"), new byte[] {2});
            pending.close();
            assertArrayEquals(new byte[] {1}, read(first.listFiles()[0]));
            assertArrayEquals(new byte[] {2}, read(new IwcmFile(second, "same.txt")));
            assertArrayEquals(new byte[] {7}, read(IwcmFile.fromVirtualPath(shared.getVirtualPath() + "/common.txt")));
            assertFalse(Constants.getBoolean("enableStaticFilesExternalDir"));
            sk.iway.iwcm.SetCharacterEncodingFilter.setCurrentRequestBean(null);
            assertArrayEquals(new byte[] {1}, read(new IwcmFile(first, "same.txt")));
            assertThrows(IllegalStateException.class, () -> IwcmFile.fromVirtualPath(directory.getVirtualPath()));
        } finally {
            sk.iway.iwcm.SetCharacterEncodingFilter.setCurrentRequestBean(previousRequest);
            Constants.setBoolean("multiDomainEnabled", false);
        }
    }

    /** Permission and bucket errors remain errors, even when stale local bytes exist. */
    @Test
    void storageFailuresNeverFallBackToDisk() throws Exception {
        IwcmFile target = new IwcmFile(directory, "stale.txt");
        Files.createDirectories(Path.of(target.getPath()).getParent());
        Files.writeString(Path.of(target.getPath()), "stale local data");
        assertFalse(target.exists());
        assertThrows(FileNotFoundException.class, () -> new IwcmInputStream(target));
        S3Storage denied = new S3Storage(MinioClient.builder().endpoint(Constants.getString("s3Url"))
                .region("us-east-1").credentials("invalid-autotest-key", "invalid-autotest-secret").build(), bucket, 0, 0);
        assertThrows(java.io.UncheckedIOException.class, () -> denied.metadata(key(target)));
        S3Storage missingBucket = new S3Storage(client, "autotest-missing-" + UUID.randomUUID(), 0, 0);
        assertThrows(java.io.UncheckedIOException.class, () -> missingBucket.list("defaulthost/files"));
        assertThrows(IOException.class, () -> missingBucket.open(key(target)));
        assertEquals("stale local data", Files.readString(Path.of(target.getPath())));
    }

    /** File ancestors and directory targets must never become ambiguous S3 prefixes. */
    @Test
    void rejectsFileDirectoryConflicts() throws Exception {
        IwcmFile parentFile = new IwcmFile(directory, "parent");
        assertTrue(parentFile.createNewFile());
        assertFalse(parentFile.createNewFile());
        IwcmFile nested = new IwcmFile(parentFile, "child.txt");
        assertThrows(IOException.class, () -> write(nested, new byte[] {3}));
        assertFalse(nested.exists());
        assertTrue(parentFile.isFile());
        assertThrows(IOException.class, () -> write(directory, new byte[] {4}));
        assertTrue(directory.isDirectory());
    }

    /** Multipart writes exceed the SDK part size without loading the file into one heap buffer. */
    @Test
    void streamsLargeMultipartObject() throws Exception {
        IwcmFile large = new IwcmFile(directory, "large.bin");
        byte[] block = new byte[1024 * 1024];
        new java.util.Random(42).nextBytes(block);
        java.security.MessageDigest expected = java.security.MessageDigest.getInstance("SHA-256");
        try (IwcmOutputStream out = new IwcmOutputStream(large)) {
            for (int i = 0; i < 18; i++) { out.write(block); expected.update(block); }
        }
        assertEquals(18L * block.length, large.length());
        java.security.MessageDigest actual = java.security.MessageDigest.getInstance("SHA-256");
        try (InputStream in = new IwcmInputStream(large)) {
            int count;
            while ((count = in.read(block)) != -1) actual.update(block, 0, count);
        }
        assertArrayEquals(expected.digest(), actual.digest());
    }

    /** The administrative endpoint publishes only the final chunk and supports replacement modes. */
    @Test
    void uploadsChunksAndMultipleFilesThroughAdminServlet() throws Exception {
        org.springframework.mock.web.MockHttpSession session = adminSession();
        String name = "autotest-document.pdf";
        for (int i = 0; i < 3; i++) {
            org.json.JSONObject result = upload(session, directory, name, new byte[] {(byte)i}, i, 3, "overwrite", false);
            assertTrue(result.getBoolean("success"), result.toString());
            assertEquals(i == 2, new IwcmFile(directory, name).exists());
        }
        assertArrayEquals(new byte[] {0, 1, 2}, read(new IwcmFile(directory, name)));
        assertTrue(upload(session, directory, name, new byte[] {4}, 0, 1, "keepboth", false).getBoolean("success"));
        assertEquals(2, directory.listFiles().length);
        assertTrue(upload(session, directory, "autotest-second.pdf", new byte[0], 0, 1, "overwrite", false).getBoolean("success"));
        assertEquals(0, new IwcmFile(directory, "autotest-second.pdf").length());
        org.json.JSONObject denied = upload(new org.springframework.mock.web.MockHttpSession(), directory, "forbidden.pdf", new byte[] {9}, 0, 1, "overwrite", false);
        assertFalse(denied.getBoolean("success"));
        assertFalse(new IwcmFile(directory, "forbidden.pdf").exists());
    }

    /** Image-editor saves and gallery resizing operate on remote originals and thumbnails. */
    @Test
    void uploadsEditsAndResizesGalleryImage() throws Exception {
        for (String setting : new String[] {"imageAlwaysCreateGalleryBean", "galleryWatermarkApplyOnUpload", "galleryConvertCmykToRgb"}) {
            previous.put(setting, Constants.getString(setting));
            Constants.setBoolean(setting, false);
        }
        IwcmFile images = IwcmFile.fromVirtualPath("/images/" + directory.getName());
        additionalDirectories.add(images);
        images.mkdirs();
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes));
        org.json.JSONObject result = upload(adminSession(), images, "autotest-image.png", java.util.Base64.getEncoder().encode(bytes.toByteArray()), 0, 1, "overwrite", true);
        assertTrue(result.getBoolean("success"), result.toString());
        IwcmFile original = new IwcmFile(images, "autotest-image.png");
        assertArrayEquals(bytes.toByteArray(), read(original));
        IwcmFile thumbnail = new IwcmFile(images, "s_autotest-image.png");
        assertEquals(0, sk.iway.iwcm.gallery.GalleryDB.resizePicture(original.getPath(), thumbnail.getPath(), 40, 30));
        try (InputStream in = new IwcmInputStream(thumbnail)) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in);
            assertEquals(40, image.getWidth());
            assertEquals(30, image.getHeight());
        }
        assertFalse(Files.exists(Path.of(thumbnail.getPath())));
        IwcmFile thumbCache = IwcmFile.fromVirtualPath("/WEB-INF/imgcache/images/" + directory.getName());
        additionalDirectories.add(thumbCache);
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thumb" + original.getVirtualPath());
            request.setSession(adminSession());
            request.addParameter("w", "40");
            request.addParameter("h", "30");
            MockHttpServletResponse response = new MockHttpServletResponse();
            sk.iway.iwcm.editor.ThumbServlet.getThumbImage(original.getVirtualPath(), request, response);
            assertEquals(200, response.getStatus());
            assertEquals(40, javax.imageio.ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray())).getWidth());
        }
        assertTrue(thumbCache.exists());
        assertFalse(upload(adminSession(), images, "autotest-image.png", "invalid!base64".getBytes(StandardCharsets.UTF_8), 0, 1, "overwrite", true).getBoolean("success"));
        assertArrayEquals(bytes.toByteArray(), read(original));
    }

    /** Public XHR uploads keep temporary chunks local and publish form attachments to S3. */
    @Test
    void movesXhrFormAttachmentIntoS3() throws Exception {
        IwcmFile formDirectory = IwcmFile.fromVirtualPath("/WEB-INF/formfiles/" + directory.getName());
        additionalDirectories.add(formDirectory);
        formDirectory.mkdirs();
        var service = new sk.iway.iwcm.components.upload.XhrFileUploadService() {
            @Override public sk.iway.iwcm.components.upload.XhrFileUploadResponse processUpload(jakarta.servlet.http.HttpServletRequest request) {
                return super.processUpload(request);
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/xhr-upload/");
        request.addPart(new org.springframework.mock.web.MockPart("file", "autotest-document.pdf", new byte[] {8, 9}));
        try (var events = org.mockito.Mockito.mockStatic(sk.iway.iwcm.system.spring.events.WebjetEventPublisher.class)) {
            var response = service.processUpload(request);
            assertTrue(response.isSuccess(), response.getError());
            String fileKey = response.getKey().get(0);
            String temporary = service.getTempFilePath(fileKey);
            assertTrue(Files.exists(Path.of(temporary)));
            String savedName = service.moveFile(fileKey, formDirectory.getPath() + "/");
            assertNotNull(savedName);
            assertFalse(Files.exists(Path.of(temporary)));
            assertArrayEquals(new byte[] {8, 9}, read(new IwcmFile(formDirectory, savedName)));
        }
    }

    private org.springframework.mock.web.MockHttpSession adminSession() {
        sk.iway.iwcm.Identity user = new sk.iway.iwcm.Identity();
        user.setAdmin(true);
        user.setWritableFolders("*");
        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(Constants.USER_KEY, user);
        return session;
    }

    private org.json.JSONObject upload(org.springframework.mock.web.MockHttpSession session, IwcmFile folder, String name,
            byte[] bytes, int chunk, int chunks, String overwrite, boolean image) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/upload/chunk");
        request.setSession(session);
        request.addParameter("name", name);
        request.addParameter("uploadType", image ? "image" : "file");
        request.addParameter("destinationFolder", folder.getVirtualPath() + "/");
        request.addParameter("writeDirectlyToDestination", "true");
        request.addParameter("overwriteMode", overwrite);
        request.addParameter("dztotalfilesize", Integer.toString(bytes.length));
        request.addParameter("chunk", Integer.toString(chunk));
        request.addParameter("chunks", Integer.toString(chunks));
        if (image) request.addParameter("encoding", "base64");
        request.addPart(new org.springframework.mock.web.MockPart("file", name, bytes));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new sk.iway.iwcm.admin.upload.AdminUploadServlet().service(request, response);
        return new org.json.JSONObject(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private MockHttpServletResponse serve(IwcmFile file, String method, String range, String etag) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, file.getVirtualPath());
        if (range != null) request.addHeader("Range", range);
        if (etag != null) request.addHeader("If-None-Match", etag);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MultipartFileSender.fromFile(file).with(request).with(response).serveResource();
        return response;
    }

    private static void write(IwcmFile file, byte[] bytes) throws IOException {
        try (IwcmOutputStream output = new IwcmOutputStream(file)) { output.write(bytes); }
    }

    private static byte[] read(IwcmFile file) throws IOException {
        try (IwcmInputStream input = new IwcmInputStream(file)) { return input.readAllBytes(); }
    }

    private static String key(IwcmFile file) { return "defaulthost" + file.getVirtualPath(); }
    private static String env(String name, String fallback) { return System.getenv().getOrDefault(name, fallback); }
}
