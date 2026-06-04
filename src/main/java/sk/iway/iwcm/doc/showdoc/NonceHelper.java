package sk.iway.iwcm.doc.showdoc;

import sk.iway.iwcm.Tools;

/**
 * Helper class for Content-Security-Policy (CSP) nonce functionality.
 * Handles nonce injection into HTML tags, inline style/event handler migration,
 * and CSP configuration parsing.
 */
public final class NonceHelper {

    private NonceHelper() {
        // Utility class - no instantiation
    }


    /**
     * Injects CSP nonce into <script>, <style>, and <link rel="stylesheet"> tags in a single pass.
     * Optimized for memory efficiency: uses single StringBuilder, processes all tag types in one pass.
     *
     * @param htmlContent The HTML content to process
     * @param nonce The CSP nonce value
     * @param injectIntoScripts Whether to inject nonce into <script> tags (false if script-src allows unsafe-inline)
     * @param injectIntoStyles Whether to inject nonce into <style> and <link> tags (false if style-src allows unsafe-inline)
     * @return HTML content with nonce injected into eligible tags
     */
    public static String injectCspNonceIntoTags(String htmlContent, String nonce, boolean injectIntoScripts, boolean injectIntoStyles) {
        if (Tools.isEmpty(htmlContent) || Tools.isEmpty(nonce)) {
            return htmlContent;
        }
        // Single regex pattern to match all three tag types (script, style, link)
        // Uses alternation to match any of: <script..., <style..., <link...rel="stylesheet"...>
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(<script[^>]*?>)|(<style[^>]*?>)|(<link\\b[^>]*?rel\\s*=\\s*(?:\"stylesheet\"|'stylesheet'|stylesheet)[^>]*?>)", //NOSONAR
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(htmlContent);
        // Use StringBuilder (no synchronization overhead) with initial capacity
        StringBuilder sb = new StringBuilder(htmlContent.length());
        // Pre-compile patterns for each tag type (includes closing '>' to handle plain tags like <style>)
        java.util.regex.Pattern scriptPattern = java.util.regex.Pattern.compile("(<script)(\\s+[^>]*)?>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern stylePattern = java.util.regex.Pattern.compile("(<style)(\\s+[^>]*)?>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile("(<link)(\\s+[^>]*)?>", java.util.regex.Pattern.CASE_INSENSITIVE);
        int lastEnd = 0;
        while (matcher.find()) {
            // Append text before this match
            sb.append(htmlContent, lastEnd, matcher.start());
            String tagContent = matcher.group(0);
            // Determine which tag type and inject nonce (skip if corresponding CSP directive allows unsafe-inline)
            String processedTag = processTagForNonce(tagContent, nonce, scriptPattern, stylePattern, linkPattern, injectIntoScripts, injectIntoStyles);
            sb.append(processedTag);
            lastEnd = matcher.end();
        }
        // Append remaining text after last match
        sb.append(htmlContent, lastEnd, htmlContent.length());
        return sb.toString();
    }

    /**
     * Processes a single tag (script, style, or link) to inject nonce if not already present.
     * Skips injection for tag types whose corresponding CSP directive allows unsafe-inline.
     *
     * @param tagContent The matched tag string
     * @param nonce The CSP nonce value
     * @param scriptPattern Pre-compiled pattern for script tags
     * @param stylePattern Pre-compiled pattern for style tags
     * @param linkPattern Pre-compiled pattern for link tags
     * @param injectIntoScripts Whether to inject nonce into script tags
     * @param injectIntoStyles Whether to inject nonce into style/link tags
     * @return Processed tag string
     */
    protected static String processTagForNonce(String tagContent, String nonce, java.util.regex.Pattern scriptPattern,
                                      java.util.regex.Pattern stylePattern, java.util.regex.Pattern linkPattern,
                                      boolean injectIntoScripts, boolean injectIntoStyles) {
        // Check if nonce attribute is already present (lookbehind ensures nonce is preceded by whitespace or letter, not - or ?)
        if (java.util.regex.Pattern.compile("(?<=[\\sa-z])nonce\\s*=").matcher(tagContent).find()) {
            return tagContent;
        }
        // Extract tag name (handles both <script> and <script type="...">)
        // Find the end of the tag name (space or '>')
        int gtIndex = tagContent.indexOf('>');
        int spaceIndex = tagContent.indexOf(' ');
        int nameEnd;
        if (spaceIndex < 0) {
            nameEnd = gtIndex;
        } else if (gtIndex < 0) {
            nameEnd = spaceIndex;
        } else {
            nameEnd = Math.min(spaceIndex, gtIndex);
        }
        if (nameEnd < 0) {
            return tagContent;
        }
        String tagName = tagContent.substring(0, nameEnd).toLowerCase();
        // Skip nonce injection for tag types whose CSP directive allows unsafe-inline
        if (tagName.equals("<script") && !injectIntoScripts) {
            return tagContent;
        }
        if ((tagName.equals("<style") || tagName.equals("<link")) && !injectIntoStyles) {
            return tagContent;
        }
        java.util.regex.Pattern tagSpecificPattern;
        switch (tagName) {
            case "<script":
                tagSpecificPattern = scriptPattern;
                break;
            case "<style":
                tagSpecificPattern = stylePattern;
                break;
            case "<link":
                tagSpecificPattern = linkPattern;
                break;
            default:
                return tagContent;
        }
        // Inject nonce attribute after existing attributes (or after tag name)
        java.util.regex.Matcher tagMatcher = tagSpecificPattern.matcher(tagContent);
        String injectPoint = tagContent;
        if (tagMatcher.find()) {
            String attributes = tagMatcher.group(2);
            //
            attributes = attributes.replaceAll("\\s*/\\s*$", "");
            if (Tools.isNotEmpty(attributes)) {
                injectPoint = tagName + attributes + " nonce=\"" + nonce + "\"";
            } else {
                injectPoint = tagName + " nonce=\"" + nonce + "\"";
            }
        }
        return injectPoint + ">";
    }


    /**
     * Processes inline styles by replacing them with data attributes and injecting CSS rules with nonce.
     * For elements with inline style="...", replaces with data-inline-style="counter" and generates
     * CSS rules like [data-inline-style="1"] { property: value !important; }.
     *
     * @param htmlContent The HTML content
     * @param nonce The CSP nonce
     * @return Processed HTML with inline styles replaced and CSS injected
     */
    public static String processInlineStyles(String htmlContent, String nonce) {
        if (Tools.isEmpty(htmlContent) || Tools.isEmpty(nonce)) {
            return htmlContent;
        }

        // Find all elements with inline style attributes (handles both double and single quotes)
        java.util.regex.Pattern stylePattern = java.util.regex.Pattern.compile(
            "(\\s+style\\s*=\\s*)(\"([^\"]*)\")|(\\s+style\\s*=\\s*)('([^']*)')",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = stylePattern.matcher(htmlContent);

        StringBuilder cssRules = new StringBuilder();
        int styleCounter = 0;
        StringBuilder processedHtml = new StringBuilder(htmlContent.length());
        int lastEnd = 0;

        while (matcher.find()) {
            styleCounter++;
            String quotePrefix;
            String styleValue;

            // Determine which quote style was used (group 1 = double-quoted prefix, group 4 = single-quoted prefix)
            if (matcher.group(1) != null) {
                quotePrefix = matcher.group(1);
                styleValue = matcher.group(3); // double-quoted value
            } else {
                quotePrefix = matcher.group(4);
                styleValue = matcher.group(6); // single-quoted value
            }

            String dataAttr = quotePrefix.replaceAll("(?i)style", "data-inline-style") + "\"nonce" + styleCounter + "\"";

            // Append text before this match
            processedHtml.append(htmlContent, lastEnd, matcher.start());
            processedHtml.append(dataAttr);

            // Generate CSS rule with !important for each property
            cssRules.append("       [data-inline-style=\"nonce").append(styleCounter).append("\"] {");

            // Parse CSS properties and add !important to each
            String[] properties = styleValue.split(";");
            for (String prop : properties) {
                String trimmed = prop.trim();
                if (Tools.isNotEmpty(trimmed) && trimmed.contains(":")) {
                    // Remove trailing semicolon if present
                    String propPart = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                    // Escape CSS special characters to prevent breaking out of the rule block
                    propPart = propPart.replace("}", "\\}").replace("{", "\\{");
                    cssRules.append(propPart).append(" !important;");
                }
            }
            cssRules.append("}\n");

            lastEnd = matcher.end();
        }

        // Append remaining text
        processedHtml.append(htmlContent, lastEnd, htmlContent.length());

        // If we found inline styles, inject CSS with nonce
        if (styleCounter > 0) {
            String styleInjection = "\n<style nonce=\"" + nonce + "\">\n" + cssRules.toString() + "</style>\n";
            // Find the closing </body> tag and insert before it, or append at end
            int bodyCloseIndex = processedHtml.lastIndexOf("</body>");
            if (bodyCloseIndex > 0) {
                processedHtml.insert(bodyCloseIndex, styleInjection);
            } else {
                processedHtml.append(styleInjection);
            }
        }

        return processedHtml.toString();
    }

    /**
     * Processes inline event handlers by replacing them with data attributes and injecting JavaScript with nonce.
     * For elements with inline event handlers (onclick, onmouseover, etc.), replaces with
     * data-inline-onclick="counter" and generates JavaScript code to restore the handlers.
     *
     * @param htmlContent The HTML content
     * @param nonce The CSP nonce
     * @return Processed HTML with inline event handlers replaced and JavaScript injected
     */
    public static String processInlineEventHandlers(String htmlContent, String nonce) {
        if (Tools.isEmpty(htmlContent) || Tools.isEmpty(nonce)) {
            return htmlContent;
        }

        // Find all inline event handlers (onclick, onmouseover, onmouseout, onfocus, onblur, etc.)
        // Handles both double and single quotes
        java.util.regex.Pattern eventPattern = java.util.regex.Pattern.compile(
            "(\\s+(onclick|onmouseover|onmouseout|onfocus|onblur|onkeydown|onkeyup|onsubmit|onchange|oninput|ondblclick|oncontextmenu|ondrag|ondrop)\\s*=\\s*)(\"([^\"]*)\")|(\\s+(onclick|onmouseover|onmouseout|onfocus|onblur|onkeydown|onkeyup|onsubmit|onchange|oninput|ondblclick|oncontextmenu|ondrop)\\s*=\\s*)('([^']*)')", //NOSONAR
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = eventPattern.matcher(htmlContent);

        StringBuilder jsCode = new StringBuilder();
        java.util.Map<String, Integer> eventCounters = new java.util.HashMap<>();
        StringBuilder processedHtml = new StringBuilder(htmlContent.length());
        int lastEnd = 0;

        while (matcher.find()) {
            String quotePrefix;
            String eventType;
            String handlerValue;

            // Determine which quote style was used
            if (matcher.group(1) != null) {
                quotePrefix = matcher.group(1);
                eventType = matcher.group(2).toLowerCase();
                handlerValue = matcher.group(4); // double-quoted
            } else {
                quotePrefix = matcher.group(5);
                eventType = matcher.group(6).toLowerCase();
                handlerValue = matcher.group(8); // single-quoted
            }

            //unfilter values
            handlerValue = handlerValue.replace("&#39;", "'");
            handlerValue = handlerValue.replace("&quot;", "\"");

            // Get or create counter for this event type
            int counter = eventCounters.getOrDefault(eventType, 0) + 1;
            eventCounters.put(eventType, counter);

            String dataAttr = quotePrefix.replaceAll("(?i)" + eventType, "data-inline-" + eventType) + "\"nonce" + counter + "\"";

            // Append text before this match
            processedHtml.append(htmlContent, lastEnd, matcher.start());
            processedHtml.append(dataAttr);

            // Generate JavaScript to restore the event handler
            jsCode.append("       document.querySelectorAll('[data-inline-").append(eventType).append("=\"nonce").append(counter).append("\"]').forEach(function(el) {");
            jsCode.append("el.").append(eventType).append(" = function(event) {").append(handlerValue).append(";};");
            jsCode.append("});\n");

            lastEnd = matcher.end();
        }

        // Append remaining text
        processedHtml.append(htmlContent, lastEnd, htmlContent.length());

        // If we found inline event handlers, inject JavaScript with nonce
        if (!eventCounters.isEmpty()) {
            String scriptInjection = "\n<script nonce=\"" + nonce + "\">\n" + jsCode.toString() + "</script>\n";
            // Find the closing </body> tag and insert before it, or append at end
            int bodyCloseIndex = processedHtml.lastIndexOf("</body>");
            if (bodyCloseIndex > 0) {
                processedHtml.insert(bodyCloseIndex, scriptInjection);
            } else {
                processedHtml.append(scriptInjection);
            }
        }

        return processedHtml.toString();
    }

    /**
     * Checks if a specific CSP directive allows 'unsafe-inline'.
     * Parses using indexOf to find the directive start and end (next ';' or end of string),
     * then checks if 'unsafe-inline' exists within that range.
     *
     * @param cspValue The full CSP configuration string
     * @param directiveName The directive name to check (e.g., "script-src", "style-src")
     * @return true if the directive allows 'unsafe-inline'
     */
    public static boolean isDirectiveAllowsUnsafeInline(String cspValue, String directiveName) {
        // Find the directive by looking for the name followed by whitespace
        int directiveIndex = cspValue.indexOf(directiveName);
        if (directiveIndex < 0) {
            return false;
        }
        // Move past the directive name to the value part
        int valueStart = directiveIndex + directiveName.length();
        // Skip whitespace after directive name
        while (valueStart < cspValue.length() && Character.isWhitespace(cspValue.charAt(valueStart))) {
            valueStart++;
        }
        // Find the end of this directive (next ';' or end of string)
        int directiveEnd = cspValue.indexOf(';', valueStart);
        if (directiveEnd < 0) {
            directiveEnd = cspValue.length();
        }
        // Use fast check instead tokenizing
        // Check if 'unsafe-inline' appears within this directive's value
        String directiveValue = cspValue.substring(valueStart, directiveEnd);
        if (directiveValue.contains("{nonce}")) return false;
        return directiveValue.contains("unsafe-inline");
    }
}
