package sk.iway.iwcm.components.memory_cleanup.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.dataDeleting.DataDeletingManager;
import sk.iway.iwcm.i18n.Prop;

/** Verifies manual heatmap cleanup routes without executing any database operation. */
class DatabaseDeleteServiceHeatMapTest {
    private static final Date FROM = Date.from(Instant.parse("2026-08-01T00:00:00Z"));
    private static final Date TO = Date.from(Instant.parse("2026-09-15T23:59:59Z"));

    /** Heatmaps always count monthly partitions; legacy statistics still follow their global switch. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void countsHeatmapPartitionsRegardlessOfGlobalPartitioning(boolean globalPartitioning) {
        DatabaseDeleteService service = spy(new DatabaseDeleteService());
        Prop prop = mock(Prop.class);
        List<DatabaseDeleteBean> entries = statisticsEntries(service, prop);
        doReturn(entries).when(service).getAllItems(prop);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DataDeletingManager> deletion = mockStatic(DataDeletingManager.class)) {
            constants.when(() -> Constants.getBoolean("statEnableTablePartitioning")).thenReturn(globalPartitioning);
            deletion.when(() -> DataDeletingManager.checkTablePartitioning("stat_clicks_v2", FROM, TO)).thenReturn(7);
            deletion.when(() -> DataDeletingManager.checkTablePartitioning("stat_views", FROM, TO)).thenReturn(9);
            deletion.when(() -> DataDeletingManager.checkData("stat_views", FROM, TO, false, -1)).thenReturn(11);

            List<DatabaseDeleteBean> result = service.getMemoryCleanupEntities(FROM, TO, prop);

            assertEquals(7, find(result, "stat_clicks_v2").getNumberOfEntriesToDelete());
            assertEquals(globalPartitioning ? 9 : 11, find(result, "stat_views").getNumberOfEntriesToDelete());
            deletion.verify(() -> DataDeletingManager.checkTablePartitioning("stat_clicks_v2", FROM, TO));
            if (globalPartitioning) {
                deletion.verify(() -> DataDeletingManager.checkTablePartitioning("stat_views", FROM, TO));
            } else {
                deletion.verify(() -> DataDeletingManager.checkData("stat_views", FROM, TO, false, -1));
            }
            deletion.verifyNoMoreInteractions();
        }
    }

    /** Manual deletion uses the same partition policy as the displayed record count. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deletesHeatmapPartitionsRegardlessOfGlobalPartitioning(boolean globalPartitioning) {
        DatabaseDeleteService service = new DatabaseDeleteService();
        List<DatabaseDeleteBean> entries = statisticsEntries(service, mock(Prop.class));
        for (DatabaseDeleteBean entry : entries) {
            entry.setFrom(FROM);
            entry.setTo(TO);
        }

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DataDeletingManager> deletion = mockStatic(DataDeletingManager.class)) {
            constants.when(() -> Constants.getBoolean("statEnableTablePartitioning")).thenReturn(globalPartitioning);

            assertTrue(service.delete(find(entries, "stat_clicks_v2")));
            assertTrue(service.delete(find(entries, "stat_views")));

            deletion.verify(() -> DataDeletingManager.deleteTablePartitioning("stat_clicks_v2", FROM, TO, true));
            if (globalPartitioning) {
                deletion.verify(() -> DataDeletingManager.deleteTablePartitioning("stat_views", FROM, TO, true));
            } else {
                deletion.verify(() -> DataDeletingManager.deleteData("stat_views", FROM, TO, false, -1, true));
            }
            deletion.verifyNoMoreInteractions();
        }
    }

    private static List<DatabaseDeleteBean> statisticsEntries(DatabaseDeleteService service, Prop prop) {
        return service.getAllItems(prop).stream()
                .filter(entry -> "stat_clicks_v2".equals(entry.getTableName()) || "stat_views".equals(entry.getTableName()))
                .toList();
    }

    private static DatabaseDeleteBean find(List<DatabaseDeleteBean> entries, String tableName) {
        return entries.stream().filter(entry -> tableName.equals(entry.getTableName())).findFirst().orElseThrow();
    }
}
