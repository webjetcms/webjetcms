package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sk.iway.iwcm.stat.heat_map.HeatMapCoordinateMode.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.StatWriteBuffer;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.ReportRange;
import sk.iway.iwcm.system.spring.events.DocumentPublishEvent;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises migration, buffered persistence, cross-month grouping and PNG isolation on an isolated MariaDB schema. */
class HeatMapContainerDatabaseTest extends BaseWebjetTest {
    @TempDir
    Path cache;

    /**
     * Verifies that repeated schema upgrades preserve legacy clicks and keep container and document tiles separate.
     *
     * @throws Exception if fixture setup, event persistence or rendering fails
     */
    @Test
    void persistsAndRendersContainerGroupsWithoutMixingLegacyClicks() throws Exception {
        assumeTrue(StatWriteBuffer.size() == 0, "This database fixture needs an empty statistics buffer");
        try (Connection database = DBPool.getConnection()) {
            String product = database.getMetaData().getDatabaseProductName();
            assumeTrue(product.contains("MariaDB") || product.contains("MySQL"), "MariaDB integration fixture");
            String originalCatalog = database.getCatalog();
            String catalog = "autotest_heatmap_" + UUID.randomUUID().toString().replace("-", "");
            try (Statement sql = database.createStatement()) {
                sql.execute("CREATE DATABASE " + catalog);
            }
            int originalType = Constants.DB_TYPE;
            try {
                database.setCatalog(catalog);
                Constants.DB_TYPE = Constants.DB_MYSQL;
                try (Statement sql = database.createStatement()) {
                    sql.execute("CREATE TABLE documents (doc_id INT, group_id INT)");
                    sql.execute("CREATE TABLE `groups` (group_id INT, domain_name VARCHAR(255))");
                    sql.execute("INSERT INTO documents VALUES (11, 1)");
                    sql.execute("INSERT INTO `groups` VALUES (1, 'autotest.example')");
                    for (String suffix : List.of("_2026_9", "_2026_10")) {
                        String rowFormat = "_2026_9".equals(suffix) ? "COMPACT" : "REDUNDANT";
                        sql.execute("CREATE TABLE stat_clicks" + suffix + " (stat_click_id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "event_id CHAR(32),domain_name VARCHAR(255),viewport_width INT NOT NULL DEFAULT 1920,"
                                + "document_id INT,day_of_month INT,x INT,y INT) ENGINE=InnoDB ROW_FORMAT="
                                + rowFormat + " DEFAULT CHARSET=utf8mb4");
                    }
                    sql.execute("INSERT INTO stat_clicks_2026_9 (document_id,day_of_month,viewport_width,x,y) VALUES (11,1,1140,900,350)");
                }
                Connection borrowed = spy(database);
                doNothing().when(borrowed).close();
                try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
                        MockedStatic<StatNewDB> partitions = mockStatic(StatNewDB.class)) {
                    pool.when(DBPool::getConnection).thenReturn(borrowed);
                    pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(borrowed);
                    partitions.when(() -> StatNewDB.getTableSuffix(eq("stat_clicks"), anyLong(), anyLong()))
                            .thenReturn(new String[] {"_2026_9", "_2026_10"});
                    HeatMapSchema.upgradeExistingTables();
                    HeatMapSchema.upgradeExistingTables();
                    HeatMapSchema.upgradeClickCounts();
                    HeatMapSchema.upgradeClickCounts();
                    record(1, 9, 1223, 487, 1140, "autotest.example");
                    record(2, 10, 1369, 487, 1140, "autotest.example");
                    record(3, 10, 1369, 487, 1140, "autotest.example");
                    record(3, 10, 1369, 487, 1140, "autotest.example");
                    record(4, 10, 1140, 900, 0, "autotest.example");
                    record(5, 10, 390, 100, 0, "autotest.example");
                    record(6, 10, 1369, 487, 1140, "other.example");
                    StatWriteBuffer.main(new String[0]);
                    HeatMapSchema.upgradeReportIndexes();
                    HeatMapSchema.upgradeReportIndexes();
                    for (String suffix : List.of("_2026_9", "_2026_10")) {
                        Set<String> indexes = new HashSet<>();
                        try (ResultSet rows = database.getMetaData().getIndexInfo(
                                database.getCatalog(), null, "stat_clicks" + suffix, false, false)) {
                            while (rows.next()) indexes.add(rows.getString("INDEX_NAME"));
                        }
                        assertTrue(indexes.containsAll(List.of("hm_width" + suffix, "hm_tile" + suffix, "hm_page_v2" + suffix)));
                    }
                    // The report starts before the first monthly table exists.
                    LocalDate from = LocalDate.of(2026, 8, 1);
                    LocalDate to = LocalDate.of(2026, 10, 31);
                    assertEquals(List.of(new HeatMapStorage.WidthCount(1140, CONTAINER, 3, 1369),
                            new HeatMapStorage.WidthCount(1140, DOCUMENT, 2, 1140),
                            new HeatMapStorage.WidthCount(390, DOCUMENT, 1, 390)),
                            HeatMapStorage.getWidths("autotest.example", 11, from, to));
                    assertEquals(new HeatMapStorage.AreaCount(3, 3),
                            HeatMapStorage.getAreaCount("autotest.example", 11, from, to, 1140, CONTAINER, 487, 350, 488, 351));
                    assertEquals(new HeatMapStorage.AreaCount(0, 3),
                            HeatMapStorage.getAreaCount("autotest.example", 11, from, to, 1140, CONTAINER, 488, 350, 500, 351));
                    assertEquals(new HeatMapStorage.AreaCount(0, 3),
                            HeatMapStorage.getAreaCount("autotest.example", 11, from, to, 1140, CONTAINER, 480, 340, 487, 350));
                    assertEquals(new HeatMapStorage.AreaCount(0, 2),
                            HeatMapStorage.getAreaCount("autotest.example", 11, from, to, 1140, DOCUMENT, 487, 350, 488, 351));
                    assertEquals(new HeatMapStorage.AreaCount(1, 1),
                            HeatMapStorage.getAreaCount("other.example", 11, from, to, 1140, CONTAINER, 487, 350, 488, 351));
                    assertEquals(new HeatMapStorage.AreaCount(0, 0),
                            HeatMapStorage.getAreaCount("autotest.example", 12, from, to, 1140, CONTAINER, 487, 350, 488, 351));
                    assertEquals(new HeatMapStorage.AreaCount(1, 1),
                            HeatMapStorage.getAreaCount("autotest.example", 11, from, LocalDate.of(2026, 9, 30), 1140, CONTAINER, 487, 350, 488, 351));
                    try (MockedStatic<Tools> paths = mockStatic(Tools.class)) {
                        paths.when(() -> Tools.getRealPath("/WEB-INF/tmp/heat_map/")).thenReturn(cache.toString());
                        assertEquals(3, HeatMapStorage.getMaximumDensity("autotest.example", 11, from, to, 1140, CONTAINER));
                        assertEquals(2, HeatMapStorage.getMaximumDensity("autotest.example", 11, from, to, 1140, DOCUMENT));
                        assertEquals(1, HeatMapStorage.getMaximumDensity("other.example", 11, from, to, 1140, CONTAINER));
                        assertEquals(1, HeatMapStorage.getMaximumDensity("autotest.example", 11, from, LocalDate.of(2026, 9, 30), 1140, CONTAINER));
                        byte[] container = HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, CONTAINER, 0, 0);
                        byte[] document = HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, DOCUMENT, 0, 0);
                        assertTrue((ImageIO.read(new ByteArrayInputStream(container)).getRGB(487, 350) >>> 24) > 0);
                        assertEquals(0, ImageIO.read(new ByteArrayInputStream(document)).getRGB(487, 350) >>> 24);
                        assertTrue((ImageIO.read(new ByteArrayInputStream(document)).getRGB(900, 350) >>> 24) > 0);
                        assertArrayEquals(container, HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, CONTAINER, 0, 0));
                        verifySharedPublicationDay(database);
                        verifyVersionDensity(database);
                    }
                    verifyHistoryProjectionAndCache(database);
                }
            } finally {
                Constants.DB_TYPE = originalType;
                database.setCatalog(originalCatalog);
                try (Statement sql = database.createStatement()) {
                    sql.execute("DROP DATABASE " + catalog);
                }
            }
        }
    }

    /** Exercises the production history query, including remaps, and refreshes cached intervals after history changes. */
    private static void verifyHistoryProjectionAndCache(Connection database) throws Exception {
        int docId = 41;
        long start = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date[] period = { new Date(start), new Date(start + 59999) };
        try (Statement sql = database.createStatement()) {
            sql.execute("CREATE TABLE documents_history (history_id INT, doc_id INT, save_date DATETIME, data LONGTEXT,"
                    + " approve_date DATETIME, approved_by INT DEFAULT 0, disapproved_by INT, awaiting_approve VARCHAR(16),"
                    + " publicable BOOLEAN, is_delete BOOLEAN, sync_status INT, available BOOLEAN DEFAULT 1,"
                    + " external_link VARCHAR(255), publish_after_start BOOLEAN, publish_start DATETIME,"
                    + " disable_after_end BOOLEAN, publish_end DATETIME)");
        }
        HeatMapHistoryService service = spy(new HeatMapHistoryService(null, null));
        DocDetails page = new DocDetails();
        page.setDocId(docId);
        var published = new WebjetEvent<>(new DocumentPublishEvent(page), WebjetEventType.ON_PUBLISH);
        service.pagePublished(published);
        try (PreparedStatement insert = database.prepareStatement(
                "INSERT INTO documents_history (doc_id, history_id, save_date, data) VALUES (?, ?, ?, ?)");
                MockedStatic<MultigroupMappingDB> mappings = mockStatic(MultigroupMappingDB.class)) {
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(docId, true)).thenReturn(docId);
            insert.setInt(1, docId);
            insert.setInt(2, 1);
            insert.setTimestamp(3, new Timestamp(start));
            insert.setString(4, "Literal examples: !remap_page(2)! and !REMAPxPAGE(2)!");
            insert.executeUpdate();

            var full = ReportRange.of(period);
            assertEquals(full, service.versionRange(docId, period, 1));
            assertEquals(full, service.versionRange(docId, period, 1));
            verify(service, times(1)).loadHistory(docId, period[1].getTime());

            insert.setInt(2, 2);
            insert.setTimestamp(3, new Timestamp(start + 10000));
            insert.setString(4, "x".repeat(9000) + "!REMAP_PAGE(42)!");
            insert.executeUpdate();
            service.pagePublished(published);
            assertEquals(new ReportRange(start, start + 10000), service.versionRange(docId, period, 1));
            assertThrows(ResponseStatusException.class, () -> service.versionRange(docId, period, 2));

            try (Statement sql = database.createStatement()) {
                sql.executeUpdate("DELETE FROM documents_history WHERE history_id=2");
            }
            HeatMapHistoryService.historyDeleted(docId);
            assertEquals(full, service.versionRange(docId, period, 1));
            verify(service, times(3)).loadHistory(docId, period[1].getTime());
        } finally {
            service.pagePublished(published);
        }
    }

    /** Both versions include every click on a shared publication day, while the full report counts each row once. */
    private static void verifySharedPublicationDay(Connection database) throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 1);
        long start = day.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long boundary = start + 43200;
        for (int i = 0; i < 2; i++) {
            HeatMapStorage.record(new HeatMapEvent(String.format("%032x", 20 + i), 22, 800,
                    boundary - 1 + i, 150 + 400 * i, 150), "autotest.example");
        }
        StatWriteBuffer.main(new String[0]);
        try (Statement sql = database.createStatement()) {
            String columns = " (domain_name,document_id,day_of_month,viewport_width,x,y) VALUES ";
            sql.execute("INSERT INTO stat_clicks_2026_9" + columns + "('autotest.example',22,30,800,700,150)");
            sql.execute("INSERT INTO stat_clicks_2026_10" + columns
                    + "('autotest.example',22,1,800,700,150), ('autotest.example',22,2,800,700,150)");
        }
        LocalDate first = day.minusDays(1);
        LocalDate last = day.plusDays(1);
        assertEquals(5, HeatMapStorage.getWidths("autotest.example", 22, first, last).get(0).clicks());
        var before = new ReportRange(first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), boundary * 1000);
        var after = new ReportRange(boundary * 1000, last.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        for (var version : List.of(before, after)) {
            assertEquals(4, HeatMapStorage.getWidths("autotest.example", 22, version.firstDay(), version.lastDay()).get(0).clicks());
            byte[] png = HeatMapStorage.getTile("autotest.example", 22, version.firstDay(), version.lastDay(), 800, DOCUMENT, 0, 0);
            var tile = ImageIO.read(new ByteArrayInputStream(png));
            assertTrue((tile.getRGB(150, 150) >>> 24) > 0);
            assertTrue((tile.getRGB(550, 150) >>> 24) > 0);
            assertTrue((tile.getRGB(700, 150) >>> 24) > 0);
        }
    }

    /** A quiet version reaches red on its own scale, independently of a busier version and cached aggregate tiles. */
    private static void verifyVersionDensity(Connection database) throws Exception {
        try (Statement sql = database.createStatement()) {
            String columns = " (domain_name,document_id,day_of_month,viewport_width,x,y,count) VALUES ";
            sql.execute("INSERT INTO stat_clicks_2026_9" + columns + "('autotest.example',23,1,800,100,100,50)");
            sql.execute("INSERT INTO stat_clicks_2026_10" + columns + "('autotest.example',23,1,800,500,100,5000)");
        }
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate versionEnd = from.withDayOfMonth(30);
        LocalDate to = LocalDate.of(2026, 10, 31);
        assertEquals(5000, HeatMapStorage.getMaximumDensity("autotest.example", 23, from, to, 800, DOCUMENT));
        assertEquals(50, HeatMapStorage.getMaximumDensity("autotest.example", 23, from, versionEnd, 800, DOCUMENT));
        var aggregate = ImageIO.read(new ByteArrayInputStream(HeatMapStorage.getTile("autotest.example", 23, from, to, 800, DOCUMENT, 0, 0)));
        var version = ImageIO.read(new ByteArrayInputStream(HeatMapStorage.getTile("autotest.example", 23, from, versionEnd, 800, DOCUMENT, 0, 0)));
        assertNotEquals(0xb20000, aggregate.getRGB(100, 100) & 0xffffff);
        assertEquals(0xb20000, version.getRGB(100, 100) & 0xffffff);
        assertEquals(0, version.getRGB(500, 100));
    }

    private static void record(int id, int month, int viewport, int x, int container, String domain) {
        long epoch = LocalDate.of(2026, month, 1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        HeatMapStorage.record(new HeatMapEvent(String.format("%032x", id), 11, viewport, epoch, x, 350, container), domain);
    }
}
