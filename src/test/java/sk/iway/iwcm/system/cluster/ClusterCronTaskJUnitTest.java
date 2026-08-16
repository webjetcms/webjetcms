package sk.iway.iwcm.system.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.cron.CronDB;
import sk.iway.iwcm.system.cron.CronFacade;
import sk.iway.iwcm.system.cron.CronTask;

class ClusterCronTaskJUnitTest {

    private String originalClusterNames;
    private String originalNodeName;
    private String originalNodeType;

    @BeforeEach
    void saveClusterConfiguration() {
        originalClusterNames = Constants.getString("clusterNames");
        originalNodeName = Constants.getString("clusterMyNodeName");
        originalNodeType = Constants.getString("clusterMyNodeType");
    }

    @AfterEach
    void restoreClusterConfiguration() {
        Constants.setString("clusterNames", originalClusterNames);
        Constants.setString("clusterMyNodeName", originalNodeName);
        Constants.setString("clusterMyNodeType", originalNodeType);
    }

    @Test
    void encodesAndDecodesCronTaskCommand() {
        String command = CronTaskClusterCommand.create("node-main-01", 42L).encode();
        CronTaskClusterCommand parsedCommand = CronTaskClusterCommand.parse(command);

        assertEquals("crontab-node-main-01-42", command);
        assertEquals("node-main-01", parsedCommand.getConfiguredNode());
        assertEquals(42L, parsedCommand.getTaskId());

        assertEquals("crontab-all-7", CronTaskClusterCommand.create("", 7L).encode());
        assertNull(CronTaskClusterCommand.parse("crontab-node-main-invalid"));
        assertNull(CronTaskClusterCommand.parse("invalid-command"));
    }

    @Test
    void resolvesTargetNodesForAutoAndExplicitClusterModes() {
        assertEquals(List.of("node-main-01"), CronTaskClusterCommand.create("node-main-01", 42L).resolveTargetNodes("auto"));
        assertEquals(List.of("auto"), CronTaskClusterCommand.create("all", 42L).resolveTargetNodes("auto"));
        assertEquals(List.of("auto"), CronTaskClusterCommand.create("all-admin", 42L).resolveTargetNodes("auto"));
        assertEquals(List.of("auto"), CronTaskClusterCommand.create("all-public", 42L).resolveTargetNodes("auto"));

        List<String> explicitNodes = List.of("node-admin", "node-public");
        assertEquals(List.of("node-public"), CronTaskClusterCommand.create("node-public", 42L).resolveTargetNodes("node-admin,node-public"));
        assertEquals(explicitNodes, CronTaskClusterCommand.create("all", 42L).resolveTargetNodes("node-admin,node-public"));
        assertEquals(explicitNodes, CronTaskClusterCommand.create("all-admin", 42L).resolveTargetNodes("node-admin,node-public"));
        assertEquals(explicitNodes, CronTaskClusterCommand.create("all-public", 42L).resolveTargetNodes("node-admin,node-public"));
        assertEquals(explicitNodes, CronTaskClusterCommand.create(null, 42L).resolveTargetNodes("node-admin,node-public"));
    }

