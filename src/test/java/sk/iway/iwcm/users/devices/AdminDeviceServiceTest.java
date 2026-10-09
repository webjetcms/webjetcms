package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionClusterService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.users.UsersDB;

/** Verifies cookie lifecycle, successful-login boundaries, account isolation and safe notification data. */
class AdminDeviceServiceTest {
    private static final long NOW = Instant.parse("2026-10-05T10:00:00Z").toEpochMilli();
    private static final String EVENT_ID = "42";
    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    private static final String HEADLESS_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/131.0.6778.33 Safari/537.36";
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
        users.when(() -> UsersDB.getCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
        browsers = mockConstruction(BrowserDetector.class, (browser, context) -> {
            when(browser.getBrowserName()).thenReturn("Firefox");
            when(browser.getBrowserVersion()).thenReturn("131.0");
            when(browser.getBrowserPlatform()).thenReturn("Windows");
            when(browser.getBrowserSubplatform()).thenReturn("11");
        });
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
        constants.when(() -> Constants.getInt("adminNewDeviceMaxAgeDays")).thenReturn(90);
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

    /** Device lists derive ownership, page size and ordering on the server and require an administrator. */
    @Test
    void deviceListIsOwnedBoundedAndRequiresAdministrator() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastSeen", "id"));
        var page = new org.springframework.data.domain.PageImpl<>(List.of(event()), pageable, 1);
        when(repository.findDevices(7, pageable)).thenReturn(page);
        assertSame(page, service.getDevices(user, -1));
        verify(repository).findDevices(7, pageable);
        assertThrows(AccessDeniedException.class, () -> service.getDevices(null, 0));
        when(user.isAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.getDevices(user, 0));
        when(user.isAdmin()).thenReturn(true);
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(false);
        assertTrue(service.getDevices(user, 0).isEmpty());
        verifyNoMoreInteractions(repository);
    }

    /** A newly issued browser identifier is opaque, protected and persisted only as a hash. */
    @Test
    void issuesProtectedCookieAndQueuesOnlyNewEvents() {
        DeviceEntity event = event();
        when(repository.recordLogin(anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull()))
            .thenReturn(event);

        AdminDeviceService.recordSuccessfulLogin(user, request, response);

        Cookie cookie = response.getCookie(AdminDeviceService.COOKIE_NAME);
        verify(browsers.constructed().get(0)).parse(request);
        assertNotNull(cookie);
        assertTrue(cookie.getValue().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(32, java.util.Base64.getUrlDecoder().decode(cookie.getValue()).length);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(90 * 86400, cookie.getMaxAge());
        verify(repository).recordLogin(7, AdminDeviceService.hashToken(cookie.getValue()), NOW,
            NOW - Duration.ofDays(90).toMillis(), "Firefox", "131.0", "Windows 11", "192.0.2.1", null);
        verify(service).sendNotification(user, request, event);
        assertEquals(event.getId(), request.getSession().getAttribute(AdminDeviceService.SESSION_DEVICE_ID));
    }

    /** A recognized browser retains its cookie and does not send another notification. */
    @Test
    void renewsKnownCookieWithoutAnotherNotification() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        when(repository.findByTokenHash(7, AdminDeviceService.hashToken(TOKEN))).thenReturn(event());

        AdminDeviceService.recordSuccessfulLogin(user, request, response);

        assertEquals(TOKEN, response.getCookie(AdminDeviceService.COOKIE_NAME).getValue());
        verify(repository).recordLogin(eq(7), eq(AdminDeviceService.hashToken(TOKEN)), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull());
        verify(service, never()).sendNotification(any(), any(), any());
        assertEquals(42L, request.getSession().getAttribute(AdminDeviceService.SESSION_DEVICE_ID));
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
        verify(repository).recordLogin(eq(7), anyString(), eq(NOW), eq(NOW - Duration.ofDays(30).toMillis()), anyString(), anyString(), anyString(), anyString(), isNull());
    }

