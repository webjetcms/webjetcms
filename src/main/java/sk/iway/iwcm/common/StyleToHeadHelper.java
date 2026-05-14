package sk.iway.iwcm.common;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;

/**
 * Helper class for extracting &lt;style&gt; tags from HTML component output
 * and collecting them for later insertion into the &lt;head&gt; section.
 *
 * This solves HTML validation issues where &lt;style&gt; tags appear in the body
 * instead of the head section.
 *
 * Usage:
 * 1. Call extractAndCollectStyles() during component processing in WriteTag
 * 2. Call getCollectedStyles() in StyleToHeadFilter to insert into head
 *
 * @author WebJET CMS
 */
public class StyleToHeadHelper {

    private StyleToHeadHelper() {
        // Utility class
    }

    /**
     * Request attribute key for storing collected styles
     */
    public static final String COLLECTED_STYLES_KEY = "sk.iway.iwcm.styleToHead.collectedStyles";

    /**
     * Pattern to match &lt;style&gt; tags including attributes and content.
     * Uses DOTALL flag to match multiline CSS content.
     * Captures the entire style tag including opening and closing tags.
     */
    private static final Pattern STYLE_PATTERN = Pattern.compile(
        "<style[^>]*>.*?</style>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * Extracts all &lt;style&gt; tags from the given HTML code, removes them from the content,
     * and stores them in the request for later insertion into the head section.
     *
     * If moveStyleToHead config is disabled, returns the original content unchanged.
     *
     * @param htmlCode The HTML content to process
     * @param request The HTTP request to store collected styles
     * @return StringBuilder with style tags removed (or original if disabled)
     */
    public static StringBuilder extractAndCollectStyles(StringBuilder htmlCode, HttpServletRequest request) {
        if (htmlCode == null || htmlCode.length() == 0) {
            return htmlCode;
        }

        // Check if feature is enabled
        if (Constants.getBoolean("moveStyleToHead") == false) {
            return htmlCode;
        }

        // Quick check - if no style tag present, return early
        String htmlLower = htmlCode.toString().toLowerCase();
        if (htmlLower.indexOf("<style") == -1) {
            return htmlCode;
        }

        // Get or create the set of collected styles
        @SuppressWarnings("unchecked")
        Set<String> collectedStyles = (Set<String>) request.getAttribute(COLLECTED_STYLES_KEY);
        if (collectedStyles == null) {
            collectedStyles = new LinkedHashSet<>();
            request.setAttribute(COLLECTED_STYLES_KEY, collectedStyles);
        }

        // Find and extract all style tags
        Matcher matcher = STYLE_PATTERN.matcher(htmlCode);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before this style tag
            result.append(htmlCode, lastEnd, matcher.start());

            // Get the style tag content and add to collection (deduplication via Set)
            String styleTag = matcher.group();
            collectedStyles.add(styleTag);

            lastEnd = matcher.end();
        }

        // Append remaining text after last style tag
        if (lastEnd < htmlCode.length()) {
            result.append(htmlCode, lastEnd, htmlCode.length());
        }

        return result;
    }

    /**
     * Checks if there are any collected styles in the request.
     *
     * @param request The HTTP request
     * @return true if styles have been collected, false otherwise
     */
    public static boolean hasCollectedStyles(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Set<String> collectedStyles = (Set<String>) request.getAttribute(COLLECTED_STYLES_KEY);
        return collectedStyles != null && collectedStyles.isEmpty() == false;
    }

    /**
     * Returns all collected styles as a single string ready for insertion into the head section.
     * Styles are returned in the order they were first encountered (LinkedHashSet preserves order).
     *
     * @param request The HTTP request
     * @return String containing all style tags, or empty string if none collected
     */
    public static String getCollectedStyles(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Set<String> collectedStyles = (Set<String>) request.getAttribute(COLLECTED_STYLES_KEY);

        if (collectedStyles == null || collectedStyles.isEmpty()) {
            return "";
        }

        StringBuilder styles = new StringBuilder();
        styles.append("\n<!-- WJ: Styles moved from body to head for HTML validation -->\n");
        for (String style : collectedStyles) {
            styles.append(style).append("\n");
        }

        return styles.toString();
    }

    /**
     * Clears collected styles from the request.
     * Called after styles have been inserted into the head section.
     *
     * @param request The HTTP request
     */
    public static void clearCollectedStyles(HttpServletRequest request) {
        request.removeAttribute(COLLECTED_STYLES_KEY);
    }
}
