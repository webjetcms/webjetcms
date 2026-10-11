package sk.iway.iwcm.admin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConstantsV9;

/** Resolves the documentation server shared by administration navigation and help links. */
public final class DocumentationTools {

    private DocumentationTools() {
    }

    /**
     * Returns the configured documentation origin, using the current request for a root-relative endpoint.
     *
     * @return HTTP(S) origin without a trailing slash, or the public documentation origin for invalid configuration
     */
    public static String getBaseUrl() {
        RequestBean request = SetCharacterEncodingFilter.getCurrentRequestBean();
        return resolveBaseUrl(Constants.getString("adminDocumentationSearchUrl"), request == null ? null : request.getBaseHref());
    }

    /**
     * Returns the documentation origin for legacy pages with an explicit servlet request.
     *
     * @param request current request, used only for root-relative endpoints
     * @return HTTP(S) documentation origin without a trailing slash
     */
    public static String getBaseUrl(HttpServletRequest request) {
        String endpoint = Constants.getString("adminDocumentationSearchUrl");
        String requestBaseUrl = isRootRelative(endpoint) && request != null ? Tools.getBaseHref(request) : null;
        return resolveBaseUrl(endpoint, requestBaseUrl);
    }

    static String resolveBaseUrl(String endpoint, String requestBaseUrl) {
        if (endpoint == null || endpoint.isBlank()) return ConstantsV9.DEFAULT_DOCUMENTATION_BASE_URL;
        try {
            URI uri = new URI(endpoint.trim());
            if (isRootRelative(endpoint) && requestBaseUrl != null) uri = new URI(requestBaseUrl).resolve(uri);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null
                    && uri.getPort() <= 65535) {
                return new URI(scheme.toLowerCase(Locale.ROOT), null, uri.getHost(), uri.getPort(), null, null, null).toASCIIString();
            }
        } catch (URISyntaxException ex) {
            // Invalid configuration keeps navigation available through the public documentation server.
        }
        return ConstantsV9.DEFAULT_DOCUMENTATION_BASE_URL;
    }

    private static boolean isRootRelative(String endpoint) {
        return endpoint != null && endpoint.trim().startsWith("/") && !endpoint.trim().startsWith("//");
    }

    /**
     * Maps administration languages to the available documentation translations.
     *
     * @param language administration language code
     * @return sk, cs or en; unsupported or missing languages use English
     */
    public static String normalizeLanguage(String language) {
        if ("sk".equals(language)) return "sk";
        if ("cz".equals(language) || "cs".equals(language)) return "cs";
        return "en";
    }
}