    /** Both lists must match before database writes, browser cookies and notifications are skipped. */
    @ParameterizedTest
    @ValueSource(strings = {"demo.webjetcms.sk", "iwcm.interway.sk", "test.cms.interway.sk", "localhost", "localhost:8080"})
    void skipsExactUserAgentOnlyOnFixedHosts(String host) {
        prepareHeadlessLogin(host);
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        request.getSession().setAttribute(AdminDeviceService.SESSION_DEVICE_ID, 99L);

        LogonTools.afterSuccessLogon(request, response);

        verifyNoInteractions(repository);
        verify(service, never()).sendNotification(any(), any(), any());
        assertTrue(browsers.constructed().isEmpty());
        assertEquals(0, response.getCookies().length);
        assertNull(request.getSession().getAttribute(AdminDeviceService.SESSION_DEVICE_ID));
    }

    /** Similar hostnames, parent domains and unrelated environments remain subject to detection. */
    @ParameterizedTest
    @ValueSource(strings = {"cms.example.com", "interway.sk", "evilinterway.sk", "iwcm.interway.sk.example.com",
        "demo.webjetcms.sk.example.com", "other.demo.webjetcms.sk", "localhost.example.com"})
    void recordsIgnoredUserAgentOutsideFixedHosts(String host) {
        prepareHeadlessLogin(host);

        assertDeviceRecorded();
    }

    /** User-Agent entries are literal full headers, including case, whitespace and version. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"HeadlessChrome 131.0", "HeadlessChrome/131.0.6778.33", "*HeadlessChrome*", "Autotest Browser", " "})
    void recordsUserAgentsWithoutAnExactEntry(String userAgent) {
        prepareHeadlessLogin("localhost");
        request.removeHeader("User-Agent");
        if (userAgent != null) request.addHeader("User-Agent", userAgent);

        assertDeviceRecorded();
    }

    /** A browser update or any other header difference requires an explicit code change. */
    @ParameterizedTest
    @ValueSource(strings = {"version", "case", "whitespace", "device-test"})
    void recordsChangedFullUserAgent(String change) {
        prepareHeadlessLogin("localhost");
        String userAgent = switch (change) {
            case "version" -> HEADLESS_USER_AGENT.replace("131.0.6778.33", "132.0.6778.33");
            case "case" -> HEADLESS_USER_AGENT.replace("HeadlessChrome", "headlesschrome");
            case "device-test" -> HEADLESS_USER_AGENT + " WebJET-autotest-new-device";
            default -> HEADLESS_USER_AGENT + " ";
        };
        request.removeHeader("User-Agent");
        request.addHeader("User-Agent", userAgent);

        assertDeviceRecorded();
    }

    /** The locally verified macOS E2E browser is also excluded on the fixed test hosts. */
    @Test
    void skipsMacHeadlessUserAgent() {
        prepareHeadlessLogin("localhost");
        request.removeHeader("User-Agent");
        request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/131.0.6778.33 Safari/537.36");

        AdminDeviceService.recordSuccessfulLogin(user, request, response);

        verifyNoInteractions(repository);
        verify(service, never()).sendNotification(any(), any(), any());
        assertEquals(0, response.getCookies().length);
    }

    /** Legacy checks read only the current account's block and never start an email challenge. */
    @Test
    void checksExistingBlockWithoutChangingDeviceOrStartingVerification() {
        DeviceEntity device = event();
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        when(repository.findByTokenHash(7, AdminDeviceService.hashToken(TOKEN))).thenReturn(device);
        assertFalse(AdminDeviceService.isBlocked(user, request));

        device.setReportedAt(Instant.ofEpochMilli(NOW - 1));
        assertTrue(AdminDeviceService.isBlocked(user, request));
        when(user.getUserId()).thenReturn(8);
        assertFalse(AdminDeviceService.isBlocked(user, request), "Another account must not inherit this device's block");

        verify(repository, times(2)).findByTokenHash(7, AdminDeviceService.hashToken(TOKEN));
        verify(repository).findByTokenHash(8, AdminDeviceService.hashToken(TOKEN));
        verifyNoMoreInteractions(repository);
        verify(service, never()).sendVerificationEmail(any(), any(), any(), any(), any());
        assertNull(request.getSession().getAttribute(AdminDeviceService.PENDING_LOGIN));
        assertNull(request.getSession().getAttribute(Constants.USER_KEY));
        assertEquals(0, response.getCookies().length);
    }

