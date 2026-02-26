package sk.iway.iwcm.system.spring.passkey;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Success handler for WebAuthn/PassKey authentication.
 * After successful passkey verification, establishes the WebJET session (Identity)
 * and Spring Security context, then redirects to admin.
 *
 * Follows the same pattern as OAuth2AdminSuccessHandler:
 * 1. Gets username from the WebAuthn authentication principal
 * 2. Finds the user in the WebJET database
 * 3. Creates Identity and sets it to the session
 * 4. Creates Spring Security authentication token
 * 5. Redirects to /admin/v9/
 */
public class PasskeyAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public PasskeyAuthSuccessHandler() {
        super("/admin/v9/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            String username = authentication.getName();
            Logger.info(PasskeyAuthSuccessHandler.class, "PassKey authentication success for user: " + username);

            // Find user in WebJET CMS database
            UserDetails userDetails = UsersDB.getUser(username);
            if (userDetails == null) {
                Logger.error(PasskeyAuthSuccessHandler.class, "User not found: " + username);
                response.sendRedirect("/admin/logon/?error=passkey_user_not_found");
                return;
            }

            if (userDetails.isAdmin() == false) {
                Logger.warn(PasskeyAuthSuccessHandler.class, "User " + username + " does not have admin rights");
                response.sendRedirect("/admin/logon/?error=passkey_access_denied");
                return;
            }

            // Establish WebJET CMS session (Identity) - same pattern as OAuth2AdminSuccessHandler
            Identity identity = new Identity(userDetails);
            identity.setValid(true);
            HttpSession session = request.getSession();
            LogonTools.setUserToSession(session, identity);

            // Establish Spring Security context
            Authentication springAuth = WebjetAuthentificationProvider.authenticate(identity);
            SecurityContextHolder.getContext().setAuthentication(springAuth);

            // Update request context for correct logging
            SetCharacterEncodingFilter.registerDataContext(request);
            Adminlog.add(Adminlog.TYPE_USER_LOGON, "PassKey - user (ADMIN) successfully logged: name=" + userDetails.getLogin(), -1, -1);

            // Check for saved redirect URL
            String afterLogonRedirect = (String) session.getAttribute("adminAfterLogonRedirect");
            if (afterLogonRedirect != null) {
                session.removeAttribute("adminAfterLogonRedirect");
                response.sendRedirect(afterLogonRedirect);
            } else {
                response.sendRedirect("/admin/v9/");
            }
        } catch (Exception ex) {
            Logger.error(PasskeyAuthSuccessHandler.class, "PassKey onAuthenticationSuccess error: " + ex.getMessage(), ex);
            response.sendRedirect("/admin/logon/?error=passkey_exception");
        }
    }
}
