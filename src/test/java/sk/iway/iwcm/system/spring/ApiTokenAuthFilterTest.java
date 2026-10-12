package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Verifies that API token authentication remains separate from interactive post-login actions. */
class ApiTokenAuthFilterTest {
    /** A successful API token authenticates only the current request and skips browser-login callbacks. */
    @Test
    void skipsPostLoginActionsForApiTokenAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var session = request.getSession();
        request.addHeader("X-Api-Token", "api-user:api-secret");
        FilterChain chain = mock(FilterChain.class);
        try (var constants = mockStatic(Constants.class); var users = mockStatic(UsersDB.class);
             var logon = mockStatic(LogonTools.class); var properties = mockStatic(Prop.class);
             var dataContext = mockStatic(SetCharacterEncodingFilter.class); var logger = mockStatic(Logger.class)) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            constants.when(() -> Constants.getString("springSecurityAllowedAuths")).thenReturn("api-token");
            constants.when(() -> Constants.getString("logonTokenHeaderName")).thenReturn("X-Api-Token");
            users.when(() -> UsersDB.getUser("api-user")).thenReturn(new UserDetails());

            new ApiTokenAuthFilter().doFilter(request, response, chain);

            logon.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
            logon.verify(() -> LogonTools.afterSuccessLogon(request, response), never());
            verify(chain).doFilter(request, response);
            assertEquals("1", request.getAttribute("csrfDisabled"));
            assertThrows(IllegalStateException.class, () -> session.getAttribute(Constants.USER_KEY));
        }
    }
}
