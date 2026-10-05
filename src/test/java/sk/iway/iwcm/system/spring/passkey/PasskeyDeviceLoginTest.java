package sk.iway.iwcm.system.spring.passkey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies browser recognition and safe redirect handling at the passkey authentication boundary. */
class PasskeyDeviceLoginTest {
    /** A verified administrator passkey records a device and uses the shared safe after-login destination. */
    @Test
    void recordsAuthenticatedAdministratorAndPreservesEventDestination() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("passkey-admin");
        UserDetails user = mock(UserDetails.class);
        when(user.isAdmin()).thenReturn(true);
        try (var users = mockStatic(UsersDB.class); var logon = mockStatic(LogonTools.class);
             var audit = mockStatic(Adminlog.class); var devices = mockStatic(AdminDeviceService.class);
             var identities = mockConstruction(Identity.class)) {
            users.when(() -> UsersDB.getUser("passkey-admin")).thenReturn(user);
            devices.when(() -> AdminDeviceService.getAfterLoginRedirect(request)).thenReturn("/admin/v9/?securityEvent=42");

            new PasskeyAuthSuccessHandler().onAuthenticationSuccess(request, response, authentication);

            assertEquals("/admin/v9/?securityEvent=42", response.getRedirectedUrl());
            logon.verify(() -> LogonTools.logonUserWithAllChecks(any(Identity.class), eq(request)));
            devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(identities.constructed().get(0), request, response));
        }
    }

    /** A valid passkey for a non-administrator cannot register an administrator device. */
    @Test
    void ignoresAccountsWithoutAdministratorAccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("website-user");
        try (var users = mockStatic(UsersDB.class); var devices = mockStatic(AdminDeviceService.class)) {
            users.when(() -> UsersDB.getUser("website-user")).thenReturn(mock(UserDetails.class));
            new PasskeyAuthSuccessHandler().onAuthenticationSuccess(request, response, authentication);
            assertEquals("/admin/logon/?error=passkey_access_denied", response.getRedirectedUrl());
            devices.verifyNoInteractions();
        }
    }
}
