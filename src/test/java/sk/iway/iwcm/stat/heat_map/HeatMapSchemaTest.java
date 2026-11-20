package sk.iway.iwcm.stat.heat_map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.UpdateDatabase;

/** Verifies that failed startup migrations are retried before recording success. */
class HeatMapSchemaTest {
    /** An installation with previous migration markers retries this upgrade until it succeeds. */
    @Test
    void marksStartupMigrationSuccessfulOnlyAfterAllTablesSucceed() throws Exception {
        try (MockedStatic<UpdateDatabase> updates = mockStatic(UpdateDatabase.class);
                MockedStatic<HeatMapSchema> schema = mockStatic(HeatMapSchema.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            updates.when(UpdateDatabase::updateStatClicksColumns).thenCallRealMethod();
            updates.when(() -> UpdateDatabase.isAllreadyUpdated("16.09.2026 [codex] add responsive tracking columns to stat_clicks")).thenReturn(true);
            schema.when(HeatMapSchema::upgradeExistingTables).thenThrow(new SQLException("index permission denied"));

            UpdateDatabase.updateStatClicksColumns();
            schema.verify(HeatMapSchema::upgradeExistingTables);
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(anyString()), never());

            schema.reset();
            UpdateDatabase.updateStatClicksColumns();
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(anyString()));
        }
    }

    /** Previously migrated MySQL installations retry engine conversion and record success only after it completes. */
    @Test
    void retriesEngineMigrationDespiteExistingCountMarker() throws Exception {
        int originalType = Constants.DB_TYPE;
        SQLException failure = new SQLException("engine conversion failed");
        try (MockedStatic<UpdateDatabase> updates = mockStatic(UpdateDatabase.class);
                MockedStatic<HeatMapSchema> schema = mockStatic(HeatMapSchema.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            Constants.DB_TYPE = Constants.DB_MYSQL;
            updates.when(UpdateDatabase::updateStatClickCounts).thenCallRealMethod();
            updates.when(() -> UpdateDatabase.isAllreadyUpdated("06.11.2026 [sivan] add count to monthly stat_clicks tables"))
                    .thenReturn(true);
            schema.when(HeatMapSchema::upgradeClickCounts).thenThrow(failure);

            UpdateDatabase.updateStatClickCounts();
            logger.verify(() -> Logger.error(UpdateDatabase.class, failure));
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(anyString()), never());

            schema.reset();
            UpdateDatabase.updateStatClickCounts();
            schema.verify(HeatMapSchema::upgradeClickCounts);
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate("09.11.2026 [sivan] add count and convert monthly stat_clicks tables to InnoDB"));
        } finally {
            Constants.DB_TYPE = originalType;
        }
    }
}
