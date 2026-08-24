package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.InitServlet;

class WebjetInitializationActionsTest {

    private final WebjetInitializationActions initializationActions = new WebjetInitializationActions();

    @Test
    void rejectedInitializedCoreStopsBackgroundServicesBeforeDestroyingDatabaseResources() {
        List<String> cleanupCalls = new ArrayList<>();

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedConstruction<InitServlet> initServletInstances = mockConstruction(
                    InitServlet.class,
                    (instance, context) -> doAnswer(invocation -> {
                        cleanupCalls.add("destroy");
                        return null;
                    }).when(instance).destroy()
                )) {
            initServlet.when(InitServlet::cleanupAfterFailedSpringInitialization).thenAnswer(invocation -> {
                cleanupCalls.add("background");
                return null;
            });

            initializationActions.cleanupAfterRejectedCoreInitialization(true);

            assertEquals(List.of("background", "destroy"), cleanupCalls);
            assertEquals(1, initServletInstances.constructed().size());
        }
    }

    @Test
    void rejectedUninitializedCoreOnlyDestroysDatabaseResources() {
        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedConstruction<InitServlet> initServletInstances = mockConstruction(InitServlet.class)) {
            initializationActions.cleanupAfterRejectedCoreInitialization(false);

            initServlet.verify(InitServlet::cleanupAfterFailedSpringInitialization, never());
            assertEquals(1, initServletInstances.constructed().size());
            org.mockito.Mockito.verify(initServletInstances.constructed().get(0)).destroy();
        }
    }
}
