package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Publication entry points enforce node and feature boundaries and select the appropriate work. */
class BranchSchedulerTest {
    @Test void publicNodeNeverOpensDatabase() {
        try(var constants=mockStatic(Constants.class);var cluster=mockStatic(ClusterDB.class);var database=mockStatic(DBPool.class)) {
            constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
            constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
            cluster.when(ClusterDB::isPublicNode).thenReturn(true);
            BranchScheduler.main(new String[0]);
            BranchScheduler.publishRequested();
            database.verifyNoInteractions();
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

    @Test void immediateRunProcessesRequestsWithoutScanningScheduledPackages() {
        BranchService branches=mock(BranchService.class);
        BranchEntity queued=new BranchEntity();
        queued.setId(42L);queued.setPublishRequestedBy(7);queued.setRevision(6L);
        when(branches.publicationRequests()).thenReturn(List.of(queued));
        try(var access=mockStatic(BranchAccess.class);var cluster=mockStatic(ClusterDB.class);
                var readiness=mockStatic(BranchReadiness.class);var services=mockStatic(BranchService.class);
                var writers=mockConstruction(BranchPublishService.class)) {
            access.when(BranchAccess::isEnabled).thenReturn(true);
            services.when(BranchService::getInstance).thenReturn(branches);

            BranchScheduler.publishRequested();

            verify(branches).publicationRequests();
            verifyNoMoreInteractions(branches);
            verify(writers.constructed().get(0)).runRequested(42L,7,6L);
        }
    }
}
