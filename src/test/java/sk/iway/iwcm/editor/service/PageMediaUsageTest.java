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
     * Verifies that a mobile banner image and a Document Embed URL split across reads are the only candidates matched.
     *
     * @param webroot temporary web root containing the template with the embedded document
     * @throws Exception if the template or database fixtures cannot be prepared or scanned
     */
    @Test
    void preservesReferencesOutsidePageContent(@TempDir Path webroot) throws Exception {
        String folder = "/files/autotest-" + UUID.randomUUID() + "/";
        String banner = folder + "mobile.jpg";
        String document = folder + "catalogue.pdf";
        Path template = Files.createDirectories(webroot.resolve("templates")).resolve("embed.html");
        // The embed starts in one read and its encoded URL ends in the next.
        Files.writeString(template, " ".repeat(22000) + "!INCLUDE(/components/app-docsembed/embed.jsp,"
            + " ".repeat(12000) + "url=" + Tools.base64Encode(document) + ")!");

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
                    var usage = new PageMediaUsage(Set.of(banner, document, folder + "unused.pdf"), Map.of());
                    assertEquals(Set.of(banner, document), usage.referencedElsewhere(Set.of()));
                }
            } finally {
                connection.rollback();
            }
        }
    }
}
