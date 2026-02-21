package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Helper class with shared functionality for OAuth2 handlers
 */
public class OAuth2LoginHelper {

    private static final String SESSION_ATTR_ADMIN_SECTION = "oauth2_is_admin_section";
    private static final String SESSION_ATTR_ERROR = "oauth2_logon_error";
    private static final String ADMIN_REDIRECT_URL = "/admin/logon/";
    private static final String USER_REDIRECT_URL = "/";

    /**
     * Set info that login will be for admin section
     * @param request
     */
    public static void setAdminLogin(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_ATTR_ADMIN_SECTION, true);
    }

    /**
     * Determines if this is an administration login
     * Based on session attribute set in admin/user logon page
     *
     * @param request HTTP request
     * @return true if this is admin login
     */
    public static boolean isAdminLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Boolean isAdminSection = (Boolean) session.getAttribute(SESSION_ATTR_ADMIN_SECTION);
        return isAdminSection != null && isAdminSection;
    }

    /**
     * Determines if this is an administration login and removes session attribute
     *
     * @param request HTTP request
     * @return true if this is admin login
     */
    public static boolean isAdminLoginAndClear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Boolean isAdminSection = (Boolean) session.getAttribute(SESSION_ATTR_ADMIN_SECTION);
        boolean result = isAdminSection != null && isAdminSection;
        // Remove attribute after use
        session.removeAttribute(SESSION_ATTR_ADMIN_SECTION);
        return result;
    }

    /**
     * Returns redirect URL for error based on login type
     *
     * @param isAdminLogin true if this is admin login
     * @return redirect URL
     */
    public static String getErrorRedirectUrl(boolean isAdminLogin) {
        return isAdminLogin ? ADMIN_REDIRECT_URL : USER_REDIRECT_URL;
    }

    /**
     * Returns redirect URL for admin zone
     *
     * @return redirect URL for admin zone
     */
    public static String getAdminRedirectUrl() {
        return ADMIN_REDIRECT_URL;
    }

    /**
     * Returns redirect URL for user zone
     *
     * @return redirect URL for user zone
     */
    public static String getUserRedirectUrl() {
        return USER_REDIRECT_URL;
    }

    /**
     * Sets error code to session and performs redirect
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param errorCode error code
     * @param redirectUrl redirect URL
     * @throws IOException if redirect fails
     */
    public static void handleError(HttpServletRequest request, HttpServletResponse response, String errorCode, String redirectUrl) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_ATTR_ERROR, errorCode);
        Logger.debug(OAuth2LoginHelper.class, "OAuth2 error: " + errorCode + ", redirecting to: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Sets error code to session and performs redirect based on login type
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param errorCode error code
     * @param isAdminLogin true if this is admin login
     * @throws IOException if redirect fails
     */
    public static void handleError(HttpServletRequest request, HttpServletResponse response, String errorCode, boolean isAdminLogin) throws IOException {
        String redirectUrl = getErrorRedirectUrl(isAdminLogin);
        handleError(request, response, errorCode, redirectUrl);
    }

    /**
     * Gets logon URLs for configured OAuth2 clients to display on login page
     * @param request HTTP request
     * @return map of client names to authorization URLs
     */
    public static Map<String, String> getLogonUrls(boolean isAdmin, HttpServletRequest request) {
        if (Tools.isNotEmpty(Constants.getString("oauth2_clients"))) {
            // Označ že ide o user sekciu (nie admin)
            HttpSession session = request.getSession();
            if (isAdmin) {
                session.setAttribute("oauth2_is_admin_section", true);
            } else {
                //save session redirect
                session.setAttribute("afterLogonRedirect", PathFilter.getOrigPath(request));
                session.removeAttribute("oauth2_is_admin_section");
            }

            Map<String, String> oauth2AuthenticationUrls = new HashMap<>();
            String authorizationRequestBaseUri = "/oauth2/authorization";

            try {
                ClientRegistrationRepository clientRegistrationRepository = Tools.getSpringBean("clientRegistrationRepository", ClientRegistrationRepository.class);
                if (clientRegistrationRepository != null) {
                    Iterable<ClientRegistration> clientRegistrations = null;
                    ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
                    if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
                        @SuppressWarnings("unchecked")
                        Iterable<ClientRegistration> clientRegistration = (Iterable<ClientRegistration>) clientRegistrationRepository;
                        clientRegistrations = clientRegistration;
                    }
                    if (clientRegistrations != null) {
                        for (ClientRegistration registration : clientRegistrations) {
                            oauth2AuthenticationUrls.put(registration.getClientName(),
                                authorizationRequestBaseUri + "/" + registration.getRegistrationId());
                        }
                        Logger.debug(OAuth2LoginHelper.class, "OAuth2 URLs set for user logon: " + oauth2AuthenticationUrls);
                        return oauth2AuthenticationUrls;
                    }
                }
            } catch (Exception e) {
                Logger.error(OAuth2LoginHelper.class, "Error getting OAuth2 client registrations", e);
            }
        }
        return null;
    }
}
