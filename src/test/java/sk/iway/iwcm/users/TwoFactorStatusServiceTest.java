package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.welcome.DashboardNoticeService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.devices.AdminDeviceService;

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

    /** Repeated display reads store only a Boolean with a ten-minute lifetime. */
    @Test
    void cachesEnabledStatusWithoutRetainingSecret() {
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("private-second-factor-key");

        assertTrue(service.isEnabled(7));
        assertTrue(service.isEnabled(7));

        verify(repository, times(1)).getMobileDeviceByUserId(7L);
        verify(cache).setObjectSeconds(eq("users.twoFactorStatus:7"), eq(Boolean.TRUE), eq(600), anyBoolean());
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

    /** Integrates setup writes, the status endpoint, menu and dashboard through their shared cache. */
    @Test
    void setupChangesRefreshMenuAndDashboardWithoutDuplicateReads() {
        var request = new MockHttpServletRequest();
        var user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        var prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        var storedSecret = new AtomicReference<>("");
        when(repository.getMobileDeviceByUserId(7L)).thenAnswer(invocation -> storedSecret.get());
        doAnswer(invocation -> {
            storedSecret.set(invocation.getArgument(1));
            return null;
        }).when(repository).updateMobileDeviceByUserId(eq(7L), anyString());
        var controller = new UsersRestServiceController(repository, service);
        var notices = new DashboardNoticeService(service, mock(AdminDeviceService.class));

        try (var users = mockStatic(UsersDB.class); var csrf = mockStatic(CSRF.class);
             var constants = mockStatic(Constants.class); var properties = mockStatic(Prop.class);
             var tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var authentication = mockStatic(AuthenticationFilter.class); var paths = mockConstruction(IwcmFile.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);
            csrf.when(() -> CSRF.verifyTokenAndDeleteIt(request)).thenReturn(true);
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            constants.when(() -> Constants.getBoolean("2factorAuthEnabled")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("statWebJET7Converted")).thenReturn(true);
            properties.when(() -> Prop.getInstance(request)).thenReturn(prop);
            tools.when(() -> Tools.getSpringBean("twoFactorStatusService", TwoFactorStatusService.class)).thenReturn(service);
            tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            int reads = 0;
            for (boolean enabled : new boolean[] { false, true, false }) {
                if (reads > 0) assertEquals("", controller.setGauth(enabled ? "autotest-key" : "", request));
                var layout = new LayoutService(request);
                assertEquals(enabled, layout.isTwoFactorEnabled(), "The next page must display the saved status.");
                assertEquals(enabled ? "true" : "", controller.getGauth(request));
                assertEquals(!enabled, notices.load(user, request).stream().anyMatch(notice -> "twoFactor".equals(notice.get("id"))),
                    "The dashboard warning must agree with the menu.");
                verify(repository, times(++reads)).getMobileDeviceByUserId(7L);
            }
            verify(cache, times(2)).removeObject("users.twoFactorStatus:7", true);
        }
    }

}
