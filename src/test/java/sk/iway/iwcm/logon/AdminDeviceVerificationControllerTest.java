package sk.iway.iwcm.logon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies the unauthenticated email form's CSRF, cancellation and unsuccessful submission boundaries. */
class AdminDeviceVerificationControllerTest {
    private static final String FORM = "/admin/skins/webjet8/logon-spring-device";
    private final AdminLogonController controller = new AdminLogonController(mock(UserDetailsRepository.class));
    private final AdminDeviceService service = mock(AdminDeviceService.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final ModelMap model = new ModelMap();
    private MockedStatic<Tools> tools;
    private MockedStatic<CSRF> csrf;
    private MockedStatic<Prop> props;
    private MockedStatic<LogonTools> logon;

    @BeforeEach
    void setUp() {
        tools = mockStatic(Tools.class);
        tools.when(() -> Tools.getSpringBean("adminDeviceService", AdminDeviceService.class)).thenReturn(service);
        csrf = mockStatic(CSRF.class);
        csrf.when(() -> CSRF.verifyTokenAjax(request)).thenReturn(true);
        props = mockStatic(Prop.class);
        Prop prop = mock(Prop.class);
        props.when(() -> Prop.getInstance(request)).thenReturn(prop);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        logon = mockStatic(LogonTools.class);
        Identity user = new Identity();
        user.setEmail("autotest@example.test");
        when(service.getPendingLogin(request)).thenReturn(new AdminDeviceService.PendingLogin(user, 42, "hash", "nonce", Long.MAX_VALUE));
    }

    @AfterEach
    void tearDown() {
        logon.close();
        props.close();
        csrf.close();
        tools.close();
    }

    /** All form actions must reject a missing or invalid CSRF proof before accessing the challenge. */
    @Test
    void rejectsInvalidCsrf() {
        csrf.when(() -> CSRF.verifyTokenAjax(request)).thenReturn(false);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> submit("verify", "123456"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(service);
        logon.verifyNoInteractions();
    }

    /** Expired or absent pending identities return to credentials without checking a code. */
    @Test
    void requiresPendingLogin() {
        when(service.getPendingLogin(request)).thenReturn(null);
        assertEquals("redirect:/admin/logon/", submit("verify", "123456"));
        verify(service, never()).verifyUnblockCode(any(), any());
        logon.verifyNoInteractions();
    }

    /** Cancellation discards the whole pending session without completing authentication. */
    @Test
    void cancelInvalidatesPendingSession() {
        MockHttpSession session = (MockHttpSession) request.getSession();
        assertEquals("redirect:/admin/logon/", submit("cancel", null));
        assertTrue(session.isInvalid());
        logon.verifyNoInteractions();
    }

    /** Resending a code stays on the challenge form and masks the account's mailbox. */
    @Test
    void resendDoesNotCompleteLogin() {
        assertEquals(FORM, submit("resend", null));
        verify(service).requestUnblockCode(request);
        assertEquals("a***@example.test", model.get("deviceEmail"));
        logon.verifyNoInteractions();
    }

    /** Incorrect email codes cannot run successful-login callbacks. */
    @Test
    void incorrectCodeKeepsTheChallenge() {
        assertEquals(FORM, submit("verify", "000000"));
        assertEquals("admin.dashboard.newDevice.codeInvalid.js", model.get("deviceError"));
        logon.verifyNoInteractions();
    }

    private String submit(String action, String code) {
        return controller.verifyDevice(action, code, model, request, response);
    }
}
