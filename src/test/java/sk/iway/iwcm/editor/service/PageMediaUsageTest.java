package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests file references shared by the directory report and page cleanup. */
class PageMediaUsageTest extends BaseWebjetTest {
    @TempDir Path webroot;
    private ServletContext previousContext;

    @BeforeEach
    void useTemporaryWebRoot() {
        previousContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return webroot.resolve(path.substring(1)).toString();
            }
        });
    }

    @AfterEach
    void restoreWebRoot() {
        Constants.setServletContext(previousContext);
    }

    /** Pending revisions, group fields and configured stylesheets protect files. Superseded drafts do not. */
    @Test
    void preservesFilesFromPendingHistoryAndCmsRecords() throws Exception {
        String fixture = "autotest-" + UUID.randomUUID();
        String folder = "/files/" + fixture + "/";
        String old = folder + "old.pdf";
        String latest = folder + "latest.pdf";
        String second = folder + "second.pdf";
        String approval = folder + "approval.pdf";
        String scheduled = folder + "scheduled.pdf";
        String logo = folder + "logo.svg";
        Path css = Files.createDirectories(webroot.resolve("css")).resolve("autotest.css");
        Files.writeString(css, "body {background: url(" + logo + ")}");
        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            var dataSource = new SingleConnectionDataSource(connection, true);
            try (var pool = mockStatic(DBPool.class)) {
                pool.when(DBPool::getConnection).thenReturn(dataSource.getConnection());
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                for (String title : List.of(fixture, fixture + "-second")) {
                    jdbc.update("INSERT INTO documents (title,data,data_asc,navbar) VALUES (?,?,?,?)", title, title, title, title);
                }
                String revision = "INSERT INTO documents_history (doc_id,data,data_asc,navbar,actual,approved_by,publicable,awaiting_approve) "
                    + "SELECT doc_id,?,title,title,?,-1,?,? FROM documents WHERE title=?";
                jdbc.update(revision, old, false, false, null, fixture);
                jdbc.update(revision, approval, false, false, "7", fixture);
                jdbc.update(revision, scheduled, false, true, null, fixture);
                jdbc.update(revision, latest, false, false, null, fixture);
                jdbc.update(revision, second, false, false, null, fixture + "-second");

                String groupFile = folder + "group.pdf";
                jdbc.update("INSERT INTO groups (group_name,field_a) VALUES (?,?)", fixture, groupFile);
                jdbc.update("INSERT INTO templates (temp_name,forward,css) VALUES (?,?,?)", fixture, "autotest.jsp", "/css/autotest.css");
                assertEquals(Set.of(latest, second, approval, scheduled, groupFile, logo),
                    FileReferenceScanner.referencedElsewhere(Set.of(old, latest, second, approval, scheduled, groupFile, logo), Map.of(), Set.of()));
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    @EnabledOnOs({ OS.MAC, OS.LINUX })
    void keepsFilesLinkedThroughADirectoryAlias() throws Exception {
        String folder = "/files/autotest-" + UUID.randomUUID();
        Path directory = Files.createDirectories(webroot.resolve(folder.substring(1)));
        Files.createFile(directory.resolve("guide.pdf"));
        Files.createSymbolicLink(directory.resolveSibling("autotest-link"), directory);
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("link.html");
        Files.writeString(template, "<a href='/files/autotest-link/guide.pdf'>Guide</a>");
        assertEquals(Set.of(folder + "/guide.pdf"),
            FileReferenceScanner.referencedElsewhere(Set.of(folder + "/guide.pdf", folder + "/unused.pdf"), Map.of(), Set.of()));
    }

    /** The directory report resolves public URLs from embeds, galleries and stylesheets against aliased storage. */
    @Test
    void findsReferencesInSiteSources() throws Exception {
        String url = "/files/autotest-" + UUID.randomUUID();
        String storage = url.replace("/files/", "/files/autotest-tenant/");
        Path directory = Files.createDirectories(webroot.resolve(storage.substring(1)));
        String background = "čaj.jpg";
        String legacyName = new String(background.getBytes(StandardCharsets.UTF_8), Charset.forName(Constants.FILE_ENCODING));
        for (String name : List.of("map.jpg", "legacy.jpg", "guide.pdf", background, legacyName, "unused.txt")) {
            Files.createFile(directory.resolve(name));
        }
        Path album = Files.createDirectories(directory.resolve("gallery/album"));
        Files.createFile(album.resolve("photo.jpg"));
        Files.createFile(album.resolve("unused.pdf"));
        Path sources = Files.createDirectories(webroot.resolve("templates"));
        Files.writeString(sources.resolve("page.css"), "body { background-image: url(" + url + "/" + background + "); }");
        String legacyEmbed = "utf8:" + Tools.URLEncode("<img src='" + url + "/legacy.jpg'>");
        // The encoded Document Embed URL crosses the scanner's read boundary.
        Files.writeString(sources.resolve("embed.html"), " ".repeat(22000) + "!INCLUDE(/components/app-docsembed/embed.jsp,"
            + " ".repeat(12000) + "url=" + Tools.base64Encode(url + "/guide.pdf") + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode("<img src='" + url + "/map.jpg'>") + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode(legacyEmbed) + ")!"
            + "!INCLUDE(/components/gallery/gallery.jsp, dir=" + url + "/gallery, recursive=true)!"
            + "<a href='" + url + "/unused.txt.bak'>Backup</a>");

        boolean previous = Constants.getBoolean("multiDomainEnabled");
        ConfDetails alias = new ConfDetails();
        alias.setValue("autotest-tenant");
        try (var settings = mockStatic(ConstantsV9.class)) {
            Constants.setBoolean("multiDomainEnabled", true);
            settings.when(() -> ConstantsV9.getValuesStartsWith("multiDomainAlias:")).thenReturn(List.of(alias));
            assertEquals(List.of(storage + "/gallery/album/unused.pdf", storage + "/unused.txt"),
                FileTools.getDirFileUsage(storage, null).stream().map(file -> file.getVirtualParent() + "/" + file.getName()).toList());
        } finally {
            Constants.setBoolean("multiDomainEnabled", previous);
        }
    }

    /** A gallery without a directory uses images directly under /images/gallery. */
    @Test
    void protectsImagesInTheDefaultGalleryDirectory() throws Exception {
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("gallery.html");
        Files.writeString(template, "!INCLUDE(/components/gallery/gallery.jsp)!");
        String photo = "/images/gallery/team.jpg";
        var candidates = Set.of(photo, "/images/gallery/team.pdf", "/images/gallery/album/team.jpg");
        try (var pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(mock(Connection.class, RETURNS_DEEP_STUBS));
            assertEquals(Set.of(photo), FileReferenceScanner.referencedElsewhere(candidates, Map.of(), Set.of()));
        }
    }

    /** A failed scan cannot produce a list of files offered for deletion. */
    @Test
    void rejectsIncompleteDirectoryScans() throws Exception {
        Files.createFile(Files.createDirectories(webroot.resolve("files")).resolve("autotest.pdf"));
        Connection broken = mock(Connection.class);
        when(broken.prepareStatement(anyString())).thenThrow(new SQLException("Simulated scan failure"));
        try (var pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(broken);
            assertThrows(IllegalStateException.class, () -> FileTools.getDirFileUsage("/files", null));
        }
    }
}
