package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.FilterChain;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies device rejection before legacy browser logins establish an administrator session. */
@Execution(ExecutionMode.SAME_THREAD)
class PathFilterLegacyLoginTest extends BaseWebjetTest {
    private static final String PATH = "/admin/v9";
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);
    private MockedStatic<LogonTools> logon;
    private MockedStatic<AdminDeviceService> devices;
    private MockedStatic<DomainRedirectDB> redirects;
    private MockedStatic<DocDB> documents;

    @BeforeEach
    void prepareRequest() {
        request.setRequestURI(PATH);
        request.addHeader("Referer", "http://localhost/admin/logon/");
        request.getSession().setAttribute("pathFilter.checkAdmin", Boolean.TRUE);
        redirects = mockStatic(DomainRedirectDB.class);
        documents = mockStatic(DocDB.class);
        documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
        documents.when(() -> DocDB.getDomain(request)).thenReturn("localhost");
        logon = mockStatic(LogonTools.class);
        logon.when(() -> LogonTools.logon(eq("legacy-admin"), eq("test-password"), any(), any(), eq(request), any()))
            .thenAnswer(invocation -> {
                Identity user = invocation.getArgument(2);
                user.setUserId(7);
                user.setAdmin(true);
                return "logon_ok_admin";
            });
        logon.when(() -> LogonTools.setUserToSession(any(), any())).thenCallRealMethod();
        logon.when(() -> LogonTools.clearUserFromSession(any())).thenCallRealMethod();
        devices = mockStatic(AdminDeviceService.class);
    }

    @AfterEach
    void releaseMocks() {
        devices.close();
        logon.close();
        documents.close();
        redirects.close();
        SecurityContextHolder.clearContext();
    }

    /** All legacy input formats reject blocked devices without authenticating or starting a challenge. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "redir", "wjlogontoken"})
    void rejectsBlockedDeviceBeforeEstablishingSession(String mode) throws Exception {
        prepareCredentials(mode);
        devices.when(() -> AdminDeviceService.isBlocked(any(), eq(request))).thenReturn(true);

        new PathFilter().doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertRejected();
        verifyNoInteractions(chain);
        devices.verify(() -> AdminDeviceService.isBlocked(any(), eq(request)));
        devices.verifyNoMoreInteractions();
    }

    /** A failed lookup ends the request instead of falling through the filter's broad exception handler. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "redir", "wjlogontoken"})
    void rejectsLoginWhenDeviceStateCannotBeRead(String mode) throws Exception {
        prepareCredentials(mode);
        devices.when(() -> AdminDeviceService.isBlocked(any(), eq(request))).thenThrow(new IllegalStateException("Unavailable"));

        new PathFilter().doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertRejected();
        verifyNoInteractions(chain);
    }

    /** Unblocked browsers retain the legacy session and redirect behavior without an email challenge. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "redir", "wjlogontoken"})
    void preservesSuccessfulLegacyLogin(String mode) throws Exception {
        prepareCredentials(mode);

        new PathFilter().doFilter(request, response, chain);

        Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
        assertNotNull(user);
        assertEquals(7, user.getUserId());
        assertTrue(user.isValid());
        assertNull(request.getSession().getAttribute(AdminDeviceService.PENDING_LOGIN));
        devices.verify(() -> AdminDeviceService.isBlocked(user, request));
        devices.verifyNoMoreInteractions();
        if ("redir".equals(mode)) {
            assertEquals(PATH, response.getRedirectedUrl());
            verifyNoInteractions(chain);
        } else {
            assertEquals(200, response.getStatus());
            verify(chain).doFilter(request, response);
        }
    }

    /** Failed credentials never reach the device lookup or establish an authenticated session. */
    @ParameterizedTest
    @ValueSource(strings = {"true", "redir", "wjlogontoken"})
    void rejectedCredentialsDoNotCheckDevice(String mode) throws Exception {
        prepareCredentials(mode);
        logon.when(() -> LogonTools.logon(anyString(), anyString(), any(), any(), eq(request), any())).thenReturn("logon_error");

        new PathFilter().doFilter(request, response, chain);

        assertNull(request.getSession().getAttribute(Constants.USER_KEY));
        logon.verify(() -> LogonTools.setUserToSession(any(), any()), never());
        devices.verifyNoInteractions();
    }

    private void prepareCredentials(String mode) {
        if ("wjlogontoken".equals(mode)) {
            String token = Base64.getEncoder().encodeToString("legacy-admin:test-password".getBytes(StandardCharsets.UTF_8));
            request.addHeader("wjlogontoken", "A" + token.replace('=', '|'));
        } else {
            request.setParameter("doFilterLogon", mode);
            request.setParameter("username", "legacy-admin");
            request.setParameter("password", "test-password");
        }
    }

    private void assertRejected() {
        assertNull(request.getSession().getAttribute(Constants.USER_KEY));
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession().getAttribute(AdminDeviceService.PENDING_LOGIN));
        assertNull(response.getRedirectedUrl());
        logon.verify(() -> LogonTools.setUserToSession(any(), any()), never());
    }
}
