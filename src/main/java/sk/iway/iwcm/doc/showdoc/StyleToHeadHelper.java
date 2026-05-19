package sk.iway.iwcm.doc.showdoc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * Helper class for extracting &lt;style&gt; tags and &lt;link rel="stylesheet"&gt; tags
 * from HTML component output and collecting them for later insertion into the &lt;head&gt; section.
 *
 * This solves HTML validation issues where &lt;style&gt; and stylesheet &lt;link&gt; tags
 * appear in the body instead of the head section, and ensures correct CSS cascade order.
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
     * Pattern to match &lt;link&gt; tags that reference stylesheets.
     * Matches self-closing &lt;link ... /&gt; and non-self-closing &lt;link ...&gt; forms.
     * Requires rel="stylesheet" attribute to avoid moving other link types (favicon, prefetch, etc.).
     */
    private static final Pattern LINK_STYLESHEET_PATTERN = Pattern.compile(
         "<link[^>]*\\srel=[\"']stylesheet[\"'][^>]*/?\\s*>",
        Pattern.CASE_INSENSITIVE
     );

     /**
      * Extracts all &lt;style&gt; tags and &lt;link rel="stylesheet"&gt; tags from the given HTML code,
      * removes them from the content, and stores them in the request for later insertion into the head section.
      *
      * If showDocMoveStyleToHead config is disabled, returns the original content unchanged.
      *
      * @param htmlCode The HTML content to process
      * @param request The HTTP request to store collected styles
      * @return StringBuilder with style and link tags removed (or original if disabled)
      */
    public static StringBuilder extractAndCollectStyles(StringBuilder htmlCode, HttpServletRequest request) {
        if (Tools.isEmpty(htmlCode)) {
            return htmlCode;
         }

         // Check if feature is enabled
        if (Constants.getBoolean("showDocMoveStyleToHead") == false) {
            return htmlCode;
         }

         // Quick check - if no style tag or stylesheet link present, return early
        String htmlLower = htmlCode.toString().toLowerCase();
        if (htmlLower.indexOf("<style") == -1 && htmlLower.indexOf("rel=\"stylesheet\"") == -1 && htmlLower.indexOf("rel='stylesheet'") == -1) {
            return htmlCode;
         }

         // Get or create the set of collected styles
         @SuppressWarnings("unchecked")
        Set<String> collectedStyles = (Set<String>) request.getAttribute(COLLECTED_STYLES_KEY);
        if (collectedStyles == null) {
            collectedStyles = new LinkedHashSet<>();
            request.setAttribute(COLLECTED_STYLES_KEY, collectedStyles);
         }

         // Find all matches from both patterns and collect them
        List<MatchPosition> allMatches = new ArrayList<>();

        Matcher styleMatcher = STYLE_PATTERN.matcher(htmlCode);
        while (styleMatcher.find()) {
            allMatches.add(new MatchPosition(styleMatcher.start(), styleMatcher.end(), styleMatcher.group()));
         }

        Matcher linkMatcher = LINK_STYLESHEET_PATTERN.matcher(htmlCode);
        while (linkMatcher.find()) {
            allMatches.add(new MatchPosition(linkMatcher.start(), linkMatcher.end(), linkMatcher.group()));
         }

         // Sort by start position to preserve document order
        allMatches.sort((a, b) -> Integer.compare(a.start, b.start));

         // Build result with matches extracted
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        for (MatchPosition match : allMatches) {
             // Append text before this match
            result.append(htmlCode, lastEnd, match.start);

             // Add to collection (deduplication via Set, but preserve first-encountered order)
            collectedStyles.add(match.text);

            lastEnd = match.end;
         }

         // Append remaining text after last match
        if (lastEnd < htmlCode.length()) {
            result.append(htmlCode, lastEnd, htmlCode.length());
         }

        return result;
     }

     /**
      * Simple helper class to hold match position and text.
      */
    private static class MatchPosition {
        final int start;
        final int end;
        final String text;

        MatchPosition(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
         }
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
          //styles.append("\n<!-- WJ: Styles moved from body to head for HTML validation -->\n");
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

    /**
     * Insert style tags into the head section of HTML.
     *
     * @param html The HTML content
     * @param styles The style tags to insert
     * @return HTML with styles inserted before &lt;/head&gt;
     */
    public static String insertStylesIntoHead(String html, String styles) {
        if (Tools.isEmpty(styles)) {
            return html;
        }

        // Find </head> tag (case insensitive)
        int headEndIndex = html.indexOf("</head>");
        if (headEndIndex == -1) {
            headEndIndex = html.indexOf("</HEAD>");
        }

        if (headEndIndex == -1) {
            // No head section found, return unchanged
            Logger.debug(StyleToHeadHelper.class, "No </head> tag found, styles will remain in original position");
            return html;
        }

        // Insert styles before </head>
        StringBuilder result = new StringBuilder(html.length() + styles.length());
        result.append(html, 0, headEndIndex);
        //result.append("\n<!-- Styles moved from components -->\n");
        result.append(styles);
        result.append(html, headEndIndex, html.length());

        return result.toString();
    }
}
