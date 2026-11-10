package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises click-count migration with the same schema resolution as unqualified report queries. */
class HeatMapSchemaVisibilityDatabaseTest extends BaseWebjetTest {
    private static final YearMonth OCTOBER = YearMonth.of(2025, 10);
    private static final YearMonth NOVEMBER = YearMonth.of(2025, 11);

    /** The second search-path schema supplies missing partitions and can contain a shadowed partition. */
    @Test
    void followsPostgresqlSearchPath() throws SQLException {
        try (Connection db = DBPool.getConnection(); Statement sql = db.createStatement()) {
            assumeTrue("PostgreSQL".equals(db.getMetaData().getDatabaseProductName()));
            String archive = "hm_" + UUID.randomUUID().toString().replace("-", "");
            String first = archive + "_first";
            db.setAutoCommit(false);
            try {
                sql.execute("CREATE SCHEMA " + first);
                sql.execute("CREATE SCHEMA " + archive);
                sql.execute("SET LOCAL search_path TO " + first + ", " + archive);
                verifyMigration(db, sql, first, archive);
            } finally {
                db.rollback();
            }
        }
    }

    /** A user with its own default schema can also read partitions owned by dbo. */
    @Test
    void followsSqlServerDefaultSchemaAndDboFallback() throws SQLException {
        try (Connection db = DBPool.getConnection(); Statement sql = db.createStatement()) {
            assumeTrue(db.getMetaData().getDatabaseProductName().contains("SQL Server"));
            String original = db.getCatalog();
            String catalog = "autotest_heatmap_" + UUID.randomUUID().toString().replace("-", "");
            sql.execute("CREATE DATABASE " + catalog);
            try {
                db.setCatalog(catalog);
                sql.execute("CREATE SCHEMA hm_first");
                sql.execute("CREATE USER hm_user WITHOUT LOGIN WITH DEFAULT_SCHEMA = hm_first");
                sql.execute("ALTER ROLE db_owner ADD MEMBER hm_user");
                sql.execute("EXECUTE AS USER = 'hm_user'");
                try {
                    verifyMigration(db, sql, "hm_first", "dbo");
                } finally {
                    sql.execute("REVERT");
                }
            } finally {
                db.setCatalog(original);
                sql.execute("DROP DATABASE " + catalog);
            }
        }
    }

    private static void verifyMigration(Connection db, Statement sql, String first, String fallback) throws SQLException {
        String october = "stat_clicks_2025_10";
        String november = "stat_clicks_2025_11";
        for (String table : List.of(fallback + "." + october, first + "." + november, fallback + "." + november)) {
            sql.execute("CREATE TABLE " + table + " (document_id INT, domain_name VARCHAR(20), day_of_month INT)");
        }
        sql.executeUpdate("INSERT INTO " + fallback + "." + october + " VALUES (11, 'example', 1)");
        sql.executeUpdate("INSERT INTO " + first + "." + november + " VALUES (12, 'example', 1)");
        sql.executeUpdate("INSERT INTO " + fallback + "." + november + " VALUES (13, 'example', 1)");

        assertEquals(List.of(OCTOBER, NOVEMBER), HeatMapSchema.existingMonths(db));
        Connection borrowed = spy(db);
        doNothing().when(borrowed).close();
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(borrowed);
            HeatMapSchema.upgradeClickCounts();
            assertEquals(Map.of(11, 1L, 12, 1L), HeatMapStorage.getPageCounts("example", OCTOBER.atDay(1), NOVEMBER.atEndOfMonth()));
        }
        try (ResultSet shadowed = sql.executeQuery("SELECT * FROM " + fallback + "." + november + " WHERE 1=0")) {
            assertEquals(3, shadowed.getMetaData().getColumnCount());
        }
    }
}
