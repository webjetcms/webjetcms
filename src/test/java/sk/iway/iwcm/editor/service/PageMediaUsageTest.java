package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests how page file cleanup finds references in component data and template files. */
class PageMediaUsageTest extends BaseWebjetTest {
    /**
     * Verifies references in a mobile banner, a split Document Embed URL and both HTML Embed formats.
     *
     * @param webroot temporary web root containing the template with the embedded document
     * @throws Exception if the template or database fixtures cannot be prepared or scanned
     */
    @Test
    void preservesReferencesOutsidePageContent(@TempDir Path webroot) throws Exception {
        String folder = "/files/autotest-" + UUID.randomUUID() + "/";
        String banner = folder + "mobile.jpg";
        String document = folder + "catalogue.pdf";
        String image = folder + "map.jpg";
        String legacyImage = folder + "legacy.jpg";
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("embed.html");
        // The embed starts in one read and its encoded URL ends in the next.
        Files.writeString(template, " ".repeat(22000) + "!INCLUDE(/components/app-docsembed/embed.jsp,"
            + " ".repeat(12000) + "url=" + Tools.base64Encode(document) + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode("<img src='" + image + "'>") + ")!"
            + "!INCLUDE(/components/app-htmlembed/embed.jsp, html=" + Tools.base64Encode("utf8:" + Tools.URLEncode("<img src='" + legacyImage + "'>")) + ")!");

        try (Connection connection = DBPool.getConnection()) {
            connection.setAutoCommit(false);
            var dataSource = new SingleConnectionDataSource(connection, true);
            try {
                new JdbcTemplate(dataSource).update(
                    "INSERT INTO banner_banners (banner_id,banner_type,active,image_link_mobile) VALUES (?,?,?,?)",
                    PkeyGenerator.getNextValue("banner_banners"), 4, true, banner);
                try (var pool = mockStatic(DBPool.class); var paths = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
                    pool.when(DBPool::getConnection).thenReturn(dataSource.getConnection());
                    paths.when(() -> Tools.getRealPath(anyString())).thenAnswer(call ->
                        webroot.resolve(call.<String>getArgument(0).substring(1)).toString());
                    var usage = new PageMediaUsage(Set.of(banner, document, image, legacyImage, folder + "unused.pdf"), Map.of());
                    assertEquals(Set.of(banner, document, image, legacyImage), usage.referencedElsewhere(Set.of()));
                }
            } finally {
                connection.rollback();
            }
        }
    }
}