    @Test
    void returnsDatabaseWriteResult() {
        Constants.setString("clusterNames", "node-admin,node-public");
        Constants.setString("clusterMyNodeName", "node-admin");

        try (MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) ->
            when(query.executeInTransaction(anyList(), anyList())).thenAnswer(invocation -> {
                List<String> sqlCommands = invocation.getArgument(0);
                List<Object[]> sqlParameters = invocation.getArgument(1);

                assertEquals(2, sqlCommands.size());
                assertEquals(2, sqlParameters.size());
                assertEquals("node-admin", sqlParameters.get(0)[0]);
                assertEquals("node-public", sqlParameters.get(1)[0]);
                assertEquals("crontab-all-42", sqlParameters.get(0)[1]);
                assertEquals("crontab-all-42", sqlParameters.get(1)[1]);
                return true;
            }))) {
            assertTrue(ClusterDB.addCronTask("all", 42L));
            verify(queries.constructed().get(0)).executeInTransaction(anyList(), anyList());
        }
    }

    @Test
    void returnsFalseWhenDatabaseWriteFails() {
        Constants.setString("clusterNames", "auto");
        Constants.setString("clusterMyNodeName", "node-main-01");

        try (MockedConstruction<SimpleQuery> ignored = mockConstruction(SimpleQuery.class, (query, context) ->
            when(query.executeInTransaction(anyList(), anyList())).thenThrow(new IllegalStateException("DB write failed")))) {
            assertFalse(ClusterDB.addCronTask("all", 42L));
        }
    }

    @Test
    void matchesCurrentClusterNodeAndNodeType() {
        Constants.setString("clusterNames", "node-admin,node-public");
        Constants.setString("clusterMyNodeName", "node-admin");
        Constants.setString("clusterMyNodeType", "admin");

        assertTrue(CronDB.isCronTaskForCurrentNode(null));
        assertTrue(CronDB.isCronTaskForCurrentNode("all"));
        assertTrue(CronDB.isCronTaskForCurrentNode("node-admin"));
        assertTrue(CronDB.isCronTaskForCurrentNode("all-admin"));
        assertFalse(CronDB.isCronTaskForCurrentNode("node-public"));
        assertFalse(CronDB.isCronTaskForCurrentNode("all-public"));

        Constants.setString("clusterMyNodeType", "public");
        assertTrue(CronDB.isCronTaskForCurrentNode("all-public"));
        assertFalse(CronDB.isCronTaskForCurrentNode("all-admin"));

        Constants.setString("clusterNames", "");
        assertTrue(CronDB.isCronTaskForCurrentNode("another-node"));
    }

    @Test
    void doesNotDeduplicateCronTaskCommands() {
        Set<String> alreadyExecuted = new HashSet<>();

        assertFalse(ClusterRefresher.shouldSkipDuplicate(alreadyExecuted, "crontab-all-42"));
        assertFalse(ClusterRefresher.shouldSkipDuplicate(alreadyExecuted, "crontab-all-42"));
        assertFalse(ClusterRefresher.shouldSkipDuplicate(alreadyExecuted, "sk.iway.iwcm.Cache-test"));
        assertTrue(ClusterRefresher.shouldSkipDuplicate(alreadyExecuted, "sk.iway.iwcm.Cache-test"));
    }

    @Test
    void runsExistingTaskOnMatchingNode() throws Exception {
        ClusterRefresher refresher = mock(ClusterRefresher.class, CALLS_REAL_METHODS);
        CronTask task = new CronTask();
        task.setId(42L);
        CronFacade facade = mock(CronFacade.class);

        try (MockedStatic<CronDB> cronDb = mockStatic(CronDB.class);
             MockedStatic<CronFacade> cronFacade = mockStatic(CronFacade.class)) {
            cronDb.when(() -> CronDB.getById(42L)).thenReturn(task);
            cronDb.when(() -> CronDB.isCronTaskForCurrentNode("node-main-01")).thenReturn(true);
            cronFacade.when(CronFacade::getInstance).thenReturn(facade);

            refresher.runCronTask("crontab-node-main-01-42");

            verify(facade).runSimpleTaskOnce(task);
        }
    }

    @Test
    void ignoresMissingTaskAndNonMatchingNode() {
        ClusterRefresher refresher = mock(ClusterRefresher.class, CALLS_REAL_METHODS);
        CronTask task = new CronTask();
        task.setId(42L);

        try (MockedStatic<CronDB> cronDb = mockStatic(CronDB.class);
             MockedStatic<CronFacade> cronFacade = mockStatic(CronFacade.class)) {
            cronDb.when(() -> CronDB.getById(42L)).thenReturn(null, task);
            cronDb.when(() -> CronDB.isCronTaskForCurrentNode("all-public")).thenReturn(false);

            refresher.runCronTask("crontab-all-public-42");
            refresher.runCronTask("crontab-all-public-42");

            cronFacade.verifyNoInteractions();
        }
    }
}
