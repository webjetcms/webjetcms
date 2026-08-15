package sk.iway.iwcm.system.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
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
        String command = ClusterDB.getCronTaskCommand("node-main-01", 42L);

        assertEquals("crontab-node-main-01-42", command);
        assertEquals("node-main-01", ClusterDB.getCronTaskNode(command));
        assertEquals(42L, ClusterDB.getCronTaskId(command));

        assertEquals("crontab-all-7", ClusterDB.getCronTaskCommand("", 7L));
        assertEquals(-1L, ClusterDB.getCronTaskId("crontab-node-main-invalid"));
        assertEquals(-1L, ClusterDB.getCronTaskId("invalid-command"));
    }

    @Test
    void resolvesTargetNodesForAutoAndExplicitClusterModes() {
        assertEquals(List.of("node-main-01"), ClusterDB.getCronTaskTargetNodes("node-main-01", "auto"));
        assertEquals(List.of("auto"), ClusterDB.getCronTaskTargetNodes("all", "auto"));
        assertEquals(List.of("auto"), ClusterDB.getCronTaskTargetNodes("all-admin", "auto"));
        assertEquals(List.of("auto"), ClusterDB.getCronTaskTargetNodes("all-public", "auto"));

        List<String> explicitNodes = List.of("node-admin", "node-public");
        assertEquals(List.of("node-public"), ClusterDB.getCronTaskTargetNodes("node-public", "node-admin,node-public"));
        assertEquals(explicitNodes, ClusterDB.getCronTaskTargetNodes("all", "node-admin,node-public"));
        assertEquals(explicitNodes, ClusterDB.getCronTaskTargetNodes("all-admin", "node-admin,node-public"));
        assertEquals(explicitNodes, ClusterDB.getCronTaskTargetNodes("all-public", "node-admin,node-public"));
        assertEquals(explicitNodes, ClusterDB.getCronTaskTargetNodes(null, "node-admin,node-public"));
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
