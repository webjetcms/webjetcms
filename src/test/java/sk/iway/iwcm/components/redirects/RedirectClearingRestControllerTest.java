package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.UrlRedirectDB;

class RedirectClearingRestControllerTest {

    @Test
    void successfulExecutionClearsSnapshotAndRefreshesCacheOnce() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        RedirectClearingRestController controller = controller(service);
        RedirectClearingPlan plan = plan();
        when(service.analyze("example.com")).thenReturn(plan);
        when(service.execute(plan, "example.com")).thenReturn(new ExecutionResult(1, 0, 0));

        try (
            MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
            MockedStatic<UrlRedirectDB> redirects = mockStatic(UrlRedirectDB.class)
        ) {
            cloud.when(CloudToolsForCore::getDomainName).thenReturn("example.com");

            assertTrue(controller.processAction(null, "analyze"));
            assertEquals(1, controller.getAllItems(PageRequest.of(0, 10)).getTotalElements());
            assertTrue(controller.processAction(null, "execute"));

            redirects.verify(UrlRedirectDB::refreshCache, org.mockito.Mockito.times(1));
            assertEquals(0, controller.getAllItems(PageRequest.of(0, 10)).getTotalElements());
            verify(service).execute(plan, "example.com");
        }
    }

    @Test
    void failedExecutionPreservesSnapshotForRetry() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        RedirectClearingRestController controller = controller(service);
        RedirectClearingPlan plan = plan();
        when(service.analyze("example.com")).thenReturn(plan);
        when(service.execute(plan, "example.com")).thenThrow(new IllegalStateException("database failure"));

        try (MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class)) {
            cloud.when(CloudToolsForCore::getDomainName).thenReturn("example.com");

            assertTrue(controller.processAction(null, "analyze"));
            assertThrows(RuntimeException.class, () -> controller.processAction(null, "execute"));
            assertEquals(1, controller.getAllItems(PageRequest.of(0, 10)).getTotalElements());
        }
    }

    @Test
    void changingCurrentDomainInvalidatesSnapshot() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        RedirectClearingRestController controller = controller(service);
        when(service.analyze("example.com")).thenReturn(plan());

        try (MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class)) {
            cloud.when(CloudToolsForCore::getDomainName).thenReturn("example.com", "other.example");

            assertTrue(controller.processAction(null, "analyze"));
            assertEquals(0, controller.getAllItems(PageRequest.of(0, 10)).getTotalElements());
        }
    }

    private static RedirectClearingRestController controller(RedirectClearingService service) {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(anyString(), anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        RedirectClearingRestController controller = new RedirectClearingRestController(service) {
            @Override
            public Prop getProp() {
                return prop;
            }
        };
        controller.setRequest(new MockHttpServletRequest());
        return controller;
    }

    private static RedirectClearingPlan plan() {
        RedirectClearingAction action = new RedirectClearingAction(
            1L,
            ActionType.UPDATE_OPTIMIZE,
            "/old",
            "/middle",
            "/target",
            "example.com",
            301,
            null,
            null,
            null
        );
        return new RedirectClearingPlan("example.com", List.of(action), 1, 0);
    }
}