    /** Missing, empty and malformed browser cookies cannot identify a blocked device. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"malformed"})
    void legacyBlockCheckIgnoresUnrecognizedCookies(String token) {
        if (token != null) request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, token));
        assertFalse(AdminDeviceService.isBlocked(user, request));
        verifyNoInteractions(repository);
    }

    /** Disabled detection and non-administrator accounts do not query device state. */
    @Test
    void legacyBlockCheckHonorsFeatureAndAccountScope() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        assertFalse(AdminDeviceService.isBlocked(null, request));
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(false);
        assertFalse(AdminDeviceService.isBlocked(user, request));
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
        when(user.isAdmin()).thenReturn(false);
        assertFalse(AdminDeviceService.isBlocked(user, request));
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(0);
        assertFalse(AdminDeviceService.isBlocked(user, request));
        verifyNoInteractions(repository);
    }

    /** Lookup failures must reach the legacy caller instead of being treated as an unblocked device. */
    @Test
    void legacyBlockCheckPropagatesLookupFailure() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        when(repository.findByTokenHash(anyInt(), anyString())).thenThrow(new IllegalStateException("Unavailable"));
        assertThrows(IllegalStateException.class, () -> AdminDeviceService.isBlocked(user, request));
    }

    /** A blocked cookie clears both authentication contexts and accepts only this login's emailed code. */
    @Test
    void blockedBrowserWaitsOutsideAuthenticationUntilItsOwnEmailCodeSucceeds() {
        DeviceEntity device = event();
        device.setReportedAt(Instant.ofEpochMilli(NOW - 1));
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        request.getSession().setAttribute(Constants.USER_KEY, user);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        when(repository.findByTokenHash(7, AdminDeviceService.hashToken(TOKEN))).thenReturn(device);
        when(repository.findEvent(7, 42L)).thenReturn(device);
        when(repository.issueUnblockCode(eq(7), eq(42L), anyString(), eq(NOW))).thenReturn(true);
        tools.when(() -> Tools.isNotEmpty(user.getEmail())).thenReturn(true);
        doReturn(true).when(service).sendVerificationEmail(eq(user), eq(request), eq(device), isNull(), anyString());

        assertTrue(AdminDeviceService.requireVerification(request));
        assertNull(request.getSession().getAttribute(Constants.USER_KEY));
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(service.getPendingLogin(request));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(service).sendVerificationEmail(eq(user), eq(request), eq(device), isNull(), code.capture());
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(repository).issueUnblockCode(eq(7), eq(42L), hash.capture(), eq(NOW));
        assertNotEquals(AdminDeviceService.confirmationHash(7, 42L, code.getValue(), true), hash.getValue());

        users.when(() -> UsersDB.getUser(7)).thenReturn(user);
        when(user.isAuthorized()).thenReturn(true);
        when(repository.unblock(7, 42L, hash.getValue(), NOW)).thenReturn(device);
        try (var logon = mockStatic(LogonTools.class)) {
            logon.when(() -> LogonTools.logonUserWithAllChecks(any(Identity.class), eq(request))).thenAnswer(invocation -> {
                request.getSession().setAttribute(Constants.USER_KEY, invocation.getArgument(0));
                return null;
            });
            assertTrue(service.verifyUnblockCode(request, code.getValue()));
            assertNull(service.getPendingLogin(request));
            assertNotNull(request.getSession().getAttribute(Constants.USER_KEY));
            assertFalse(service.verifyUnblockCode(request, code.getValue()), "A completed login must not accept the code again");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** Cookie replacement and challenge expiration discard the pending identity. */
    @Test
    void pendingLoginRequiresTheOriginalBrowserAndHasAFixedDeadline() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        var pending = new AdminDeviceService.PendingLogin(user, 42L, AdminDeviceService.hashToken(TOKEN), "nonce", NOW + 1);
        request.getSession().setAttribute(AdminDeviceService.PENDING_LOGIN, pending);
        assertSame(pending, service.getPendingLogin(request));
        var later = new AdminDeviceService(repository, Clock.fixed(Instant.ofEpochMilli(NOW + 1), ZoneOffset.UTC));
        assertNull(later.getPendingLogin(request));
        request.getSession().setAttribute(AdminDeviceService.PENDING_LOGIN, pending);
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, "x".repeat(43)));
        assertNull(service.getPendingLogin(request));
        verifyNoInteractions(repository);
    }

    /** A failed blocking lookup cannot leave an authenticated identity behind. */
    @Test
    void blockingLookupFailureRemovesAuthentication() {
        request.setCookies(new Cookie(AdminDeviceService.COOKIE_NAME, TOKEN));
        request.getSession().setAttribute(Constants.USER_KEY, user);
        when(repository.findByTokenHash(anyInt(), anyString())).thenThrow(new IllegalStateException("Unavailable"));
        assertThrows(IllegalStateException.class, () -> AdminDeviceService.requireVerification(request));
        assertNull(request.getSession().getAttribute(Constants.USER_KEY));
    }

    /** New browsers receive informational mail after the actual second factor confirms them. */
    @Test
    void verifiedSecondFactorConfirmsTheRecordedDevice() {
        DeviceEntity device = event();
        when(repository.recordLogin(anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull())).thenReturn(device);
        when(repository.confirmAfterSecondFactor(7, 42L, NOW)).thenAnswer(invocation -> {
            device.setConfirmedAt(Instant.ofEpochMilli(NOW));
            return device;
        });
        request.setAttribute(AdminDeviceService.SECOND_FACTOR_VERIFIED, Boolean.TRUE);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        assertNotNull(device.getConfirmedAt());
        verify(service).sendNotification(user, request, device);
        doCallRealMethod().when(service).sendNotification(user, request, device);
        doReturn(true).when(service).sendVerificationEmail(user, request, device, null, null);
        service.sendNotification(user, request, device);
        verify(service).sendVerificationEmail(user, request, device, null, null);
        verify(repository, never()).issueConfirmation(anyInt(), anyLong(), anyString(), anyLong(), anyBoolean());
    }

    /** Blocking signs out matching local and remote sessions without touching other devices or accounts. */
    @Test
    void reportUsesExistingLogoutForAllKnownDeviceSessions() {
        DeviceEntity device = event();
        device.setReportedAt(Instant.ofEpochMilli(NOW));
        when(repository.report(7, 42L, NOW)).thenReturn(device);
        SessionHolder holder = mock(SessionHolder.class);
        SessionDetails local = new SessionDetails();
        local.setSessionId("local"); local.setLoggedUserId(7); local.setDeviceId(42L);
        SessionDetails otherDevice = new SessionDetails();
        otherDevice.setSessionId("other"); otherDevice.setLoggedUserId(7); otherDevice.setDeviceId(43L);
        SessionDetails foreign = new SessionDetails();
        foreign.setSessionId("foreign"); foreign.setLoggedUserId(8); foreign.setDeviceId(42L);
        SessionDetails remote = new SessionDetails();
        remote.setSessionId("remote"); remote.setLoggedUserId(7); remote.setDeviceId(42L);
        when(holder.getList()).thenReturn(List.of(local, otherDevice, foreign));
        try (var holders = mockStatic(SessionHolder.class); var cluster = mockStatic(SessionClusterService.class)) {
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            cluster.when(() -> SessionClusterService.getSessionsForUsers(Set.of(7))).thenReturn(List.of(local, remote));
            assertSame(device, service.report(user, EVENT_ID));
            verify(holder).invalidateSession(7, "local");
            verify(holder).invalidateSession(7, "remote");
            verify(holder, times(2)).invalidateSession(anyInt(), anyString());
        }
    }

    private void prepareHeadlessLogin(String host) {
        tools.when(() -> Tools.getServerName(request, false)).thenCallRealMethod();
        request.setServerName(host);
        request.removeHeader("User-Agent");
        request.addHeader("User-Agent", HEADLESS_USER_AGENT);
    }

    private void assertDeviceRecorded() {
        DeviceEntity event = event();
        when(repository.recordLogin(anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull()))
            .thenReturn(event);

        AdminDeviceService.recordSuccessfulLogin(user, request, response);

        verify(repository).recordLogin(eq(7), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull());
        verify(service).sendNotification(user, request, event);
        assertNotNull(response.getCookie(AdminDeviceService.COOKIE_NAME));
        assertEquals(event.getId(), request.getSession().getAttribute(AdminDeviceService.SESSION_DEVICE_ID));
    }

    @Test
    void disabledFeatureAndNonAdministratorsDoNotReadOrWriteDevices() {
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(false);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        assertTrue(service.getActiveEvents(user).isEmpty());
        assertNull(service.findEvent(user, EVENT_ID));
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
        when(user.isAdmin()).thenReturn(false);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        assertThrows(AccessDeniedException.class, () -> service.confirm(user, EVENT_ID, TOKEN, false));
        verifyNoInteractions(repository);
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void storageFailureCannotRejectSuccessfulAuthentication() {
        request.getSession().setAttribute(AdminDeviceService.SESSION_DEVICE_ID, 99L);
        when(repository.recordLogin(anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull()))
            .thenThrow(new IllegalStateException("Autotest storage unavailable"));
        try (var logger = mockStatic(Logger.class)) {
            assertDoesNotThrow(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
        }
        verify(service, never()).sendNotification(any(), any(), any());
        assertNull(request.getSession().getAttribute(AdminDeviceService.SESSION_DEVICE_ID));
    }

    /** Device state comes from the current account's database records, including notices older than seven days. */
    @Test
    void enrichesOnlyOwnedSessionDevicesWithFreshConfirmation() throws Exception {
        var sessions = new ObjectMapper().readTree("""
            {"userSessions":[{"userSessions":[
                {"deviceId":42},{"deviceId":43},{"deviceId":42},{"deviceId":99},{}
            ]}]}
            """);
        DeviceEntity unconfirmed = event();
        unconfirmed.setCreateDate(Instant.ofEpochMilli(NOW - Duration.ofDays(8).toMillis()));
        DeviceEntity confirmed = event();
        confirmed.setId(43L);
        confirmed.setConfirmedAt(Instant.ofEpochMilli(NOW));
        when(repository.findByIds(7, Set.of(42L, 43L, 99L))).thenReturn(List.of(unconfirmed, confirmed));

        service.addSessionDeviceStatus(user, sessions);

        var rows = sessions.path("userSessions").get(0).path("userSessions");
        assertFalse(rows.get(0).path("deviceConfirmed").asBoolean(true));
        assertTrue(rows.get(1).path("deviceConfirmed").asBoolean());
        assertFalse(rows.get(2).path("deviceConfirmed").asBoolean(true));
        assertFalse(rows.get(3).has("deviceConfirmed"), "Foreign devices must not receive an actionable state");
        assertFalse(rows.get(4).has("deviceConfirmed"), "Older sessions without a device must remain usable");
        verify(repository).findByIds(7, Set.of(42L, 43L, 99L));
        assertFalse(sessions.toString().contains("tokenHash"));
    }

    /** Disabled detection and unauthorized accounts cannot load confirmation metadata. */
    @Test
    void gatesSessionDeviceStateByFeatureAndAdministrator() throws Exception {
        var sessions = new ObjectMapper().readTree("{\"userSessions\":[{\"userSessions\":[{\"deviceId\":42}]}]}");
        constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(false);
        service.addSessionDeviceStatus(user, sessions);
        assertThrows(AccessDeniedException.class, () -> service.addSessionDeviceStatus(null, sessions));
        verifyNoInteractions(repository);
    }

    @Test
    void recordsAndNotifiesWhenBrowserRejectsCookie() {
        tools.when(() -> Tools.addCookie(any(), any(), any())).thenReturn(false);
        DeviceEntity event = event();
        when(repository.recordLogin(anyInt(), anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), isNull()))
            .thenReturn(event);
        AdminDeviceService.recordSuccessfulLogin(user, request, response);
        verify(service, times(1)).sendNotification(user, request, event);
        assertEquals(0, response.getCookies().length);
    }

    @Test
    void rejectsMalformedEventIdsBeforeQueryingAndDerivesAccountScope() {
        assertNull(service.confirm(user, "foreign-or-invalid", TOKEN, false));
        assertNull(service.report(user, "../event"));
        assertNull(service.findEvent(user, null));
        assertNull(service.findEvent(user, "0"));
        assertNull(service.findEvent(user, "-1"));
        assertNull(service.findEvent(user, "9223372036854775808"));
        verifyNoInteractions(repository);
        service.findEvent(user, EVENT_ID);
        service.confirm(user, EVENT_ID, TOKEN, false);
        service.report(user, EVENT_ID);
        verify(repository).findEvent(7, 42L);
        verify(repository).confirm(7, 42L, AdminDeviceService.confirmationHash(7, 42L, TOKEN, false), NOW, false);
        verify(repository).report(7, 42L, NOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://attacker.test/admin/v9/", "//attacker.test/admin/v9/", "/admin/v9/../../outside", "/admin/v9/%2e%2e/outside", "/admin/v9/\\attacker", "/admin/v9/?x=%0d%0aLocation:attacker", "/public/", "/admin/v9/%zz"})
    void rejectsUnsafeReturnTargets(String target) {
        request.getSession().setAttribute("adminAfterLogonRedirect", target);
        assertEquals("/admin/v9/", AdminDeviceService.getAfterLoginRedirect(request));
    }

    @Test
    void preservesAndConsumesTheEmailReturnTarget() {
        String target = "/admin/v9/?securityEvent=" + EVENT_ID + "&deviceConfirmation=" + TOKEN;
        request.getSession().setAttribute("adminAfterLogonRedirect", target);
        assertEquals(target, AdminDeviceService.getAfterLoginRedirect(request));
        assertEquals("/admin/v9/", AdminDeviceService.getAfterLoginRedirect(request));
    }

    /** Mail uses password-reset sender resolution, escaped snapshots and a read-only detail link. */
    @Test
    void queuesEscapedEmailWithMatchingPasswordResetSenderAndReadOnlyLink() {
        doCallRealMethod().when(service).sendNotification(any(), any(), any());
        when(repository.issueConfirmation(eq(7), eq(42L), anyString(), eq(NOW), eq(false))).thenReturn(true);
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
            assertFalse(html.getValue().contains("/report"));
            var matcher = java.util.regex.Pattern.compile("deviceConfirmation=([A-Za-z0-9_-]{43})").matcher(html.getValue());
            assertTrue(matcher.find());
            verify(repository).issueConfirmation(7, 42L, AdminDeviceService.confirmationHash(7, 42L, matcher.group(1), false), NOW, false);
        }
    }

    /** UI requests deliver an unpredictable six-digit proof without confirming the device. */
    @Test
    void sendsCodeOnlyToAccountEmailAndEnforcesCooldown() {
        when(repository.findEvent(7, 42L)).thenReturn(event());
        when(repository.issueConfirmation(eq(7), eq(42L), anyString(), eq(NOW), eq(true))).thenReturn(true);
        doReturn(true).when(service).sendVerificationEmail(any(), any(), any(), isNull(), anyString());
        service.requestCode(user, EVENT_ID, request);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(service).sendVerificationEmail(eq(user), eq(request), any(), isNull(), code.capture());
        assertTrue(code.getValue().matches("[0-9]{6}"));
        verify(repository).issueConfirmation(7, 42L, AdminDeviceService.confirmationHash(7, 42L, code.getValue(), true), NOW, true);
        verify(repository, never()).confirm(anyInt(), anyLong(), anyString(), anyLong(), anyBoolean());
        when(repository.issueConfirmation(eq(7), eq(42L), anyString(), eq(NOW), eq(true))).thenReturn(false);
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.requestCode(user, EVENT_ID, request));
        assertEquals(429, error.getStatusCode().value());
        verify(service, times(1)).sendVerificationEmail(any(), any(), any(), isNull(), anyString());
    }

    /** Missing and malformed proofs cannot reach the persistence service. */
    @Test
    void rejectsIdOnlyOrMalformedConfirmation() {
        assertNull(service.confirm(user, EVENT_ID, null, false));
        assertNull(service.confirm(user, EVENT_ID, "123456", false));
        assertNull(service.confirm(user, EVENT_ID, "12345", true));
        assertNull(service.confirm(user, EVENT_ID, "1234567", true));
        verifyNoInteractions(repository);
        service.confirm(user, EVENT_ID, "012345", true);
        verify(repository).confirm(7, 42L, AdminDeviceService.confirmationHash(7, 42L, "012345", true), NOW, true);
    }

    private static DeviceEntity event() {
        DeviceEntity device = new DeviceEntity();
        device.setId(Long.parseLong(EVENT_ID));
        device.setCreateDate(Instant.ofEpochMilli(NOW));
        device.setLastSeen(Instant.ofEpochMilli(NOW));
        device.setBrowserName("Firefox");
        device.setBrowserVersion("131.0");
        device.setOperatingSystem("Windows 11");
        device.setIpAddress("192.0.2.1");
        return device;
    }
}
