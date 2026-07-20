package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;

class RedirectClearingPlanCoordinatorTest extends RedirectClearingTestSupport {

    @Test
    void analyzesAndCachesDomainPlan() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        Cache cache = mock(Cache.class);
        RedirectClearingPlan plan = plan(23, false);
        when(service.analyze(23, false)).thenReturn(plan);
        RedirectClearingPlanCoordinator coordinator = new RedirectClearingPlanCoordinator(service, cache);

        assertSame(plan, coordinator.analyze(23, false));

        verify(cache).setObjectSeconds(
            RedirectClearingPlanCoordinator.getPlanCacheKey(23),
            plan,
            RedirectClearingPlanCoordinator.PLAN_CACHE_SECONDS,
            false
        );
    }

    @Test
    void executesAndRemovesCachedPlan() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        Map<String, Object> values = new ConcurrentHashMap<>();
        values.put(RedirectClearingPlanCoordinator.getPlanCacheKey(23), plan(23, false));
        AtomicInteger refreshCount = new AtomicInteger();
        RedirectClearingPlanCoordinator coordinator = new RedirectClearingPlanCoordinator(
            service, cache(values), refreshCount::incrementAndGet
        );
        when(service.execute(coordinator.getPlan(23))).thenReturn(new ExecutionResult(1, 0, 0));

        ExecutionResult result = coordinator.execute(23);

        assertEquals(1, result.getUpdated());
        assertEquals(1, refreshCount.get());
        assertNull(coordinator.getPlan(23));
    }

    @Test
    void failedAnalysisPreservesCachedPlan() {
        RedirectClearingService service = mock(RedirectClearingService.class);
        Map<String, Object> values = new ConcurrentHashMap<>();
        RedirectClearingPlan previous = plan(23, false);
        values.put(RedirectClearingPlanCoordinator.getPlanCacheKey(23), previous);
        RedirectClearingPlanCoordinator coordinator = new RedirectClearingPlanCoordinator(service, cache(values));
        when(service.analyze(23, true)).thenThrow(new IllegalStateException("analysis failed"));

        assertThrows(IllegalStateException.class, () -> coordinator.analyze(23, true));
        assertSame(previous, coordinator.getPlan(23));
    }
}
