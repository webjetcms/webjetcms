package sk.iway.iwcm.components.customfields.rest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.components.enumerations.rest.EnumerationService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.editor.FieldType;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableFieldError;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Validates raw JSON custom fields without converting or rewriting their string values.
 */
public final class JsonEditorValidator {

    private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES, JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, JsonReadFeature.ALLOW_JAVA_COMMENTS)
        .build();
    private static final Pattern UNQUOTED_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$-]*");
    private static final String MESSAGE_PREFIX = "settings.custom-fields.jsoneditor.";

    private JsonEditorValidator() {}

    /**
     * Resolves JSON field names and required flags exclusively from server configuration.
     * @param entity entity providing field metadata and template context
     * @param context database configuration lookup context
     * @return field names mapped to their required flags
     */
    public static Map<String, Boolean> getRules(Object entity, CustomFieldsSearchDto context) {
        if (entity == null) return Map.of();
        if (entity instanceof DocDetails doc) {
            return DocEditorFields.withCustomFieldTextPrefixes(doc.getTempId(), () -> resolveRules(entity, context));
        }
        return resolveRules(entity, context);
    }

    private static Map<String, Boolean> resolveRules(Object entity, CustomFieldsSearchDto context) {
        Map<String, Boolean> rules = new LinkedHashMap<>();
        BeanWrapperImpl bean = new BeanWrapperImpl(entity);
        Map<Character, CustomFieldsEntity> configuredFields = null;
        EnumerationTypeBean enumerationType = null;
        boolean enumerationTypeLoaded = false;
        Prop typeProp = Prop.getInstance(Constants.getString("defaultLanguage"));
        for (char alphabet = 'A'; alphabet <= 'Z'; alphabet++) {
            String name = "field" + alphabet;
            if (bean.isReadableProperty(name) == false) continue;
            if (configuredFields == null) configuredFields = CustomFieldsService.getCustomFieldsMap(context);
            CustomFieldsEntity configuredField = configuredFields.get(alphabet);
            if (entity instanceof EnumerationDataBean && configuredField == null) continue;
            String labelKey = getLabelKey(entity.getClass(), name);
            String type = CustomFieldsService.getConfiguredFieldType(configuredField, labelKey, typeProp);
            if (FieldType.asFieldType(type) == FieldType.JSONEDITOR) {
                if (entity instanceof EnumerationDataBean) {
                    if (enumerationTypeLoaded == false) {
                        EnumerationTypeRepository repository = Tools.getSpringBean("enumerationTypeRepository", EnumerationTypeRepository.class);
                        if (repository == null) throw new IllegalStateException("Cannot resolve the enumeration custom-field context");
                        EnumerationTypeBean parent = repository.getNonHiddenByEnumId(context.getEntityId().intValue(), false);
                        if (parent != null) enumerationType = parent;
                        enumerationTypeLoaded = true;
                    }
                    if (Tools.isEmpty(EnumerationService.getStringFieldName(enumerationType, alphabet))) continue;
                }
                rules.put(name, configuredField != null && Tools.isTrue(configuredField.getRequired()));
            }
        }
        return rules;
    }

    private static String getLabelKey(Class<?> entityClass, String name) {
        Field field = ReflectionUtils.findField(entityClass, name);
        if (field == null) return null;
        DataTableColumn column = field.getAnnotation(DataTableColumn.class);
        if (column == null) return null;
        String labelKey = column.title();
        if (labelKey.startsWith("[[#{") && labelKey.endsWith("}]]")) {
            labelKey = labelKey.substring(4, labelKey.length() - 3);
        }
        return labelKey.endsWith(".field_" + Character.toLowerCase(name.charAt(5))) ? labelKey : null;
    }

    /**
     * Validates the supplied entity against resolved JSON field rules.
     * @param entity entity whose string values are checked
     * @param rules server-resolved field names and required flags
     * @param prop localized validation messages
     * @return native DataTable field errors, empty when all values are valid
     */
    public static List<DatatableFieldError> validate(Object entity, Map<String, Boolean> rules, Prop prop) {
        List<DatatableFieldError> errors = new ArrayList<>();
        if (rules.isEmpty()) return errors;
        BeanWrapperImpl bean = new BeanWrapperImpl(entity);
        for (Map.Entry<String, Boolean> rule : rules.entrySet()) {
            Object value = bean.getPropertyValue(rule.getKey());
            String error = validateValue(value == null ? null : value.toString(), rule.getValue(), prop);
            if (error != null) errors.add(new DatatableFieldError(rule.getKey(), error));
        }
        return errors;
    }

    /**
     * Checks one complete object, allowing single quotes, unquoted names and Java-style comments.
     * @param value original text, which is never changed
     * @param required whether an empty value is invalid
     * @param prop localized validation messages
     * @return localized error or null for a valid value
     */
    public static String validateValue(String value, boolean required, Prop prop) {
        if (value == null || value.trim().isEmpty()) {
            return required ? prop.getText("settings.custom-fields.required-err") : null;
        }
        try (JsonParser parser = JSON_FACTORY.createParser(value)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return prop.getText(MESSAGE_PREFIX + "object.js");
            int depth = 1;
            while (depth > 0) {
                JsonToken token = parser.nextToken();
                if (token == null) return prop.getText(MESSAGE_PREFIX + "invalid.js");
                if (token == JsonToken.FIELD_NAME) {
                    int offset = (int)parser.currentTokenLocation().getCharOffset();
                    char first = value.charAt(offset);
                    if (first != '\"' && first != '\'' && !UNQUOTED_NAME.matcher(parser.currentName()).matches()) {
                        return prop.getText(MESSAGE_PREFIX + "invalid.js");
                    }
                }
                if (token.isStructStart()) depth++;
                else if (token.isStructEnd()) depth--;
            }
            if (parser.nextToken() != null) return prop.getText(MESSAGE_PREFIX + "invalid.js");
            return null;
        } catch (JsonProcessingException ex) {
            JsonLocation location = ex.getLocation();
            String message = prop.getText(MESSAGE_PREFIX + "invalid.js");
            if (location != null && location.getLineNr() > 0 && location.getColumnNr() > 0) {
                message += " " + prop.getText(MESSAGE_PREFIX + "position.js", String.valueOf(location.getLineNr()), String.valueOf(location.getColumnNr()));
            }
            return message;
        } catch (IOException ex) {
            return prop.getText(MESSAGE_PREFIX + "invalid.js");
        }
    }

}
