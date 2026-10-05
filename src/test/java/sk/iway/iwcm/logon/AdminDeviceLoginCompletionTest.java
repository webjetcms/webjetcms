package sk.iway.iwcm.logon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.googleauth.GoogleAuthenticator;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies that administrator device recognition waits for all required authentication steps. */
class AdminDeviceLoginCompletionTest {
    private static final String EVENT_TARGET = "/admin/v9/?securityEvent=23";
    private static final String TWO_FACTOR_FORM = "/admin/skins/webjet8/logon-spring-2fa";
    private static final String PASSWORD_FORM = "/admin/skins/webjet8/logon-spring-change-password";
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final UserDetailsRepository repository = mock(UserDetailsRepository.class);
    private final AdminLogonController controller = new AdminLogonController(repository);
    private final UserForm form = new UserForm();
    private final List<MockedStatic<?>> staticMocks = new ArrayList<>();
    private MockedStatic<LogonTools> logon;
    private MockedStatic<Password> passwords;
    private MockedStatic<AdminDeviceService> devices;

    @BeforeEach
    void setUp() {
        request.setMethod("POST");
        form.setUsername("device-test-admin");
        form.setPassword("old-password");
        MockedStatic<Constants> constants = staticMock(Constants.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(Constants::getInstallName).thenReturn("autotest");
        MockedStatic<InitServlet> init = staticMock(InitServlet.class);
        init.when(() -> InitServlet.verify(request)).thenReturn(true);
        MockedStatic<Prop> props = staticMock(Prop.class);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        props.when(() -> Prop.getInstance(request.getServletContext(), request)).thenReturn(prop);
        logon = staticMock(LogonTools.class);
        logon.when(() -> LogonTools.setUserToSession(any(), any())).thenAnswer(invocation -> {
            request.getSession().setAttribute(Constants.USER_KEY, invocation.getArgument(1));
            return null;
        });
        logon.when(() -> LogonTools.afterSuccessLogon(request, response)).thenCallRealMethod();
        logon.when(() -> LogonTools.logon(eq(form.getUsername()), eq(form.getPassword()), any(Identity.class), any(), eq(request), eq(prop)))
            .thenAnswer(invocation -> {
                Identity user = invocation.getArgument(2);
                user.setUserId(7);
                user.setAdmin(true);
                return "logon_ok_admin";
            });
        passwords = staticMock(Password.class);
        passwords.when(() -> Password.checkPassword(anyBoolean(), anyString(), anyBoolean(), anyInt(), any(), any())).thenReturn(true);
        MockedStatic<DocDB> docs = staticMock(DocDB.class);
        DocDB docDB = mock(DocDB.class);
        when(docDB.getDocsForApprove(anyInt())).thenReturn(List.of());
        docs.when(DocDB::getInstance).thenReturn(docDB);
        staticMock(PageLng.class);
        staticMock(StatDB.class);
        staticMock(Adminlog.class);
        staticMock(UserDetailsService.class);
        devices = staticMock(AdminDeviceService.class);
        devices.when(() -> AdminDeviceService.getAfterLoginRedirect(request)).thenReturn(EVENT_TARGET);
    }

    @AfterEach
    void tearDown() {
        for (int index = staticMocks.size() - 1; index >= 0; index--) staticMocks.get(index).close();
    }

    /** A normal successful login records the authenticated browser and preserves the security-event destination. */
    @Test
    void recordsNormalLoginBeforeRedirect() {
        assertEquals("redirect:" + EVENT_TARGET, submit());
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(any(Identity.class), eq(request), eq(response)));
    }

    /** Rejected credentials cannot register a browser or consume an after-login destination. */
    @Test
    void ignoresFailedPasswordAuthentication() {
        logon.when(() -> LogonTools.logon(eq(form.getUsername()), eq(form.getPassword()), any(), any(), eq(request), any()))
            .thenAnswer(invocation -> {
                Map<String, String> errors = invocation.getArgument(3);
                errors.put("ERROR_KEY", "Invalid credentials");
                return "logon_ok_admin";
            });
        assertEquals("/admin/skins/webjet8/logon-spring", submit());
        devices.verifyNoInteractions();
        logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
    }

