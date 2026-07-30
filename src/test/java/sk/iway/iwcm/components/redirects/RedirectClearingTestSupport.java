package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.system.UrlRedirectBean;

abstract class RedirectClearingTestSupport {

    static final String DOMAIN = "example.com";

    static UrlRedirectBean redirect(long id, String oldUrl, String newUrl, Long insertDate) {
        return redirect(id, oldUrl, newUrl, DOMAIN, insertDate);
    }

    static UrlRedirectBean redirect(long id, String oldUrl, String newUrl, String domain, Long insertDate) {
        return redirect(id, oldUrl, newUrl, domain, insertDate, null, null, 302);
    }

    static UrlRedirectBean redirect(
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
        redirect.setInsertDate(date(insertDate));
        redirect.setPublishDate(date(publishDate));
        redirect.setValidTo(date(validTo));
        return redirect;
    }

    static RedirectClearingAction action(long id, ActionType type, String proposedUrl) {
        return new RedirectClearingAction(
            id, type, "/old", "/new", proposedUrl, DOMAIN, 302, null
        );
    }

    static RedirectClearingPlan plan(int domainId, boolean includeUnnamed) {
        return new RedirectClearingPlan(
            domainId,
            DOMAIN,
            includeUnnamed,
            List.of(action(domainId, ActionType.DELETE_DUPLICATE, null)),
            1,
            0
        );
    }

    static RedirectClearingPlan plan(List<RedirectClearingAction> actions) {
        return new RedirectClearingPlan(-1, DOMAIN, true, actions, actions.size(), 0);
    }

    static Cache cache(Map<String, Object> values) {
        Cache cache = mock(Cache.class);
        when(cache.getObject(anyString(), eq(RedirectClearingPlan.class)))
            .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(cache).setObjectSeconds(anyString(), any(), anyInt(), eq(false));
        doAnswer(invocation -> {
            values.remove(invocation.getArgument(0));
            return null;
        }).when(cache).removeObject(anyString());
        return cache;
    }

    static void assertAction(RedirectClearingPlan plan, long id, ActionType type, String proposedUrl) {
        RedirectClearingAction action = plan.getActions().stream()
            .filter(candidate -> candidate.getId() == id)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing action for redirect ID " + id));
        assertEquals(type, action.getAction());
        assertEquals(proposedUrl, action.getProposedNewUrl());
    }

    static void assertNoAction(RedirectClearingPlan plan, long id) {
        assertFalse(plan.getActions().stream().anyMatch(action -> action.getId() == id));
    }

    private static Date date(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
}
