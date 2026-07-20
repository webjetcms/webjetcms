package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.redirects.RedirectClearingPlanCoordinator.OperationInProgressException;
import sk.iway.iwcm.components.redirects.RedirectClearingPlanCoordinator.OperationType;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

class RedirectClearingRestControllerTest extends RedirectClearingTestSupport {

    @Test
    void analysisPassesCheckboxValueAndReturnsCachedScopeSummary() {
        RedirectClearingPlanCoordinator coordinator = mock(RedirectClearingPlanCoordinator.class);
        RedirectClearingRestController controller = controller(coordinator);
        ((MockHttpServletRequest) controller.getRequest()).setParameter("customData", "true");
        RedirectClearingPlan plan = plan(17, true);
        when(coordinator.analyze(17, true)).thenReturn(plan);
        when(coordinator.getPlan(17)).thenReturn(plan);

        try (MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class)) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(17);

            assertTrue(controller.processAction(null, "analyze"));
            Page<RedirectClearingAction> page = controller.getAllItems(PageRequest.of(0, 10));

            verify(coordinator).analyze(17, true);
            Map<String, Long> summary = ((DatatablePageImpl<?>) page).getSummary();
            assertEquals(1L, summary.get("planAvailable"));
            assertEquals(1L, summary.get("includeUnnamed"));
        }
    }

    @Test
    void reportsTheOperationWhichBlocksAnAction() {
        RedirectClearingPlanCoordinator coordinator = mock(RedirectClearingPlanCoordinator.class);
        RedirectClearingRestController controller = controller(coordinator);
        when(coordinator.analyze(5, false))
            .thenThrow(new OperationInProgressException(OperationType.EXECUTE));

        try (MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class)) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(5);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.processAction(null, "analyze"));
            assertEquals("components.redirect.clearing.busyExecute", exception.getMessage());
        }
    }

    private static RedirectClearingRestController controller(RedirectClearingPlanCoordinator coordinator) {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(anyString(), anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        RedirectClearingRestController controller = new RedirectClearingRestController(coordinator) {
            @Override
            public Prop getProp() {
                return prop;
            }
        };
        controller.setRequest(new MockHttpServletRequest());
        return controller;
    }

}
