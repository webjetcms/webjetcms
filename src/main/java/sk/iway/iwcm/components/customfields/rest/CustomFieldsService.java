package sk.iway.iwcm.components.customfields.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import jakarta.persistence.Entity;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsSearchDto;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class CustomFieldsService {

    private static final String FIELD_TYPE_KET_PREFIX = "settings.custom-fields.type.";

    private static final String CLASS_FIELD_MAP_KEY = "CustomFieldsService_classFieldsMap";
    public static final Map<String, String> BONUS_PARAMS = Map.of(
        "sk.iway.iwcm.doc.DocDetails", "tempId"
    );

    /* PUBLIC STATIC METHODS */

    /**
     * Retrieves custom fields as a map indexed by their character alphabet key.
     *
     * <p>Combines global fields (no entityId), specific entity fields, and bonus context fields
     * with appropriate priority handling. Specific fields override global fields, and bonus fields
     * have the highest priority.</p>
     *
     * @param main the search context containing className and entityId
     * @return map of custom fields indexed by their single-character alphabet key, or empty map if invalid params
     * @see #getCustomFields(CustomFieldsSearchDto)
     */
    public static Map<Character, CustomFieldsEntity> getCustomFieldsMap(CustomFieldsSearchDto main) {
        List<CustomFieldsEntity> customFields = getCustomFields(main);
        return customFields.stream()
            .collect(Collectors.toMap(
                CustomFieldsEntity::getCharacterAlphabet,
                customField -> customField
            ));
    }

    /**
     * Retrieves custom fields for the given search context.
     *
     * <p>Fetches global fields (no entityId), specific entity fields, and bonus context fields
     * when available. Specific fields override global fields, and bonus fields have the highest priority.</p>
     *
     * @param main the search context containing className and entityId
     * @return list of merged custom fields with priority handling, or empty list if invalid params
     * @see #getCustomFields(CustomFieldsSearchDto, CustomFieldsSearchDto)
     */
    public static List<CustomFieldsEntity> getCustomFields(CustomFieldsSearchDto main) {
        if(main == null || main.isValid() == false) {
            Logger.error(CustomFieldsService.class, "Bad params for getRequiredFieldsAlphabets. Returning empty list.");
            return List.of();
        }

        return getCustomFields(main, tryGetBonus(main));
    }

    /**
     * Retrieves custom fields for the given main and bonus search contexts.
     *
     * <p>Combines three field sources in priority order (lowest to highest):
     * <ol>
     * <li>Global fields - no entityId, apply to all entities of a class</li>
     * <li>Specific fields - have matching entityId, override global fields</li>
     * <li>Bonus fields - from bonus class context, have highest priority</li>
     * </ol>
     * </p>
     *
     * @param main primary search context with className and entityId
     * @param bonus optional secondary context for bonus class fields; if null, only main context is used
     * @return merged list of custom fields with priority-based deduplication, or empty list if invalid params
     */
    public static List<CustomFieldsEntity> getCustomFields(CustomFieldsSearchDto main, CustomFieldsSearchDto bonus) {

        if(main == null || main.isValid() == false) {
            Logger.error(CustomFieldsService.class, "Bad params for getRequiredFieldsAlphabets. Returning empty list.");
            return List.of();
        }

        CustomFieldsRepository customFieldsRepository = Tools.getSpringBean("customFieldsRepository", CustomFieldsRepository.class);
		if(customFieldsRepository == null) {
            Logger.error(CustomFieldsService.class, "Could not get customFieldsRepository bean from spring context, skipping custom fields retrieval and returning empty list");
            return List.of();
        }

        // First get all fields that have no entityId set - these are for all entities of specified class name (global fields)
        List<CustomFieldsEntity> globalCustomFields = customFieldsRepository.findAllGlobalCustomFields(main.getClassName(), CloudToolsForCore.getDomainId());

        // Now get all fields for the specific entityId (specific fields) for the same class name - these will override global fields if there are any
        List<CustomFieldsEntity> classCustomFields = customFieldsRepository.findAllByClassNameAndEntityId(main.getClassName(), main.getEntityId(), CloudToolsForCore.getDomainId());

        // Specific fields have higher priority than global ones, so we will override global fields with specific ones if there are any
        mergeLists(globalCustomFields, classCustomFields);

        // if bonusClassName is provided do merge BUT bonus class fields has HIGHEST priority
        if(bonus != null && bonus.isValid() == true) {
            List<CustomFieldsEntity> bonusClassCustomFields = customFieldsRepository.findAllByClassNameAndBonusContext(main.getClassName(), bonus.getClassName(), bonus.getEntityId(), CloudToolsForCore.getDomainId());
            mergeLists(classCustomFields, bonusClassCustomFields);
            classCustomFields = bonusClassCustomFields;
        }

        return classCustomFields;
    }

    /**
     * Converts custom field configuration into its canonical string representation.
     *
     * <p>Transforms entity properties into a structured value format based on field type:
     * <ul>
     * <li>text: "text" or "text-{maxLength}" or "text-{maxLength}, warningLength-{warningLength}"</li>
     * <li>select/multiselect: options string with optional leading | for non-required fields</li>
     * <li>enumeration: "enumeration{_option1_option2}" with optional _null suffix</li>
     * <li>docsIn: "docsIn_{groupId}" with optional _null suffix</li>
     * <li>autocomplete: "autocomplete:{options}"</li>
     * <li>json_group/json_doc: type name with optional _null suffix</li>
     * </ul>
     * </p>
     *
     * @param entity the custom field entity to convert
     * @return the same entity with updated value field, never null
     * @throws IllegalStateException if entity or its type is null/empty
     * @see #fromEntity(CustomFieldsEntity)
     */
    public static CustomFieldsEntity toEntity(CustomFieldsEntity entity) {

        if(entity == null || Tools.isEmpty(entity.getType())) throw new IllegalStateException("CustomFieldsEntity or its type must not be null/empty");

        String type = entity.getType();

        StringBuilder value = new StringBuilder("");
        if("text".equals(type)) {
            value.append("text");
            if(entity.getTextMaxLength() != null) value.append("-").append(entity.getTextMaxLength());
            if(entity.getTextWarningLength() != null) value.append(", warningLength-").append(entity.getTextWarningLength());
        } else if("select".equals(type)) {
            if(Tools.isFalse(entity.getRequired())) value.append("|"); //can be empty value
            value.append(entity.getSelectOptions());
        } else if("multiselect".equals(type)) {
            value.append("multiple:");
            if(Tools.isFalse(entity.getRequired())) value.append("|"); //can be empty value
            value.append(entity.getSelectOptions());
        } else if("json_group".equals(type) || "json_doc".equals(type)) {
            value.append(type);
            if(Tools.isFalse(entity.getRequired())) value.append("_null");
        } else if("docsIn".equals(type)) {
            value.append("docsIn_");
            if(entity.getDocInGroup() != null) value.append(entity.getDocInGroup().getGroupId());
            if(Tools.isFalse(entity.getRequired())) value.append("_null");
        } else if("enumeration".equals(type)) {
            String enumValue = entity.getEnumeration();
            if(Tools.isNotEmpty(enumValue)) {
                if(enumValue.startsWith("enumeration-options")) enumValue = enumValue.substring("enumeration-options".length());
                enumValue = Tools.replace(enumValue, "|", "_");
            }
            value.append("enumeration").append(enumValue);
            if(Tools.isFalse(entity.getRequired())) value.append("_null");
        } else if("autocomplete".equals(type)) {
            value.append("autocomplete:").append(entity.getAutocompleteOptions());
        } else {
            value.append(type);
        }

        entity.setValue( value.toString() );

        return entity;
    }

    /**
     * Parses the canonical string representation back into custom field properties.
     *
     * <p>Reverse operation of {@link #toEntity(CustomFieldsEntity)}. Extracts type-specific
     * properties from the value field:
     * <ul>
     * <li>text: parses maxLength and warningLength from value</li>
     * <li>select/multiselect: extracts selectOptions</li>
     * <li>enumeration: parses enumeration options from underscore-separated format</li>
     * <li>docsIn: resolves groupId to GroupDetails object</li>
     * <li>autocomplete: extracts autocompleteOptions</li>
     * </ul>
     * </p>
     *
     * @param entity the custom field entity with populated value field
     * @return the same entity with type-specific properties set, or original if type/value empty
     * @see #toEntity(CustomFieldsEntity)
     */
    public static CustomFieldsEntity fromEntity(CustomFieldsEntity entity) {
        if (entity == null || Tools.isEmpty(entity.getType()) || Tools.isEmpty(entity.getValue())) return entity;

        String type = entity.getType();
        String value = entity.getValue();

        if ("text".equals(type)) {
            // value format: "text" or "text-{maxLength}" or "text-{maxLength}, warningLength-{warningLength}" or "text, warningLength-{warningLength}"
            String remainder = value.startsWith("text-") ? value.substring(5) : value.startsWith("text") ? value.substring(4).trim() : value;
            if (remainder.contains(", warningLength-")) {
                int sepIdx = remainder.indexOf(", warningLength-");
                String maxPart = remainder.substring(0, sepIdx).trim();
                String warnPart = remainder.substring(sepIdx + 16).trim();
                if (Tools.isNotEmpty(maxPart)) entity.setTextMaxLength(Tools.getIntValue(maxPart, 0) > 0 ? Tools.getIntValue(maxPart, 0) : null);
                if (Tools.isNotEmpty(warnPart)) entity.setTextWarningLength(Tools.getIntValue(warnPart, 0) > 0 ? Tools.getIntValue(warnPart, 0) : null);
            } else if (Tools.isNotEmpty(remainder)) {
                entity.setTextMaxLength(Tools.getIntValue(remainder, 0) > 0 ? Tools.getIntValue(remainder, 0) : null);
            }
        } else if ("select".equals(type)) {
            entity.setSelectOptions(value.startsWith("|") ? value.substring(1) : value);
        } else if ("multiselect".equals(type)) {
            String remainder = value.startsWith("multiple:") ? value.substring(9) : value;
            entity.setSelectOptions(remainder.startsWith("|") ? remainder.substring(1) : remainder);
        } else if ("docsIn".equals(type)) {
            // value format: "docsIn_{groupId}" or "docsIn_{groupId}_null"
            String remainder = value.startsWith("docsIn_") ? value.substring(7) : value;
            if (remainder.endsWith("_null")) {
                remainder = remainder.substring(0, remainder.length() - 5);
            }
            long groupId = Tools.getLongValue(remainder, -1L);
            if (groupId > 0) {
                GroupDetails group = GroupsDB.getInstance().getGroup((int) groupId);
                if(group != null) entity.setDocInGroup(group);
            }
        } else if ("enumeration".equals(type)) {
            // value format: "enumeration{_option1_option2}" or "enumeration{_option1_option2}_null"
            String remainder = value.startsWith("enumeration") ? value.substring("enumeration".length()) : value;
            if (remainder.endsWith("_null")) {
                remainder = remainder.substring(0, remainder.length() - 5);
            }

            entity.setEnumeration("enumeration-options" + Tools.replace(remainder, "_", "|"));
        } else if("autocomplete".equals(type)) {
            String remainder = value.startsWith("autocomplete:") ? value.substring(13) : value;
            entity.setAutocompleteOptions(remainder);
        }

        return entity;
    }

    /**
     * Returns all available custom field types with localized labels.
     *
     * <p>Provides the complete list of supported field types for use in UI dropdowns or
     * configuration interfaces. Labels are retrieved from the i18n property bundle using
     * the key pattern "settings.custom-fields.type.{fieldType}".</p>
     *
     * @param prop the property bundle for label localization
     * @return list of LabelValue pairs with localized type names and their identifiers
     * @see #getFieldTypeLabel(Prop, String)
     */
    public static List<LabelValue> getFieldsTypes(Prop prop) {
        return List.of(
            new LabelValue( getFieldTypeLabel(prop, "text"), "text"),
            new LabelValue( getFieldTypeLabel(prop, "textarea"), "textarea"),
            new LabelValue( getFieldTypeLabel(prop, "select"), "select"),
            new LabelValue( getFieldTypeLabel(prop, "multiselect"), "multiselect"),
            new LabelValue( getFieldTypeLabel(prop, "boolean"), "boolean"),
            new LabelValue( getFieldTypeLabel(prop, "number"), "number"),
            new LabelValue( getFieldTypeLabel(prop, "date"), "date"),
            new LabelValue( getFieldTypeLabel(prop, "none"), "none"),
            new LabelValue( getFieldTypeLabel(prop, "autocomplete"), "autocomplete"),
            new LabelValue( getFieldTypeLabel(prop, "image"), "image"),
            new LabelValue( getFieldTypeLabel(prop, "link"), "link"),
            new LabelValue( getFieldTypeLabel(prop, "json_group"), "json_group"),
            new LabelValue( getFieldTypeLabel(prop, "json_doc"), "json_doc"),
            new LabelValue( getFieldTypeLabel(prop, "dir"), "dir"),
            new LabelValue( getFieldTypeLabel(prop, "docsIn"), "docsIn"),
            new LabelValue( getFieldTypeLabel(prop, "enumeration"), "enumeration"),
            new LabelValue( getFieldTypeLabel(prop, "uuid"), "uuid"),
            new LabelValue( getFieldTypeLabel(prop, "color"), "color")
        );
    }

    /**
     * Retrieves the localized label for a custom field type.
     *
     * @param prop the property bundle for label localization
     * @param fieldKey the field type key (e.g., "text", "select", "enumeration")
     * @return the localized label, retrieved using key "settings.custom-fields.type.{fieldKey}"
     */
    public static String getFieldTypeLabel(Prop prop, String fieldKey) {
        return prop.getText(FIELD_TYPE_KET_PREFIX + fieldKey);
    }

    /**
     * Returns the mapping of field types to their type-specific property names.
     *
     * <p>Specifies which properties should be visible in the UI editor for each field type.
     * Used to control which configuration options are shown when editing custom fields of
     * different types.</p>
     *
     * @return list of LabelValue pairs mapping field types to comma-separated property names
     */
    public static List<LabelValue> getSpecificFieldVisibility() {
        return List.of(
            new LabelValue("text", "textMaxLength,textWarningLength,textWarningText"),
            new LabelValue("select", "selectOptions"),
            new LabelValue("multiselect", "selectOptions"),
            new LabelValue("autocomplete", "autocompleteOptions"),
            new LabelValue("docsIn", "docInGroup"),
            new LabelValue("enumeration", "enumeration")
        );
    }

    /**
     * Retrieves the alphabet characters of required custom fields for the given context.
     *
     * <p>Returns only fields marked as required, extracting the first character from
     * their alphabet identifier. Automatically derives bonus context when applicable.</p>
     *
     * @param main the search context containing className and entityId
     * @return list of single-character alphabet keys for required fields, or empty list if invalid params
     * @see #getRequiredFieldsAlphabets(CustomFieldsSearchDto, CustomFieldsSearchDto)
     */
    public static List<Character> getRequiredFieldsAlphabets(CustomFieldsSearchDto main) {
        if(main == null || main.isValid() == false) {
            Logger.error(CustomFieldsService.class, "Bad params for getRequiredFieldsAlphabets. Returning empty list.");
            return List.of();
        }

        return getRequiredFieldsAlphabets(main, tryGetBonus(main));
    }

    /**
     * Retrieves the alphabet characters of required custom fields for the given contexts.
     *
     * <p>Filters custom fields from both main and bonus contexts to return only those marked as
     * required, extracting the first character from each field's alphabet identifier. Maintains
     * insertion order via LinkedHashSet.</p>
     *
     * @param main primary search context with className and entityId
     * @param bonus optional secondary context for bonus class fields
     * @return ordered list of single-character alphabet keys for required fields
     * @see #getCustomFields(CustomFieldsSearchDto, CustomFieldsSearchDto)
     */
    public static List<Character> getRequiredFieldsAlphabets(CustomFieldsSearchDto main, CustomFieldsSearchDto bonus) {
        // return only required fields
        Set<Character> requiredAlphabets = new LinkedHashSet<>();
        for(CustomFieldsEntity customField : getCustomFields(main, bonus)) {
            if(customField != null && Tools.isTrue(customField.getRequired()) && Tools.isEmpty(customField.getAlphabet()) == false) {
                requiredAlphabets.add(customField.getAlphabet().charAt(0));
            }
        }
        return new ArrayList<>(requiredAlphabets);
    }

    /* PRIVATE STATIC METHODS */

    /**
     * Tries to derive a bonus lookup context from the main DTO.
     *
     * <p>Currently supports document details by resolving the template from {@code tempId}.</p>
     *
     * @param main main custom fields lookup context
     * @return derived bonus lookup context, or {@code null} when not applicable
     */
    private static CustomFieldsSearchDto tryGetBonus(CustomFieldsSearchDto main) {
        if("sk.iway.iwcm.doc.DocDetails".equals(main.getClassName()) && main.getBonusParam() instanceof Integer tempId) {
            // Try get temp
            TemplateDetails temp = TemplatesDB.getInstance().getTemplate(tempId);
            if(temp != null) {
                return new CustomFieldsSearchDto(temp.getClass().getName(), temp.getTempId());
            }
        }

        return null;
    }

    /**
     * Merges two custom fields lists by alphabet key while preserving higher-priority entries.
     *
     * @param lesserPriorityList source list with lower-priority values
     * @param higherPriorityList target list with higher-priority values
     */
    private static void mergeLists(List<CustomFieldsEntity> lesserPriorityList, List<CustomFieldsEntity> higherPriorityList) {
        if(lesserPriorityList == null || higherPriorityList == null) return;

        Set<String> higherPriorityAlphabets = new HashSet<>();
        for(CustomFieldsEntity higher : higherPriorityList) {
            if(higher != null && Tools.isEmpty(higher.getAlphabet()) == false) {
                higherPriorityAlphabets.add(higher.getAlphabet());
            }
        }

        for(CustomFieldsEntity lesser : lesserPriorityList) {
            if(lesser == null || Tools.isEmpty(lesser.getAlphabet())) {
                continue;
            }

            if(higherPriorityAlphabets.add(lesser.getAlphabet())) {
                higherPriorityList.add(lesser);
            }
        }
    }

    /* PUBLIC METHODS  */

    /**
     * Returns class name suggestions that contain the given search term.
     *
     * @param term search text
     * @return case-insensitively sorted list of matching class names
     */
    public List<String> getClassOptions(String term) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term)) return ac;

        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
        if(Tools.isEmpty(normalizedTerm)) return ac;

        for(String className : getClassSet()) {
            if(Tools.isEmpty(className)) {
                continue;
            }

            if(className.toLowerCase(Locale.ROOT).contains(normalizedTerm))
                ac.add(className);
        }

        ac.sort(String.CASE_INSENSITIVE_ORDER);

        return ac;
    }

    /* PRIVATE METHODS */

    /**
     * Returns a cached set of eligible class names, scanning the classpath when cache is empty.
     *
     * @return set of class names that can provide custom fields
     */
    private Set<String> getClassSet() {
        Cache c = Cache.getInstance();
        Object mapObject = c.getObject(CLASS_FIELD_MAP_KEY);

        if(mapObject instanceof Set<?> cachedSet) {
            Set<String> classSet = new HashSet<>();
            for(Object cachedItem : cachedSet) {
                if(cachedItem instanceof String cachedClassName && Tools.isEmpty(cachedClassName) == false) {
                    classSet.add(cachedClassName);
                }
            }
            return classSet;
        }

        Set<String> classSet = findClassesWithAnnotation();
        c.setObject(CLASS_FIELD_MAP_KEY, classSet, 60);
        return classSet;
    }

    /**
     * Scans configured application packages for entities containing nested datatable fields.
     *
     * @return set of fully qualified class names matching the required annotation pattern
     */
    private Set<String> findClassesWithAnnotation() {

        Set<String> foundClasses = new HashSet<>();

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (String packageName : AppManager.getPackageNames()) {
			for (BeanDefinition beanDef : provider.findCandidateComponents(packageName)) {
                try {

                    String fqdn = beanDef.getBeanClassName();
				    if (fqdn == null) { continue; }

                    if(hasDataTableColumnNested(fqdn) == true)
                        foundClasses.add( fqdn );

                } catch (Exception e) {
					Logger.error(CustomFieldsService.class, "Error while scanning class for custom fields: " + e.getMessage(), e);
				}
            }
        }

        //add sk.iway.iwcm.doc.TemplateDetails if not present, because it is not entity
        if(foundClasses.contains("sk.iway.iwcm.doc.TemplateDetails") == false) {
            foundClasses.add("sk.iway.iwcm.doc.TemplateDetails");
        }

        return foundClasses;
    }

    /**
     * Checks whether a class contains a field annotated with {@link DataTableColumnNested}
     * and assignable to {@link BaseEditorFields}.
     *
     * @param className fully qualified class name
     * @return {@code true} if the class is considered eligible for custom fields lookup
     * @throws ClassNotFoundException when the class cannot be loaded
     */
    private boolean hasDataTableColumnNested(String className) throws ClassNotFoundException {
        for (Field field : AuditEntityListener.getDeclaredFieldsTwoLevels( Class.forName(className)) ) {
            DataTableColumnNested annotation = field.getAnnotation(DataTableColumnNested.class);

            if (annotation == null) {
                continue;
            }

            Class<?> fieldType = field.getType();
            if (fieldType != null && BaseEditorFields.class.isAssignableFrom(fieldType)) {
                return true;
            }
        }

        return false;
    }
}