package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;

/** Verifies account-scoped administrator session counts without database or domain lookups. */
class SessionAdminCountTest {
    /** Active sessions span domains while foreign, expired, invalidated and non-administrator sessions are excluded. */
    @Test
    void countsOnlyActiveOwnedAdministratorSessionsWithoutMutatingTheHolder() {
        long now = 1_791_201_600_000L;
        long cutoff = now - 30 * 60_000L;
        var holder = spy(new SessionHolder());
        holder.getDataMap().put("autotest-current", session(7, true, now, "127.0.0.1", 1));
        holder.getDataMap().put("autotest-other-domain", session(7, true, cutoff, "127.0.0.2", 2));
        holder.getDataMap().put("autotest-expired", session(7, true, cutoff - 1, "127.0.0.3", 1));
        holder.getDataMap().put("autotest-invalidated", session(7, true, now, SessionHolder.INVALIDATE_SESSION_ADDR, 1));
        holder.getDataMap().put("autotest-non-admin", session(7, false, now, "127.0.0.4", 1));
        holder.getDataMap().put("autotest-foreign", session(8, true, now, "127.0.0.5", 1));
        var cache = mock(Cache.class);
        var cachedCounts = new AtomicReference<Object>();
        String cacheKey = "SessionHolder.adminSessionCounts";
        when(cache.getObject(cacheKey)).thenAnswer(invocation -> cachedCounts.get());
        doAnswer(invocation -> {
            cachedCounts.set(invocation.getArgument(1));
            return null;
        }).when(cache).setObjectSeconds(eq(cacheKey), any(), eq(10), eq(false));

        try (var constants = mockStatic(Constants.class); var tools = mockStatic(Tools.class); var caches = mockStatic(Cache.class);
             var domains = mockStatic(CloudToolsForCore.class); var cluster = mockStatic(SessionClusterService.class);
             var queries = mockConstruction(ComplexQuery.class); var updates = mockConstruction(SimpleQuery.class)) {
            constants.when(() -> Constants.getInt("sessionRemoveTimeout")).thenReturn(30);
            tools.when(Tools::getNow).thenReturn(now);
            caches.when(Cache::getInstance).thenReturn(cache);

            assertEquals(2, holder.getAdminSessionCount(7));
            assertEquals(0, holder.getAdminSessionCount(999));
            assertEquals(1, holder.getAdminSessionCount(8));
            assertEquals(6, holder.getDataMap().size());
            assertNotNull(holder.get("autotest-expired"));

            holder.getDataMap().put("autotest-new-login", session(7, true, now, "127.0.0.6", 1));
            assertEquals(2, holder.getAdminSessionCount(7), "A cache hit must reuse the previous count.");
            verify(cache).setObjectSeconds(eq(cacheKey), any(), eq(10), eq(false));
            cachedCounts.set(null);
            assertEquals(3, holder.getAdminSessionCount(7), "A cache miss must include newly logged-in sessions.");
            verify(cache, times(2)).setObjectSeconds(eq(cacheKey), any(), eq(10), eq(false));
            verify(holder, never()).getList();
            domains.verifyNoInteractions();
            cluster.verifyNoInteractions();
            assertTrue(queries.constructed().isEmpty());
            assertTrue(updates.constructed().isEmpty());
        }
    }

    private static SessionDetails session(int userId, boolean admin, long lastActivity, String address, int domainId) {
        var session = new SessionDetails();
        session.setLoggedUserId(userId);
        session.setAdmin(admin);
        session.setLastActivity(lastActivity);
        session.setRemoteAddr(address);
        session.setDomainId(domainId);
        return session;
    }
}
