package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sk.iway.iwcm.stat.heat_map.HeatMapCoordinateMode.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.stat.StatWriteBuffer;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises migration, buffered persistence, cross-month grouping and PNG isolation on an isolated MariaDB schema. */
class HeatMapContainerDatabaseTest extends BaseWebjetTest {
    @TempDir
    Path cache;

    /** Original rows survive migration and container groups never share document-coordinate tiles. */
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
                        sql.execute("CREATE TABLE stat_clicks" + suffix + " (stat_click_id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "event_id CHAR(32),domain_name VARCHAR(255),viewport_width INT NOT NULL DEFAULT 1920,"
                                + "document_id INT,day_of_month INT,x INT,y INT)");
                    }
                    sql.execute("INSERT INTO stat_clicks_2026_9 (document_id,day_of_month,viewport_width,x,y) VALUES (11,1,1140,900,350)");
                }
                Connection borrowed = spy(database);
                doNothing().when(borrowed).close();
                try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
                        MockedStatic<StatNewDB> partitions = mockStatic(StatNewDB.class)) {
                    pool.when(DBPool::getConnection).thenReturn(borrowed);
                    partitions.when(() -> StatNewDB.getTableSuffix(eq("stat_clicks"), anyLong(), anyLong()))
                            .thenReturn(new String[] {"_2026_9", "_2026_10"});
                    HeatMapSchema.upgradeExistingTables();
                    HeatMapSchema.upgradeExistingTables();
                    record(1, 9, 1223, 487, 1140, "autotest.example");
                    record(2, 10, 1369, 487, 1140, "autotest.example");
                    record(3, 10, 1369, 487, 1140, "autotest.example");
                    record(3, 10, 1369, 487, 1140, "autotest.example");
                    record(4, 10, 1140, 900, 0, "autotest.example");
                    record(5, 10, 390, 100, 0, "autotest.example");
                    record(6, 10, 1369, 487, 1140, "other.example");
                    StatWriteBuffer.main(new String[0]);
                    LocalDate from = LocalDate.of(2026, 9, 1);
                    LocalDate to = LocalDate.of(2026, 10, 31);
                    assertEquals(List.of(new HeatMapStorage.WidthCount(1140, CONTAINER, 3, 1369),
                            new HeatMapStorage.WidthCount(1140, DOCUMENT, 2, 1140),
                            new HeatMapStorage.WidthCount(390, DOCUMENT, 1, 390)),
                            HeatMapStorage.getWidths("autotest.example", 11, from, to));
                    try (MockedStatic<Tools> paths = mockStatic(Tools.class)) {
                        paths.when(() -> Tools.getRealPath("/WEB-INF/tmp/heat_map/")).thenReturn(cache.toString());
                        byte[] container = HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, CONTAINER, 0, 0);
                        byte[] document = HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, DOCUMENT, 0, 0);
                        assertTrue((ImageIO.read(new ByteArrayInputStream(container)).getRGB(487, 350) >>> 24) > 0);
                        assertEquals(0, ImageIO.read(new ByteArrayInputStream(document)).getRGB(487, 350) >>> 24);
                        assertTrue((ImageIO.read(new ByteArrayInputStream(document)).getRGB(900, 350) >>> 24) > 0);
                        assertArrayEquals(container, HeatMapStorage.getTile("autotest.example", 11, from, to, 1140, CONTAINER, 0, 0));
                    }
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

    private static void record(int id, int month, int viewport, int x, int container, String domain) {
        long epoch = LocalDate.of(2026, month, 1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        HeatMapStorage.record(new HeatMapEvent(String.format("%032x", id), 11, viewport, epoch, x, 350, container), domain);
    }
}
