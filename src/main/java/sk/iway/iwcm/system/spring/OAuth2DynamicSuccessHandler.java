package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import sk.iway.iwcm.Logger;

import java.io.IOException;

/**
 * Dynamický OAuth2 Success Handler, ktorý rozhoduje či použiť admin alebo user handler
 * na základe session atribútu alebo referer URL
 */
public class OAuth2DynamicSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2SuccessHandler adminHandler = new OAuth2SuccessHandler();
    private final OAuth2UserSuccessHandler userHandler = new OAuth2UserSuccessHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        boolean isAdminLogin = false;

        // Kontrola explicitného OAuth2 atribútu nastaveného v admin/user logon stránke
        if (session != null) {
            Boolean isAdminSection = (Boolean) session.getAttribute("oauth2_is_admin_section");
            isAdminLogin = (isAdminSection != null && isAdminSection);
            // Po použití odstráň atribút
            session.removeAttribute("oauth2_is_admin_section");
        }

        Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);

        if (isAdminLogin) {
            adminHandler.onAuthenticationSuccess(request, response, authentication);
        } else {
            userHandler.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
