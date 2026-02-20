package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Logger;

import java.io.IOException;

/**
 * Pomocná trieda so zdieľanou funkcionalitou pre OAuth2 handlery
 */
public class OAuth2LoginHelper {

    private static final String SESSION_ATTR_ADMIN_SECTION = "oauth2_is_admin_section";
    private static final String SESSION_ATTR_ERROR = "oauth2_logon_error";
    private static final String ADMIN_REDIRECT_URL = "/admin/logon/";
    private static final String USER_REDIRECT_URL = "/";

    /**
     * Zistí, či sa jedná o prihlásenie do administrácie
     * Na základe session atribútu nastaveného v admin/user logon stránke
     *
     * @param request HTTP request
     * @return true ak ide o admin prihlásenie
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
     * Zistí, či sa jedná o prihlásenie do administrácie a odstráni session atribút
     *
     * @param request HTTP request
     * @return true ak ide o admin prihlásenie
     */
    public static boolean isAdminLoginAndClear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Boolean isAdminSection = (Boolean) session.getAttribute(SESSION_ATTR_ADMIN_SECTION);
        boolean result = isAdminSection != null && isAdminSection;
        // Po použití odstráň atribút
        session.removeAttribute(SESSION_ATTR_ADMIN_SECTION);
        return result;
    }

    /**
     * Vráti URL pre presmerovanie pri chybe podľa typu prihlásenia
     *
     * @param isAdminLogin true ak ide o admin prihlásenie
     * @return URL pre presmerovanie
     */
    public static String getErrorRedirectUrl(boolean isAdminLogin) {
        return isAdminLogin ? ADMIN_REDIRECT_URL : USER_REDIRECT_URL;
    }

    /**
     * Vráti URL pre presmerovanie do admin zóny
     *
     * @return URL pre presmerovanie do admin zóny
     */
    public static String getAdminRedirectUrl() {
        return ADMIN_REDIRECT_URL;
    }

    /**
     * Vráti URL pre presmerovanie do user zóny
     *
     * @return URL pre presmerovanie do user zóny
     */
    public static String getUserRedirectUrl() {
        return USER_REDIRECT_URL;
    }

    /**
     * Nastaví chybový kód do session a vykoná redirect
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param errorCode kód chyby
     * @param redirectUrl URL pre presmerovanie
     * @throws IOException ak zlyhá redirect
     */
    public static void handleError(HttpServletRequest request, HttpServletResponse response, String errorCode, String redirectUrl) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_ATTR_ERROR, errorCode);
        Logger.debug(OAuth2LoginHelper.class, "OAuth2 error: " + errorCode + ", redirecting to: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Nastaví chybový kód do session a vykoná redirect podľa typu prihlásenia
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param errorCode kód chyby
     * @param isAdminLogin true ak ide o admin prihlásenie
     * @throws IOException ak zlyhá redirect
     */
    public static void handleError(HttpServletRequest request, HttpServletResponse response, String errorCode, boolean isAdminLogin) throws IOException {
        String redirectUrl = getErrorRedirectUrl(isAdminLogin);
        handleError(request, response, errorCode, redirectUrl);
    }
}
