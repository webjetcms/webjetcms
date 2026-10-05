package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import jakarta.servlet.http.Cookie;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.users.UsersDB;

/** Verifies cookie lifecycle, successful-login boundaries, account isolation and safe notification data. */
class AdminDeviceServiceTest {
    private static final long NOW = Instant.parse("2026-10-05T10:00:00Z").toEpochMilli();
    private static final String EVENT_ID = "d12b2090-5818-49d8-808c-688593fe71a4";
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    private final DeviceService repository = mock(DeviceService.class);
    private final AdminDeviceService service = spy(new AdminDeviceService(repository, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)));
    private final Identity user = mock(Identity.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private MockedStatic<Constants> constants;
    private MockedStatic<Tools> tools;
    private MockedStatic<UsersDB> users;
    private MockedConstruction<BrowserDetector> browsers;

    @BeforeEach
    void prepareLogin() {
        constants = mockStatic(Constants.class);
        tools = mockStatic(Tools.class);
        users = mockStatic(UsersDB.class);
        browsers = mockConstruction(BrowserDetector.class, (browser, context) -> {
            when(browser.getBrowserName()).thenReturn("Firefox");
            when(browser.getBrowserVersion()).thenReturn("131.0");
            when(browser.getBrowserPlatform()).thenReturn("Windows");
            when(browser.getBrowserSubplatform()).thenReturn("11");
        });
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
        constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(90);
        users.when(UsersDB::getDomainId).thenReturn(12);
        tools.when(() -> Tools.getSpringBean("adminDeviceService", AdminDeviceService.class)).thenReturn(service);
        tools.when(() -> Tools.getRemoteIP(any())).thenReturn("192.0.2.1");
        tools.when(() -> Tools.isSecure(any())).thenReturn(true);
        tools.when(() -> Tools.addCookie(any(), any(), any())).thenAnswer(invocation -> {
            ((MockHttpServletResponse) invocation.getArgument(1)).addCookie(invocation.getArgument(0));
            return true;
        });
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        when(user.getFullName()).thenReturn("Autotest Administrator");
        when(user.getFirstName()).thenReturn("Autotest");
        when(user.getEmail()).thenReturn("autotest@example.test");
        request.addHeader("User-Agent", "Autotest Browser");
        doNothing().when(service).sendNotification(any(), any(), any());
    }

    @AfterEach
    void releaseMocks() {
        browsers.close();
        users.close();
        tools.close();
        constants.close();
    }

    /** A newly issued browser identifier is opaque, protected and persisted only as a hash. */
    @Test
    void issuesProtectedCookieAndQueuesOnlyNewEvents() {
        when(repository.recordLogin(anyInt(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(event());

        AdminDeviceService.recordSuccessfulLogin(user, request, response);

        Cookie cookie = response.getCookie(AdminDeviceService.COOKIE_NAME);
        assertNotNull(cookie);
        assertTrue(cookie.getValue().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(32, java.util.Base64.getUrlDecoder().decode(cookie.getValue()).length);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(90 * 86400, cookie.getMaxAge());
        verify(repository).recordLogin(7, 12, AdminDeviceService.hashToken(cookie.getValue()), NOW,
            NOW - Duration.ofDays(90).toMillis(), "Firefox", "131.0", "Windows 11", "192.0.2.1");
        verify(service).sendNotification(user, request, event());
    }

    /** Browser-version changes retain the token; repeated hooks do not duplicate one login. */
    @Test
    void renewsKnownCookieAndProcessesALaterLoginInTheSameSession() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        verify(repository, times(1)).recordLogin(anyInt(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());

        var laterRequest = new MockHttpServletRequest();
        laterRequest.setSession(request.getSession());
        laterRequest.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        laterRequest.addHeader("User-Agent", "Updated Autotest Browser");
        AdminDeviceService.recordSuccessfulLogin(user, laterRequest, new MockHttpServletResponse());

        assertEquals(TOKEN, response.getCookie(AdminDeviceService.COOKIE_NAME).getValue());
        verify(repository, times(2)).recordLogin(eq(7), eq(12), eq(AdminDeviceService.hashToken(TOKEN)), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(service, never()).sendNotification(any(), any(), any());
    }

    /** Sharing a browser token never shares account recognition. */
    @Test
    void scopesAnotherAccountIndependently() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        when(user.getUserId()).thenReturn(8);
        AdminDeviceService.recordSuccessfulLogin(user, request, new MockHttpServletResponse());
        verify(repository).recordLogin(eq(7), eq(12), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());
        verify(repository).recordLogin(eq(8), eq(12), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void replacesMalformedCookieAndUsesConfiguredLifetimeOnHttp() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, "malformed"));
        constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(30);
        tools.when(() -> Tools.isSecure(request)).thenReturn(false);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        Cookie cookie = response.getCookie(AdminDeviceService.COOKIE_NAME);
        assertNotEquals("malformed", cookie.getValue());
        assertEquals(30 * 86400, cookie.getMaxAge());
        assertFalse(cookie.getSecure());
        verify(repository).recordLogin(eq(7), eq(12), anyString(), eq(NOW), eq(NOW - Duration.ofDays(30).toMillis()), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void disabledFeatureAndNonAdministratorsDoNotReadOrWriteDevices() {
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(false);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        assertTrue(service.activeEvents(user).isEmpty());
        assertNull(service.findEvent(user, EVENT_ID));
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
        when(user.isAdmin()).thenReturn(false);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        assertThrows(AccessDeniedException.class, () -> service.confirm(user, EVENT_ID));
        verifyNoInteractions(repository);
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void storageFailureCannotRejectSuccessfulAuthentication() {
        when(repository.recordLogin(anyInt(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("Autotest storage unavailable"));
        try (var logger = mockStatic(Logger.class)) {
            assertDoesNotThrow(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
        }
        verify(service, never()).sendNotification(any(), any(), any());
    }

    @Test
    void blockedCookieStillRecordsAndNotifiesOnlyOncePerLoginRequest() {
        tools.when(() -> Tools.addCookie(any(), any(), any())).thenReturn(false);
        when(repository.recordLogin(anyInt(), anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(event());
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        verify(service, times(1)).sendNotification(user, request, event());
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void rejectsMalformedEventIdsBeforeQueryingAndDerivesAccountScope() {
        assertNull(service.confirm(user, "foreign-or-invalid"));
        assertNull(service.report(user, "../event"));
        assertNull(service.findEvent(user, null));
        verifyNoInteractions(repository);
        service.findEvent(user, EVENT_ID);
        service.confirm(user, EVENT_ID);
        service.report(user, EVENT_ID);
        verify(repository).findEvent(7, 12, EVENT_ID, NOW);
        verify(repository).confirm(7, 12, EVENT_ID, NOW);
        verify(repository).report(7, 12, EVENT_ID, NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://attacker.test/admin/v9/", "//attacker.test/admin/v9/", "/admin/v9/../../outside", "/admin/v9/%2e%2e/outside", "/admin/v9/\\attacker", "/admin/v9/?x=%0d%0aLocation:attacker", "/public/", "/admin/v9/%zz"})
    void rejectsUnsafeReturnTargets(String target) {
        request.getSession().setAttribute("adminAfterLogonRedirect", target);
        assertEquals("/admin/v9/", AdminDeviceService.getAfterLoginRedirect(request));
    }

    @Test
    void preservesAndConsumesTheEmailReturnTarget() {
        String target = "/admin/v9/?securityEvent=" + EVENT_ID;
        request.getSession().setAttribute("adminAfterLogonRedirect", target);
        assertEquals(target, AdminDeviceService.getAfterLoginRedirect(request));
        assertEquals("/admin/v9/", AdminDeviceService.getAfterLoginRedirect(request));
    }

    /** Mail uses password-reset sender resolution and escaped snapshots rather than browser credentials. */
    @Test
    void queuesEscapedEmailWithMatchingPasswordResetSenderAndReadOnlyLink() {
        doCallRealMethod().when(service).sendNotification(any(), any(), any());
        when(user.getFirstName()).thenReturn("<script>Autotest</script>");
        tools.when(() -> Tools.escapeHtml(anyString())).thenCallRealMethod();
        tools.when(() -> Tools.getBaseHref(request)).thenReturn("https://cms.example.test");
        tools.when(() -> Tools.getServerName(request)).thenReturn("cms.example.test");
        tools.when(() -> Tools.getRequestAttribute(request, "sendPasswordFromName", "Autotest Administrator")).thenReturn("Autotest Administrator");
        tools.when(() -> Tools.getRequestAttribute(request, "sendPasswordFromEmail", "autotest@example.test")).thenReturn("autotest@example.test");
        tools.when(() -> Tools.formatDateTime(NOW)).thenReturn("05.10.2026 12:00");
        constants.when(() -> Constants.getStringExecuteMacro("dashboardEnvironmentName")).thenReturn("DEV");
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        try (var properties = mockStatic(Prop.class); var mail = mockStatic(SendMail.class)) {
            properties.when(() -> Prop.getInstance(request)).thenReturn(prop);
            mail.when(() -> SendMail.getDefaultSenderName("passwordReset", "Autotest Administrator")).thenReturn("Autotest Sender");
            mail.when(() -> SendMail.getDefaultSenderEmail("passwordReset", "autotest@example.test")).thenReturn("sender@example.test");
            mail.when(() -> SendMail.sendLater(anyString(), anyString(), anyString(), isNull(), isNull(), isNull(), anyString(), anyString(), anyString(), isNull(), isNull())).thenReturn(true);
            service.sendNotification(user, request, event());
            ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
            mail.verify(() -> SendMail.sendLater(eq("Autotest Sender"), eq("sender@example.test"), eq("autotest@example.test"), isNull(), isNull(), isNull(),
                eq("cms.example.test"), html.capture(), eq("https://cms.example.test"), isNull(), isNull()));
            assertTrue(html.getValue().contains("&lt;script&gt;Autotest&lt;/script&gt;"));
            assertFalse(html.getValue().contains("<script>"));
            assertTrue(html.getValue().contains("https://cms.example.test/admin/v9/?securityEvent=" + EVENT_ID));
            assertFalse(html.getValue().contains(TOKEN));
            assertFalse(html.getValue().contains("/report"));
        }
    }

    private static LoginEvent event() {
        return new LoginEvent(EVENT_ID, NOW, NOW + Duration.ofDays(7).toMillis(), "Firefox", "131.0", "Windows 11", "192.0.2.1", null, null);
    }
}
