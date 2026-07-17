package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;
import sk.iway.iwcm.system.RedirectsRepository;
import sk.iway.iwcm.system.UrlRedirectBean;

class RedirectClearingServiceTest {

    private final RedirectClearingService service = new RedirectClearingService(null);

    @Test
    void deletesOldTargetsWithinTheSameScheduleOnly() {
        UrlRedirectBean old = redirect(1, "aaa", "bbb", "example.com", 100L, null, null, 301);
        UrlRedirectBean newest = redirect(2, "aaa", "ccc", "example.com", 200L, null, null, 302);
        UrlRedirectBean otherSchedule = redirect(3, "aaa", "ddd", "example.com", 300L, 1_000L, 2_000L, 307);

        RedirectClearingPlan plan = service.analyze("example.com", List.of(old, newest, otherSchedule));

        assertAction(plan, 1, ActionType.DELETE_OLD, null);
        assertFalse(hasAction(plan, 2));
        assertFalse(hasAction(plan, 3));
    }

    @Test
    void namedAndUnnamedTargetsDoNotCompete() {
        UrlRedirectBean unnamed = redirect(4, "/old", "/unnamed", null, 100L, null, null, 301);
        UrlRedirectBean named = redirect(5, "/old", "/named", "example.com", 200L, null, null, 302);

        RedirectClearingPlan plan = service.analyze("example.com", List.of(named, unnamed));

        assertFalse(hasAction(plan, 4));
        assertFalse(hasAction(plan, 5));
    }

    @Test
    void duplicateComparisonIgnoresCodeAndScheduleAndKeepsOldest() {
        UrlRedirectBean oldest = redirect(10, "/old", "/new", "example.com", null, null, null, 301);
        UrlRedirectBean newer = redirect(11, "/old", "/new", "example.com", 100L, 1_000L, 2_000L, 302);
        UrlRedirectBean newest = redirect(12, "/old", "/new", "example.com", 200L, 3_000L, 4_000L, 307);

        RedirectClearingPlan plan = service.analyze("example.com", List.of(newest, newer, oldest));

        assertFalse(hasAction(plan, 10));
        assertAction(plan, 11, ActionType.DELETE_DUPLICATE, null);
        assertAction(plan, 12, ActionType.DELETE_DUPLICATE, null);
    }

    @Test
    void duplicatesAreRemovedOnlyWithinTheSameDomain() {
        UrlRedirectBean named = redirect(20, "/old", "/new", "example.com", 100L, null, null, 301);
        UrlRedirectBean newerUnnamed = redirect(21, "/old", "/new", null, 300L, 1_000L, 2_000L, 302);
        UrlRedirectBean olderUnnamed = redirect(22, "/old", "/new", "", 200L, 1_000L, 2_000L, 307);

        RedirectClearingPlan plan = service.analyze("example.com", List.of(named, newerUnnamed, olderUnnamed));

        assertFalse(hasAction(plan, 20));
        assertAction(plan, 21, ActionType.DELETE_DUPLICATE, null);
        assertFalse(hasAction(plan, 22));
    }

