package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.filebrowser.UnusedFile;
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

    /**
     * Verifies banner, embed, gallery and template layout references while leaving unrelated files eligible for cleanup.
     *
     * @throws Exception if the template or database fixtures cannot be prepared or scanned
     */
    @Test
    void preservesReferencesOutsidePageContent() throws Exception {
        String folder = "/files/autotest-" + UUID.randomUUID() + "/";
        String banner = folder + "mobile.jpg";
        String document = folder + "catalogue.pdf";
        String image = folder + "map.jpg";
        String legacyImage = folder + "legacy.jpg";
        String download = folder + "price list.pdf";
        String unused = folder + "unused.txt";
        String gallery = folder + "events";
        String nestedImage = gallery + "/conference/team.jpg";
        String otherGallery = gallery + "-archive";
        String layouts = "/templates/autotest-layouts/";
        String desktop = layouts + "story.html";
        String phone = layouts + "story-phone.jsp";
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("embed.html");
        // The embed starts in one read and its encoded URL ends in the next.
        Files.writeString(template, " ".repeat(22000) + "!INCLUDE(/components/app-docsembed/embed.jsp,"
            + " ".repeat(12000) + "url=" + Tools.base64Encode(document) + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode("<img src='" + image + "'>") + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode("utf8:" + Tools.URLEncode("<img src='" + legacyImage + "'>")) + ")!"
            + "!INCLUDE(/components/gallery/gallery.jsp, dir=&quot;" + gallery + "/&quot;, recursive=true)!"
            + "<a href='/download?file=" + download.replace(" ", "%20") + "&amp;lng=sk'>Download</a>"
            + "<a href='" + unused + ".bak'>Backup</a>"
            + "!INCLUDE(/components/gallery/gallery.jsp, dir=" + otherGallery + ", recursive=false)!");

        Set<String> candidates = Set.of(banner, document, image, legacyImage, download, nestedImage, unused,
            gallery + "/conference/unused.pdf", otherGallery + "/conference/team.jpg", desktop, phone, layouts + "archive/story.html");
        for (String candidate : candidates) {
            Path file = webroot.resolve(candidate.substring(1));
            Files.createDirectories(file.getParent());
            Files.writeString(file, candidate.equals(unused) ? "Fixture: unused.txt" : "");
        }

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            var dataSource = new SingleConnectionDataSource(connection, true);
            int originalDbType = Constants.DB_TYPE;
            try {
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                String insertLayout = "INSERT INTO templates (temp_name,forward,template_install_name) VALUES (?,?,?)";
                jdbc.update(insertLayout, "Autotest story", "story.html", "autotest-layouts");
                jdbc.update(insertLayout, "Autotest story device=phone", phone, "autotest-layouts");
                jdbc.update(
                    "INSERT INTO banner_banners (banner_id,banner_type,active,image_link_mobile) VALUES (?,?,?,?)",
                    PkeyGenerator.getNextValue("banner_banners"), 4, true, banner);
                try (var pool = mockStatic(DBPool.class)) {
                    pool.when(DBPool::getConnection).thenReturn(dataSource.getConnection());
                    assertEquals(Set.of(banner, document, image, legacyImage, download, nestedImage, desktop, phone),
                        FileReferenceScanner.referencedElsewhere(candidates, Map.of(), Set.of()));
                    assertEquals(List.of(otherGallery + "/conference/team.jpg", gallery + "/conference/unused.pdf", unused),
                        FileTools.getDirFileUsage(folder, null).stream().map(file -> file.getVirtualParent() + "/" + file.getName()).toList());

                    Connection emptyDatabase = mock(Connection.class, RETURNS_DEEP_STUBS);
                    pool.when(DBPool::getConnection).thenReturn(emptyDatabase);
                    String photo = "/images/gallery/team.jpg";
                    for (String options : List.of("", ", style=photoSwipe")) {
                        Files.writeString(template, "!INCLUDE(/components/gallery/gallery.jsp" + options + ")!");
                        assertEquals(Set.of(photo), FileReferenceScanner.referencedElsewhere(
                            Set.of(photo, "/images/gallery/team.pdf", "/images/gallery/album/team.jpg"), Map.of(), Set.of()), options);
                    }

                    Connection broken = mock(Connection.class);
                    when(broken.prepareStatement(anyString())).thenThrow(new SQLException("Simulated scan failure"));
                    pool.when(DBPool::getConnection).thenReturn(broken);
                    Constants.DB_TYPE = Constants.DB_PGSQL;
                    assertThrows(IllegalStateException.class, () -> FileTools.getDirFileUsage(folder, null));
                    verify(broken).setAutoCommit(false);
                }
            } finally {
                Constants.DB_TYPE = originalDbType;
                connection.rollback();
            }
        }
    }

    /** A public link protects the corresponding file when scanning a domain's storage directory. */
    @Test
    void scansDirectoryWithDomainAlias() throws Exception {
        String folder = "/files/autotest-tenant/batch-" + UUID.randomUUID();
        Path directory = Files.createDirectories(webroot.resolve(folder.substring(1)));
        for (int i = 0; i < 1000; i++) Files.createFile(directory.resolve("asset-" + i + ".pdf"));
        Files.createFile(Files.createDirectory(directory.resolve("child")).resolve("nested.pdf"));
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("download.html");
        Files.writeString(template, "<a href='" + folder.replace("/autotest-tenant", "") + "/asset-42.pdf'>Download</a>");

        boolean multiDomain = Constants.getBoolean("multiDomainEnabled");
        ConfDetails alias = new ConfDetails();
        alias.setValue("autotest-tenant");
        try (var settings = mockStatic(ConstantsV9.class)) {
            Constants.setBoolean("multiDomainEnabled", true);
            settings.when(() -> ConstantsV9.getValuesStartsWith("multiDomainAlias:")).thenReturn(List.of(alias));
            List<UnusedFile> result = FileTools.getDirFileUsage(folder.substring(1), null, false);
            assertEquals(999, result.size());
            assertFalse(result.stream().anyMatch(file -> file.getName().equals("asset-42.pdf")));
        } finally {
            Constants.setBoolean("multiDomainEnabled", multiDomain);
        }
    }
}
