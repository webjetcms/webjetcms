package sk.iway.iwcm.system.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Retains safe HTML on both database writes and reads, including legacy rows.
 * This explicit protection is independent of the {@code jpaentity} HTML bypass.
 * JDBC callers must invoke {@link #sanitize(String)} themselves; JPA annotations
 * do not apply to native SQL. Use this converter for HTML fragments, not URLs.
 */
@Converter(autoApply = false)
public class AllowSafeHtmlAttributeConverterWithSave implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String value) {
        return sanitize(value);
    }

    @Override
    public String convertToEntityAttribute(String value) {
        return sanitize(value);
    }

    /**
     * Applies the shared safe HTML policy without decoding legacy title notation.
     *
     * @param value HTML fragment, possibly loaded or saved through JDBC
     * @return sanitized HTML, preserving {@code null}
     */
    public static String sanitize(String value) {
        if (value == null) return null;
        return AllowSafeHtmlAttributeConverter.sanitize(value);
    }
}
