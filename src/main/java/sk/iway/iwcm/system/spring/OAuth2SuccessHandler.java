package sk.iway.iwcm.system.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import java.io.IOException;

public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            Object principal = authentication.getPrincipal();
            String email = null;
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                email = oauth2User.getAttribute("email");
            }
            if (email == null) {
                Logger.error(OAuth2SuccessHandler.class, "OAuth2 email not found");
                response.sendRedirect("/admin/logon.jsp?error=oauth2_email_not_found");
                return;
            }
            UserDetails userDetails = UsersDB.getUserByEmail(email, 1);
            if (userDetails == null) {
                // Vytvor nového používateľa
                userDetails = new UserDetails();
                userDetails.setEmail(email);
                String givenName = null;
                String familyName = null;
                if (principal instanceof OAuth2User) {
                    OAuth2User oauth2User = (OAuth2User) principal;
                    givenName = oauth2User.getAttribute("given_name");
                    familyName = oauth2User.getAttribute("family_name");
                }
                if (givenName != null) userDetails.setFirstName(givenName);
                if (familyName != null) userDetails.setLastName(familyName);
                // Nastav login ako email pred zavináčom
                String login = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                userDetails.setLogin(login);
                userDetails.setAuthorized(true);
                userDetails.setAdmin(true);
                boolean isUserSaved = UsersDB.saveUser(userDetails);
                if (!isUserSaved) {
                    Logger.error(OAuth2SuccessHandler.class, "Failed to create user for email: " + email);
                    response.sendRedirect("/admin/logon.jsp?error=user_create_failed");
                    return;
                }
                userDetails = UsersDB.getUserByEmail(email, 1);
                Logger.info(OAuth2SuccessHandler.class, "Created new user for email: " + email);
            }
            Identity identity = new Identity(userDetails);
            identity.setValid(true);
            HttpSession session = request.getSession();
            LogonTools.setUserToSession(session, identity);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            response.sendRedirect("/admin/");
        } catch (Exception ex) {
            Logger.error(OAuth2SuccessHandler.class, ex);
            response.sendRedirect("/admin/logon.jsp?error=oauth2_exception");
        }
    }
}
