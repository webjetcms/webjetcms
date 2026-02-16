package sk.iway.iwcm.system.jpaconverters;

import javax.persistence.AttributeConverter;

/**
 * Converts NULL database value to empty string. Used mainly because Oracle can't store enpty strings.
 */
public class NotNullStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        //do not care
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // NULL from DB is converted to empty string
        return (dbData == null) ? "" : dbData;
    }

}
