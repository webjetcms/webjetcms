package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;

/** Verifies Boolean-only caching, account separation and setup-change invalidation. */
class TwoFactorStatusServiceTest {
    private final UserDetailsRepository repository = mock(UserDetailsRepository.class);
    private final Cache cache = mock(Cache.class);
    private final Map<String, Boolean> values = new ConcurrentHashMap<>();
    private final TwoFactorStatusService service = new TwoFactorStatusService(repository);
    private MockedStatic<Cache> caches;

    @BeforeEach
    void setUp() {
        caches = mockStatic(Cache.class);
        caches.when(Cache::getInstance).thenReturn(cache);
        when(cache.getObject(anyString(), eq(Boolean.class))).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(cache).setObjectSeconds(anyString(), anyBoolean(), anyInt(), anyBoolean());
        doAnswer(invocation -> {
            values.remove(invocation.getArgument(0));
            return null;
        }).when(cache).removeObject(anyString(), anyBoolean());
    }

    @AfterEach
    void tearDown() {
        caches.close();
    }

    /** Repeated display reads store only a Boolean with a ten-minute lifetime and no smart refresh. */
    @Test
    void cachesEnabledStatusWithoutRetainingSecret() {
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("private-second-factor-key");

        assertTrue(service.isEnabled(7));
        assertTrue(service.isEnabled(7));

        verify(repository, times(1)).getMobileDeviceByUserId(7L);
        verify(cache).setObjectSeconds("users.twoFactorStatus:7", Boolean.TRUE, 600, false);
        assertEquals(Map.of("users.twoFactorStatus:7", Boolean.TRUE), values);
    }

    /** Disabled accounts are cached too, while another account receives its own status. */
    @Test
    void cachesDisabledStatusSeparatelyForEachAccount() {
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("");
        when(repository.getMobileDeviceByUserId(8L)).thenReturn("another-key");

        assertFalse(service.isEnabled(7));
        assertTrue(service.isEnabled(8));
        assertFalse(service.isEnabled(7));

        verify(repository, times(1)).getMobileDeviceByUserId(7L);
        verify(repository, times(1)).getMobileDeviceByUserId(8L);
        assertEquals(Map.of("users.twoFactorStatus:7", Boolean.FALSE, "users.twoFactorStatus:8", Boolean.TRUE), values);
    }

    /** A successful setup change clears the account on the cluster and reloads its new status. */
    @Test
    void invalidationReloadsChangedAccountWithoutClearingOtherAccounts() {
        when(repository.getMobileDeviceByUserId(7L)).thenReturn(null, "new-key");
        when(repository.getMobileDeviceByUserId(8L)).thenReturn("another-key");
        assertFalse(service.isEnabled(7));
        assertTrue(service.isEnabled(8));

        service.invalidate(7);

        assertTrue(service.isEnabled(7));
        assertTrue(service.isEnabled(8));
        verify(cache).removeObject("users.twoFactorStatus:7", true);
        verify(repository, times(2)).getMobileDeviceByUserId(7L);
        verify(repository, times(1)).getMobileDeviceByUserId(8L);
    }

    /** Failed reads do not cache a guessed status and a later successful request can retry. */
    @Test
    void doesNotCacheRepositoryFailure() {
        when(repository.getMobileDeviceByUserId(7L)).thenThrow(new IllegalStateException("Unavailable")).thenReturn("key");

        assertThrows(IllegalStateException.class, () -> service.isEnabled(7));
        assertTrue(values.isEmpty());
        assertTrue(service.isEnabled(7));
        verify(repository, times(2)).getMobileDeviceByUserId(7L);
    }

    /** Invalidation waits for an earlier local read so that it cannot restore status after a save. */
    @Test
    void invalidationClearsStatusLoadedByAnEarlierInFlightRead() throws Exception {
        var readStarted = new CountDownLatch(1);
        var finishRead = new CountDownLatch(1);
        var invalidationStarted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        when(repository.getMobileDeviceByUserId(7L)).thenAnswer(invocation -> {
            readStarted.countDown();
            if (!finishRead.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Read was not released");
            return "earlier-key";
        });

        try {
            var read = executor.submit(() -> {
                try (var threadCache = mockStatic(Cache.class)) {
                    threadCache.when(Cache::getInstance).thenReturn(cache);
                    return service.isEnabled(7);
                }
            });
            assertTrue(readStarted.await(5, TimeUnit.SECONDS));
            var invalidation = executor.submit(() -> {
                try (var threadCache = mockStatic(Cache.class)) {
                    threadCache.when(Cache::getInstance).thenReturn(cache);
                    invalidationStarted.countDown();
                    service.invalidate(7);
                }
                return null;
            });
            assertTrue(invalidationStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> invalidation.get(100, TimeUnit.MILLISECONDS));

            finishRead.countDown();
            assertTrue(read.get(5, TimeUnit.SECONDS));
            invalidation.get(5, TimeUnit.SECONDS);
            when(repository.getMobileDeviceByUserId(7L)).thenReturn("");

            assertFalse(service.isEnabled(7));
            verify(repository, times(2)).getMobileDeviceByUserId(7L);
        } finally {
            finishRead.countDown();
            executor.shutdownNow();
        }
    }
}
