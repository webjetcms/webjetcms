package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks MyISAM migration and configurable click aggregation in an isolated MariaDB catalog. */
class HeatMapAggregationDatabaseTest extends BaseWebjetTest {
    private static final Clock NOW = Clock.fixed(Instant.parse("2025-12-04T00:00:00Z"), ZoneOffset.UTC);
    private static final YearMonth OCTOBER = YearMonth.of(2025, 10);
    private static final String TABLE = "stat_clicks_2025_10";

    /**
     * Migrates a legacy table and preserves click totals when changing cell size or repeating aggregation.
     *
     * @throws SQLException if fixture setup, migration or aggregation fails
     */
    @Test
    void migratesMyIsamAndAggregatesUsingConfiguredCellSize() throws SQLException {
        try (Connection db = DBPool.getConnection(); Statement sql = db.createStatement()) {
            String product = db.getMetaData().getDatabaseProductName();
            assumeTrue(product.contains("MariaDB") || product.contains("MySQL"), "MariaDB integration fixture");
            String original = db.getCatalog();
            int originalType = Constants.DB_TYPE;
            int originalCellSize = Constants.getInt("statHeatMapCellSize", 4);
            String catalog = "autotest_heatmap_" + UUID.randomUUID().toString().replace("-", "");
            sql.execute("CREATE DATABASE " + catalog);
            try {
                db.setCatalog(catalog);
                Constants.DB_TYPE = Constants.DB_MYSQL;
                Connection borrowed = spy(db);
                doNothing().when(borrowed).close();
                try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
                    pool.when(DBPool::getConnection).thenReturn(borrowed);
                    StatNewDB.createStatTable("stat_clicks", "_2025_10");
                    sql.executeUpdate("ALTER TABLE " + TABLE + " DROP COLUMN count, ENGINE=MyISAM");
                    sql.executeUpdate("INSERT INTO " + TABLE
                            + " (domain_name,document_id,day_of_month,coordinate_mode,viewport_width,source_viewport_width,x,y) VALUES"
                            + " ('a',11,1,1,800,1200,100,200), ('a',11,1,1,800,1200,104,204), ('a',11,1,1,800,1200,105,200)");

                    HeatMapSchema.upgradeClickCounts();
                    try (ResultSet rows = sql.executeQuery("SELECT engine FROM information_schema.tables"
                            + " WHERE table_schema=DATABASE() AND table_name='" + TABLE + "'")) {
                        assertTrue(rows.next());
                        assertEquals("InnoDB", rows.getString(1));
                    }
                    assertEquals(3, scalar(db, "SELECT SUM(count) FROM " + TABLE));

                    Constants.setInt("statHeatMapCellSize", 5);
                    HeatMapAggregationCronJob.aggregate(db, OCTOBER, NOW);
                    assertEquals(2, scalar(db, "SELECT COUNT(*) FROM " + TABLE));
                    assertEquals(2, scalar(db, "SELECT count FROM " + TABLE + " WHERE x=100 AND y=200"));
                    assertEquals(3, scalar(db, "SELECT SUM(count) FROM " + TABLE));

                    Constants.setInt("statHeatMapCellSize", 10);
                    HeatMapAggregationCronJob.aggregate(db, OCTOBER, NOW);
                    HeatMapAggregationCronJob.aggregate(db, OCTOBER, NOW);
                    assertEquals(1, scalar(db, "SELECT COUNT(*) FROM " + TABLE));
                    assertEquals(3, scalar(db, "SELECT SUM(count) FROM " + TABLE));
                }
            } finally {
                if (!db.getAutoCommit()) db.rollback();
                db.setCatalog(original);
                Constants.DB_TYPE = originalType;
                Constants.setInt("statHeatMapCellSize", originalCellSize);
                sql.execute("DROP DATABASE " + catalog);
            }
        }
    }

    private static int scalar(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}
