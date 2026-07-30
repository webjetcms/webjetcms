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
import sk.iway.iwcm.components.redirects.RedirectCleaningService.ExecutionResult;

class RedirectCleaningPlanCoordinatorTest extends RedirectCleaningTestSupport {

    @Test
    void analyzesAndCachesDomainPlan() {
        RedirectCleaningService service = mock(RedirectCleaningService.class);
        Cache cache = mock(Cache.class);
        RedirectCleaningPlan plan = plan(23, false);
        when(service.analyze(23, false)).thenReturn(plan);
        RedirectCleaningPlanCoordinator coordinator = new RedirectCleaningPlanCoordinator(service, cache);

        assertSame(plan, coordinator.analyze(23, false));

        verify(cache).setObjectSeconds(
            RedirectCleaningPlanCoordinator.getPlanCacheKey(23),
            plan,
            RedirectCleaningPlanCoordinator.PLAN_CACHE_SECONDS,
            false
        );
    }

    @Test
    void executesAndRemovesCachedPlan() {
        RedirectCleaningService service = mock(RedirectCleaningService.class);
        Map<String, Object> values = new ConcurrentHashMap<>();
        values.put(RedirectCleaningPlanCoordinator.getPlanCacheKey(23), plan(23, false));
        AtomicInteger refreshCount = new AtomicInteger();
        RedirectCleaningPlanCoordinator coordinator = new RedirectCleaningPlanCoordinator(
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
        RedirectCleaningService service = mock(RedirectCleaningService.class);
        Map<String, Object> values = new ConcurrentHashMap<>();
        RedirectCleaningPlan previous = plan(23, false);
        values.put(RedirectCleaningPlanCoordinator.getPlanCacheKey(23), previous);
        RedirectCleaningPlanCoordinator coordinator = new RedirectCleaningPlanCoordinator(service, cache(values));
        when(service.analyze(23, true)).thenThrow(new IllegalStateException("analysis failed"));

        assertThrows(IllegalStateException.class, () -> coordinator.analyze(23, true));
        assertSame(previous, coordinator.getPlan(23));
    }
}
