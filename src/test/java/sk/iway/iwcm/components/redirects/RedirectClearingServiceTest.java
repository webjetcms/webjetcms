package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.system.UrlRedirectBean;

class RedirectClearingServiceTest extends RedirectClearingTestSupport {

    private final RedirectClearingService service = new RedirectClearingService(null);

    @Test
    void deletesObsoleteTarget() {
        RedirectClearingPlan plan = service.analyze(DOMAIN, List.of(
            redirect(1, "/old", "/first", 100L),
            redirect(2, "/old", "/latest", 200L)
        ));

        assertAction(plan, 1, ActionType.DELETE_OLD, null);
        assertNoAction(plan, 2);
    }

    @Test
    void deletesNewerDuplicate() {
        RedirectClearingPlan plan = service.analyze(DOMAIN, List.of(
            redirect(1, "/old", "/new", 100L),
            redirect(2, "/old", "/new", 200L)
        ));

        assertNoAction(plan, 1);
        assertAction(plan, 2, ActionType.DELETE_DUPLICATE, null);
    }

    @Test
    void optimizesSimpleChain() {
        RedirectClearingPlan plan = service.analyze(DOMAIN, List.of(
            redirect(1, "/a", "/b", 100L),
            redirect(2, "/b", "/c", 200L)
        ));

        assertAction(plan, 1, ActionType.UPDATE_OPTIMIZE, "/c");
        assertNoAction(plan, 2);
    }

    @Test
    void removesNewestCycleStep() {
        RedirectClearingPlan plan = service.analyze(DOMAIN, List.of(
            redirect(1, "/a", "/b", 100L),
            redirect(2, "/b", "/a", 200L)
        ));

        assertNoAction(plan, 1);
        assertAction(plan, 2, ActionType.DELETE_CYCLE, null);
    }

    @Test
    void ignoresUnsupportedRedirects() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "regexp:^/old", "/new", DOMAIN, 100L, null, null, 302),
            redirect(2, "/published", "/new", DOMAIN, 100L, 1_000L, null, 302),
            redirect(3, "/expires", "/new", DOMAIN, 100L, null, 2_000L, 302),
            redirect(4, "", "/new", 100L)
        );

        RedirectClearingPlan plan = service.analyze(DOMAIN, redirects);

        assertTrue(plan.isEmpty());
        assertEquals(4, plan.getIgnoredRecords());
    }

    @Test
    void includesUnnamedScopeOnlyWhenRequested() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "/named", "/target", DOMAIN, 100L),
            redirect(2, "/named", "/target", DOMAIN, 200L),
            redirect(3, "/unnamed", "/target", null, 100L),
            redirect(4, "/unnamed", "/target", "", 200L)
        );

        RedirectClearingPlan namedOnly = service.analyze(DOMAIN, false, redirects);
        RedirectClearingPlan withUnnamed = service.analyze(DOMAIN, true, redirects);

        assertEquals(2, namedOnly.getAnalyzedRecords());
        assertFalse(namedOnly.isIncludeUnnamed());
        assertNoAction(namedOnly, 4);
        assertEquals(4, withUnnamed.getAnalyzedRecords());
        assertAction(withUnnamed, 4, ActionType.DELETE_DUPLICATE, null);
    }
}
