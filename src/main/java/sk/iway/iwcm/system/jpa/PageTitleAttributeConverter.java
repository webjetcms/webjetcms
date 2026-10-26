package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import sk.iway.iwcm.Tools;

/** Keeps page and history titles safe on both database writes and reads. */
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
     * Unlike the general JPA converter, page titles cannot opt into executable HTML.
     *
     * @param value title or navigation label, possibly loaded through JDBC
     * @return sanitized HTML, preserving {@code null}
     */
    public static String sanitizeTitle(String value) {
        if (value == null) return null;
        return AllowSafeHtmlAttributeConverter.sanitize(Tools.convertToHtmlTags(value));
    }
}