    @Test
    void equalFinalTargetsInDifferentDomainsAreNotDeduplicated() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(30, "/a", "/target", "", 300L, null, null, 302),
            redirect(31, "/a", "/b", "example.com", 100L, null, null, 301),
            redirect(32, "/b", "/target", "example.com", 200L, null, null, 301)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 30));
        assertAction(plan, 31, ActionType.UPDATE_OPTIMIZE, "/target");
        assertFalse(hasAction(plan, 32));
    }

    @Test
    void usesIdAsAgeTieBreaker() {
        UrlRedirectBean lowerId = redirect(20, "/old", "/first", "example.com", 100L, null, null, 301);
        UrlRedirectBean higherId = redirect(21, "/old", "/second", "example.com", 100L, null, null, 302);

        RedirectClearingPlan plan = service.analyze("example.com", List.of(higherId, lowerId));

        assertAction(plan, 20, ActionType.DELETE_OLD, null);
        assertFalse(hasAction(plan, 21));
    }

    @Test
    void optimizesSimpleAndLongChainsWithoutRecursion() {
        List<UrlRedirectBean> redirects = new ArrayList<>();
        for (int index = 0; index < 3_000; index++) {
            redirects.add(redirect(index + 1L, "/chain-" + index, "/chain-" + (index + 1), "example.com", (long) index, null, null, 301));
        }

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertEquals(2_999, plan.getUpdateCount());
        assertEquals(0, plan.getDeleteCount());
        assertAction(plan, 1, ActionType.UPDATE_OPTIMIZE, "/chain-3000");
        assertAction(plan, 2_999, ActionType.UPDATE_OPTIMIZE, "/chain-3000");
        assertFalse(hasAction(plan, 3_000));
    }

    @Test
    void removesSelfLoop() {
        RedirectClearingPlan plan = service.analyze(
            "example.com",
            List.of(redirect(1, "/loop", "/loop", "example.com", 100L, null, null, 302))
        );

        assertAction(plan, 1, ActionType.DELETE_CYCLE, null);
    }

    @Test
    void removesNewestStepOfMultiElementCycleAndOptimizesRemainingPath() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "/a", "/b", "example.com", 100L, null, null, 302),
            redirect(2, "/b", "/c", "example.com", 200L, null, null, 302),
            redirect(3, "/c", "/a", "example.com", 300L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertAction(plan, 3, ActionType.DELETE_CYCLE, null);
        assertAction(plan, 1, ActionType.UPDATE_OPTIMIZE, "/c");
        assertFalse(hasAction(plan, 2));
    }

    @Test
    void namedAndUnnamedRedirectsDoNotFormMixedCycles() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(6, "/a", "/b", null, 300L, null, null, 302),
            redirect(7, "/b", "/a", "example.com", 100L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 6));
        assertFalse(hasAction(plan, 7));
    }

    @Test
    void nullAndEmptyDomainsFormOneCycleScope() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(8, "/a", "/b", null, 100L, null, null, 302),
            redirect(9, "/b", "/a", "", 200L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 8));
        assertAction(plan, 9, ActionType.DELETE_CYCLE, null);
    }

    @Test
    void namedDuplicateDoesNotInfluenceUnnamedCycle() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(12, "/a", "/b", null, 100L, null, null, 302),
            redirect(13, "/a", "/b", "example.com", 1_000L, null, null, 302),
            redirect(14, "/b", "/a", "", 200L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 12));
        assertFalse(hasAction(plan, 13));
        assertAction(plan, 14, ActionType.DELETE_CYCLE, null);
    }

    @Test
    void removesAllDuplicatesRepresentingTheSelectedCycleStep() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "/a", "/b", "example.com", 100L, null, null, 302),
            redirect(2, "/b", "/a", "example.com", 200L, null, null, 302),
            redirect(3, "/b", "/a", "example.com", 300L, null, null, 301)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertAction(plan, 2, ActionType.DELETE_CYCLE, null);
        assertAction(plan, 3, ActionType.DELETE_CYCLE, null);
        assertFalse(hasAction(plan, 1));
    }

    @Test
    void deduplicatesRecordsThatBecomeEqualAfterOptimization() {
        Date firstSchedule = new Date(1_000);
        Date secondSchedule = new Date(2_000);
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "/a", "/b", "example.com", 100L, firstSchedule.getTime(), null, 301),
            redirect(2, "/b", "/target", "example.com", 100L, firstSchedule.getTime(), null, 301),
            redirect(3, "/a", "/c", "example.com", 200L, secondSchedule.getTime(), null, 302),
            redirect(4, "/c", "/target", "example.com", 200L, secondSchedule.getTime(), null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertAction(plan, 1, ActionType.UPDATE_OPTIMIZE, "/target");
        assertAction(plan, 3, ActionType.DELETE_DUPLICATE, null);
        assertUniqueActionIds(plan);
    }

    @Test
    void chainsAreOptimizedOnlyInsideTheSameDomain() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "/a", "/b", "example.com", 100L, null, null, 302),
            redirect(2, "/b", "/c", null, 200L, null, null, 302),
            redirect(3, "/x", "/y", "", 100L, null, null, 302),
            redirect(4, "/y", "/z", null, 200L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 1));
        assertFalse(hasAction(plan, 2));
        assertAction(plan, 3, ActionType.UPDATE_OPTIMIZE, "/z");
        assertFalse(hasAction(plan, 4));
    }

    @Test
    void unnamedChainsDoNotUseNamedRedirects() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(10, "/a", "/b", null, 100L, null, null, 302),
            redirect(11, "/b", "/c", "example.com", 200L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertFalse(hasAction(plan, 10));
        assertFalse(hasAction(plan, 11));
    }

    @Test
    void ignoresRegexpAndInvalidUrls() {
        List<UrlRedirectBean> redirects = List.of(
            redirect(1, "regexp:^/old", "/new", "example.com", 100L, null, null, 302),
            redirect(2, "", "/new", "example.com", 100L, null, null, 302),
            redirect(3, "/old", "", "example.com", 100L, null, null, 302)
        );

        RedirectClearingPlan plan = service.analyze("example.com", redirects);

        assertTrue(plan.isEmpty());
        assertEquals(3, plan.getIgnoredRecords());
    }

    @Test
    void executesStoredUpdatesAndSkipsMissingOrInaccessibleRecords() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectClearingService executionService = new RedirectClearingService(repository);
        when(repository.updateNewUrlForRedirectClearing(List.of(1L), "/stored-target", "example.com")).thenReturn(1);
        when(repository.deleteForRedirectClearing(List.of(2L, 3L), "example.com")).thenReturn(0);

        RedirectClearingPlan plan = new RedirectClearingPlan("example.com", List.of(
            action(1, ActionType.UPDATE_OPTIMIZE, "/stored-target"),
            action(2, ActionType.DELETE_DUPLICATE, null),
            action(3, ActionType.DELETE_OLD, null)
        ), 3, 0);

        ExecutionResult result = executionService.execute(plan, "example.com");

        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getDeleted());
        assertEquals(2, result.getSkipped());
        verify(repository).updateNewUrlForRedirectClearing(List.of(1L), "/stored-target", "example.com");
        verify(repository).deleteForRedirectClearing(List.of(2L, 3L), "example.com");
    }

    @Test
    void executesDeletesInBatchesOfAtMostFiveHundred() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectClearingService executionService = new RedirectClearingService(repository);
        List<RedirectClearingAction> actions = new ArrayList<>();

        for (long id = 1; id <= 501; id++) {
            actions.add(action(id, ActionType.DELETE_DUPLICATE, null));
        }
        when(repository.deleteForRedirectClearing(anyList(), eq("example.com"))).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.size();
        });

        ExecutionResult result = executionService.execute(
            new RedirectClearingPlan("example.com", actions, actions.size(), 0),
            "example.com"
        );

        assertEquals(501, result.getDeleted());
        verify(repository, times(2)).deleteForRedirectClearing(anyList(), eq("example.com"));
    }

    @Test
    void executionPropagatesBulkUpdateFailure() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectClearingService executionService = new RedirectClearingService(repository);
        when(repository.updateNewUrlForRedirectClearing(anyList(), eq("/target"), eq("example.com")))
            .thenThrow(new IllegalStateException("database failure"));

        RedirectClearingPlan plan = new RedirectClearingPlan(
            "example.com",
            List.of(action(1, ActionType.UPDATE_OPTIMIZE, "/target")),
            1,
            0
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> executionService.execute(plan, "example.com")
        );
        assertEquals("database failure", exception.getMessage());
    }

    private static UrlRedirectBean redirect(
        long id,
        String oldUrl,
        String newUrl,
        String domain,
        Long insertDate,
        Long publishDate,
        Long validTo,
        int redirectCode
    ) {
        UrlRedirectBean redirect = new UrlRedirectBean(oldUrl, newUrl, redirectCode, domain);
        redirect.setUrlRedirectId(id);
        redirect.setInsertDate(insertDate == null ? null : new Date(insertDate));
        redirect.setPublishDate(publishDate == null ? null : new Date(publishDate));
        redirect.setValidTo(validTo == null ? null : new Date(validTo));
        return redirect;
    }

    private static RedirectClearingAction action(long id, ActionType type, String proposedUrl) {
        return new RedirectClearingAction(id, type, "/old", "/new", proposedUrl, "example.com", 302, null, null, null);
    }

    private static boolean hasAction(RedirectClearingPlan plan, long id) {
        return plan.getActions().stream().anyMatch(action -> action.getId() == id);
    }

    private static void assertAction(RedirectClearingPlan plan, long id, ActionType type, String proposedUrl) {
        RedirectClearingAction action = plan.getActions().stream()
            .filter(candidate -> candidate.getId() == id)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing action for redirect ID " + id));
        assertEquals(type, action.getAction());
        assertEquals(proposedUrl, action.getProposedNewUrl());
    }

    private static void assertUniqueActionIds(RedirectClearingPlan plan) {
        Set<Long> ids = new HashSet<>();
        for (RedirectClearingAction action : plan.getActions()) {
            assertTrue(ids.add(action.getId()), "Duplicate action for redirect ID " + action.getId());
        }
    }
}
