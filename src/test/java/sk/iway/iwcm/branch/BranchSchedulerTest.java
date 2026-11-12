package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Scheduler boundaries must stop before any database work. */
class BranchSchedulerTest {
    @Test void publicNodeNeverOpensDatabase() {
        try(var constants=mockStatic(Constants.class);var cluster=mockStatic(ClusterDB.class);var database=mockStatic(DBPool.class)) {
            constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
            constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            cluster.when(ClusterDB::isPublicNode).thenReturn(true);
            BranchScheduler.main(new String[0]);database.verifyNoInteractions();
        }
    }

    @Test void disabledFeatureNeverOpensDatabase() {
        try(var constants=mockStatic(Constants.class);var database=mockStatic(DBPool.class)) {
            BranchScheduler.main(new String[0]);database.verifyNoInteractions();
        }
    }

    @Test void sharedDomainModeNeverOpensDatabase() {
        try(var constants=mockStatic(Constants.class);var database=mockStatic(DBPool.class)) {
            constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
            BranchScheduler.main(new String[0]);database.verifyNoInteractions();
        }
    }

    @Test void interruptedDelayPreservesInterruptAndNeverOpensDatabase() {
        try(var constants=mockStatic(Constants.class);var cluster=mockStatic(ClusterDB.class);var database=mockStatic(DBPool.class)) {
            constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
            constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            Thread.currentThread().interrupt();
            try {
                BranchScheduler.main(new String[0]);assertTrue(Thread.currentThread().isInterrupted());database.verifyNoInteractions();
            }finally{Thread.interrupted();}
        }
    }
}
