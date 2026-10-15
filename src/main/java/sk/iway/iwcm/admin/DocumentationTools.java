package sk.iway.iwcm.admin;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Resolves the documentation server shared by administration navigation and help links. */
public final class DocumentationTools {

    private DocumentationTools() {
    }

    /**
     * Returns the protocol, host and port from the full URL configured in adminDocumentationSearchUrl.
     *
     * @return configured server URL without the search endpoint path
     */
    public static String getBaseUrl() {
        String baseUrl = extractBaseUrl(Constants.getString("adminDocumentationSearchUrl"));
        return baseUrl;
    }

    /**
     * Removes the search endpoint path from the configured URL without validating the configuration.
     *
     * @param endpoint valid full HTTP(S) documentation search URL including its path
     * @return URL before the path, or an empty string if the endpoint is empty or the path is missing
     */
    private static String extractBaseUrl(String endpoint) {
        if (Tools.isEmpty(endpoint)) return "";
        int i = endpoint.indexOf("/", 9);
        if (i > 0) {
            return endpoint.substring(0, i);
        }
        return "";
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
