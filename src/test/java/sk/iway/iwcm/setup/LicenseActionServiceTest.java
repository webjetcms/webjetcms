package sk.iway.iwcm.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

class LicenseActionServiceTest {

    private static final int USER_ID = 42;
    private static final String ERROR_VIEW = "/admin/setup/license";
    private static final String BAD_CREDENTIALS = "Invalid credentials";
    private static final String PASSWORD_QUERY = "SELECT password FROM users WHERE user_id=?";
    private static final String SALT_QUERY = "SELECT password_salt FROM users WHERE user_id=?";
    private static final String LAST_LOGON_QUERY = "UPDATE  users SET last_logon=? WHERE user_id=?";

    @Test
    void blockedRequestDoesNotReadCredentialsOrIncreaseFailedAttemptCount() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LicenseFormBean form = licenseForm();
        ExtendedModelMap model = new ExtendedModelMap();
        Prop prop = mock(Prop.class);
        when(prop.getText("logon.error.blocked")).thenReturn("Login blocked");

        try (MockedStatic<Prop> properties = mockStatic(Prop.class);
                MockedStatic<FileTools> files = mockStatic(FileTools.class);
                MockedStatic<LogonTools> logonTools = mockStatic(LogonTools.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class)) {
            properties.when(Prop::getInstance).thenReturn(prop);
            files.when(() -> FileTools.readFileContent("/components/cmp.css")).thenReturn("");
            logonTools.when(() -> LogonTools.isLoginBlocked(request)).thenReturn(true);

            String view = LicenseActionService.updateLicense(
                form, model, request, new MockHttpServletResponse()
            );

            assertEquals(ERROR_VIEW, view);
            assertEquals("Login blocked", model.get("licenseErrMsg"));
            assertNull(form.getPassword());
            users.verify(() -> UsersDB.getUser("editor"), never());
            logonTools.verify(() -> LogonTools.setLoginBlocked(request), never());
            assertEquals(0, queries.constructed().size());
        }
    }

    @Test
    void wrongPasswordAndCorrectNonAdminPasswordHaveSameResponse() {
        AuthFailure wrongPassword = authenticate(false, true);
        AuthFailure correctNonAdminPassword = authenticate(true, false);

        assertEquals(wrongPassword.view(), correctNonAdminPassword.view());
        assertEquals(wrongPassword.message(), correctNonAdminPassword.message());
        assertEquals(BAD_CREDENTIALS, wrongPassword.message());
    }

    @Test
    void unknownUserGetsGenericErrorAndIncreasesFailedAttemptCount() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LicenseFormBean form = licenseForm();
        ExtendedModelMap model = new ExtendedModelMap();
        Prop prop = mock(Prop.class);
        when(prop.getText("approveAction.err.badPass")).thenReturn(BAD_CREDENTIALS);

        try (MockedStatic<Prop> properties = mockStatic(Prop.class);
                MockedStatic<FileTools> files = mockStatic(FileTools.class);
                MockedStatic<LogonTools> logonTools = mockStatic(LogonTools.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class)) {
            properties.when(Prop::getInstance).thenReturn(prop);
            files.when(() -> FileTools.readFileContent("/components/cmp.css")).thenReturn("");
            logonTools.when(() -> LogonTools.isLoginBlocked(request)).thenReturn(false);
            users.when(() -> UsersDB.getUser("editor")).thenReturn(null);

            String view = LicenseActionService.updateLicense(
                form, model, request, new MockHttpServletResponse()
            );

            assertEquals(ERROR_VIEW, view);
            assertEquals(BAD_CREDENTIALS, model.get("licenseErrMsg"));
            assertNull(form.getPassword());
            logonTools.verify(() -> LogonTools.setLoginBlocked(request));
            assertEquals(0, queries.constructed().size());
        }
    }

    @Test
    void authenticatedAdminWithInvalidLicenseDoesNotIncreaseFailedAttemptCount() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LicenseFormBean form = licenseForm();
        form.setLicense(null);
        ExtendedModelMap model = new ExtendedModelMap();
        Prop prop = mock(Prop.class);
        UserDetails user = mock(UserDetails.class);
        when(prop.getText("setup.license.invalid_license")).thenReturn("Invalid license");
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.isAuthorized()).thenReturn(true);
        when(user.isAdmin()).thenReturn(true);
        when(user.getEditablePages()).thenReturn("");
        when(user.getEditableGroups()).thenReturn("");

        try (MockedStatic<Prop> properties = mockStatic(Prop.class);
                MockedStatic<FileTools> files = mockStatic(FileTools.class);
                MockedStatic<LogonTools> logonTools = mockStatic(LogonTools.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> {
                        when(query.forString(PASSWORD_QUERY, USER_ID)).thenReturn("password-hash");
                        when(query.forString(SALT_QUERY, USER_ID)).thenReturn("password-salt");
                    })) {
            properties.when(Prop::getInstance).thenReturn(prop);
            files.when(() -> FileTools.readFileContent("/components/cmp.css")).thenReturn("");
            logonTools.when(() -> LogonTools.isLoginBlocked(request)).thenReturn(false);
            logonTools.when(() -> LogonTools.isPasswordCorrect(
                "secret", "password-salt", "password-hash"
            )).thenReturn(true);
            users.when(() -> UsersDB.getUser("editor")).thenReturn(user);

            String view = LicenseActionService.updateLicense(
                form, model, request, new MockHttpServletResponse()
            );

            assertEquals(ERROR_VIEW, view);
            assertEquals("Invalid license", model.get("licenseErrMsg"));
            assertNull(form.getPassword());
            logonTools.verify(() -> LogonTools.setLoginBlocked(request), never());
            assertEquals(3, queries.constructed().size());
            verify(queries.constructed().get(2)).execute(
                eq(LAST_LOGON_QUERY), any(Date.class), eq(USER_ID)
            );
        }
    }

    private AuthFailure authenticate(boolean passwordCorrect, boolean admin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LicenseFormBean form = licenseForm();
        ExtendedModelMap model = new ExtendedModelMap();
        Prop prop = mock(Prop.class);
        UserDetails user = mock(UserDetails.class);
        when(prop.getText("approveAction.err.badPass")).thenReturn(BAD_CREDENTIALS);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.isAuthorized()).thenReturn(true);
        when(user.isAdmin()).thenReturn(admin);

        try (MockedStatic<Prop> properties = mockStatic(Prop.class);
                MockedStatic<FileTools> files = mockStatic(FileTools.class);
                MockedStatic<LogonTools> logonTools = mockStatic(LogonTools.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> {
                        when(query.forString(PASSWORD_QUERY, USER_ID)).thenReturn("password-hash");
                        when(query.forString(SALT_QUERY, USER_ID)).thenReturn("password-salt");
                    })) {
            properties.when(Prop::getInstance).thenReturn(prop);
            files.when(() -> FileTools.readFileContent("/components/cmp.css")).thenReturn("");
            logonTools.when(() -> LogonTools.isLoginBlocked(request)).thenReturn(false);
            logonTools.when(() -> LogonTools.isPasswordCorrect(
                "secret", "password-salt", "password-hash"
            )).thenReturn(passwordCorrect);
            users.when(() -> UsersDB.getUser("editor")).thenReturn(user);

            String view = LicenseActionService.updateLicense(
                form, model, request, new MockHttpServletResponse()
            );

            assertEquals(ERROR_VIEW, view);
            assertEquals(BAD_CREDENTIALS, model.get("licenseErrMsg"));
            assertNull(form.getPassword());
            logonTools.verify(() -> LogonTools.setLoginBlocked(request));
            assertEquals(2, queries.constructed().size());
            verify(queries.constructed().get(0)).forString(PASSWORD_QUERY, USER_ID);
            verify(queries.constructed().get(1)).forString(SALT_QUERY, USER_ID);
            return new AuthFailure(view, (String) model.get("licenseErrMsg"));
        }
    }

    private LicenseFormBean licenseForm() {
        LicenseFormBean form = new LicenseFormBean();
        form.setUsername("editor");
        form.setPassword("secret");
        form.setLicense("license");
        return form;
    }

    private record AuthFailure(String view, String message) {
    }
}