    /** Password authentication alone must not recognize the browser while a second factor is pending. */
    @Test
    void waitsForSecondFactor() {
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("existing-secret");
        assertEquals(TWO_FACTOR_FORM, submit());
        assertNotNull(request.getSession().getAttribute("adminUser_waitingForToken"));
        devices.verifyNoInteractions();
        logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
    }

    /** An incorrect second factor leaves the browser unrecognized and keeps the challenge active. */
    @Test
    void rejectsIncorrectSecondFactorWithoutRecording() {
        prepareSecondFactor();
        request.setParameter("token", "111111");
        try (var authenticators = mockConstruction(GoogleAuthenticator.class,
                (authenticator, context) -> when(authenticator.getTotpPassword("existing-secret")).thenReturn(222222))) {
            assertEquals(TWO_FACTOR_FORM, submit());
            assertNotNull(request.getSession().getAttribute("adminUser_waitingForToken"));
            devices.verifyNoInteractions();
            logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
        }
    }

    /** A verified second factor records the restored identity and retains the email's security-event link. */
    @Test
    void recordsOnlyAfterCorrectSecondFactor() {
        Identity user = prepareSecondFactor();
        request.setParameter("token", "222222");
        try (var authenticators = mockConstruction(GoogleAuthenticator.class,
                (authenticator, context) -> when(authenticator.getTotpPassword("existing-secret")).thenReturn(222222));
             var queries = mockConstruction(SimpleQuery.class,
                (query, context) -> when(query.forString(anyString(), any())).thenReturn("existing-secret"))) {
            assertEquals("redirect:" + EVENT_TARGET, submit());
            assertNull(request.getSession().getAttribute("adminUser_waitingForToken"));
            devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
        }
    }

    /** A mandatory password change delays device recognition until its successful completion. */
    @Test
    void waitsForRequiredPasswordChange() {
        passwords.when(() -> Password.checkPassword(true, form.getPassword(), true, 7, request.getSession(), null)).thenReturn(false);
        assertEquals(PASSWORD_FORM, submit());
        assertNotNull(request.getSession().getAttribute(Constants.USER_KEY + "_changepassword"));
        devices.verifyNoInteractions();
        logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
    }

    /** Completing a mandatory password change must still require the account's configured second factor. */
    @Test
    void requiredPasswordChangeDoesNotSkipSecondFactor() {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        user.setValid(true);
        request.getSession().setAttribute(Constants.USER_KEY + "_changepassword", user);
        form.setNewPassword("new-password");
        form.setRetypeNewPassword("new-password");
        when(repository.getPasswordByUserId(7L)).thenReturn("old-password");
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("existing-secret");

        assertEquals(TWO_FACTOR_FORM, controller.edit(form, new ModelMap(), request, response, request.getSession()));
        assertEquals(user, request.getSession().getAttribute("adminUser_waitingForToken"));
        devices.verifyNoInteractions();
        logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
    }

    /** A completed mandatory password change recognizes the device immediately when no second factor is required. */
    @Test
    void recordsCompletedPasswordChangeWithoutSecondFactor() {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        user.setValid(true);
        request.getSession().setAttribute(Constants.USER_KEY + "_changepassword", user);
        form.setNewPassword("new-password");
        form.setRetypeNewPassword("new-password");
        when(repository.getPasswordByUserId(7L)).thenReturn("old-password");

        assertEquals("redirect:" + EVENT_TARGET, controller.edit(form, new ModelMap(), request, response, request.getSession()));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
    }

    private Identity prepareSecondFactor() {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        user.setValid(true);
        request.getSession().setAttribute("token", "existing-secret");
        request.getSession().setAttribute("adminUser_waitingForToken", user);
        return user;
    }

    private String submit() {
        return controller.submit(form, mock(BindingResult.class), new ModelMap(), request, response);
    }

    private <T> MockedStatic<T> staticMock(Class<T> type) {
        MockedStatic<T> value = mockStatic(type);
        staticMocks.add(value);
        return value;
    }
}
