package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;

import java.io.IOException;

/**
 * Dynamic OAuth2 Success Handler that decides whether to use admin or user handler
 * based on session attribute or referer URL
 */
public class OAuth2DynamicSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AdminSuccessHandler adminHandler = new OAuth2AdminSuccessHandler();
    private final OAuth2UserSuccessHandler userHandler = new OAuth2UserSuccessHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        SetCharacterEncodingFilter.registerDataContext(request);

        // Use helper to determine login type and clear session attribute
        boolean isAdminLogin = OAuth2LoginHelper.isAdminLoginAndClear(request);

        Logger.info(OAuth2DynamicSuccessHandler.class, "OAuth2 login detected - isAdminLogin: " + isAdminLogin);

        if (isAdminLogin) {
            adminHandler.onAuthenticationSuccess(request, response, authentication);
        } else {
            userHandler.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
