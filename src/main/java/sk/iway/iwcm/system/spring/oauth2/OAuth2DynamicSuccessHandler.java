package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import sk.iway.iwcm.Logger;

import java.io.IOException;

/**
 * Dynamický OAuth2 Success Handler, ktorý rozhoduje či použiť admin alebo user handler
 * na základe session atribútu alebo referer URL
 */
public class OAuth2DynamicSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AdminSuccessHandler adminHandler = new OAuth2AdminSuccessHandler();
    private final OAuth2UserSuccessHandler userHandler = new OAuth2UserSuccessHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // Použije helper na zistenie typu prihlásenia a vyčistí session atribút
        boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);

        Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);

        if (isAdminLogin) {
            adminHandler.onAuthenticationSuccess(request, response, authentication);
        } else {
            userHandler.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
