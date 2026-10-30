package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import sk.iway.iwcm.Tools;

/**
 * Keeps page and history titles safe on both database writes and reads.
 * USES ALSO Tools.convertToHtmlTags(value) to convert legacy *|...|* to HTML
 * PageTitleAttributeConverter
 */
@Converter(autoApply = false)
public class PageTitleAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String value) {
        return sanitizeTitle(value);
    }

    @Override
    public String convertToEntityAttribute(String value) {
        return sanitizeTitle(value);
    }

    /**
     * Retains safe title formatting after decoding the legacy WebJET HTML notation.
     * Page titles cannot opt into executable HTML through the {@code jpaentity} bypass.
     *
     * @param value title or navigation label, possibly loaded through JDBC
     * @return sanitized HTML, preserving {@code null}
     */
    public static String sanitizeTitle(String value) {
        if (value == null) return null;
        String safeHtml = AllowSafeHtmlAttributeConverterWithSave.sanitize(Tools.convertToHtmlTags(value));
        // Entity decoding can reveal legacy markers; keep them as text during later rendering.
        return encodeTextSlashes(safeHtml).replace("*|", "&#42;|").replace("|*", "|&#42;");
    }

    /**
     * Encodes path separators in title text, leaving sanitized HTML tags and attribute URLs intact.
     * This scans only the sanitizer's serialized output, where literal text angle brackets are escaped.
     *
     * @param safeHtml sanitized title HTML
     * @return HTML with text slashes encoded for the legacy full-path representation
     */
    private static String encodeTextSlashes(String safeHtml) {
        if (safeHtml.indexOf('/') == -1) return safeHtml;
        StringBuilder result = new StringBuilder(safeHtml.length());
        boolean inTag = false;
        char quote = 0;
        for (int i = 0; i < safeHtml.length(); i++) {
            char character = safeHtml.charAt(i);
            if (character == '<' && !inTag) inTag = true;
            if (inTag) {
                if (quote != 0) {
                    if (character == quote) quote = 0;
                } else if (character == '"' || character == '\'') {
                    quote = character;
                } else if (character == '>') {
                    inTag = false;
                }
            } else if (character == '/') {
                // Do not produce &lt;&#47;: Tools.convertToHtmlTags treats it as legacy HTML markup.
                if (i >= 4 && safeHtml.startsWith("&lt;", i - 4)) {
                    result.setLength(result.length() - 4);
                    result.append("&#60;");
                }
                result.append("&#47;");
                continue;
            }
            result.append(character);
        }
        return result.toString();
    }
}
