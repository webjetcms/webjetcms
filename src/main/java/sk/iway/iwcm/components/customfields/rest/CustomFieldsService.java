package sk.iway.iwcm.components.customfields.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Service
public class CustomFieldsService {

    private static final String CLASS_FIELD_MAP_KEY = "CustomFieldsService_classFieldsMap";
    public static final Map<String, String> BONUS_PARAMS = Map.of(
        "sk.iway.iwcm.doc.DocDetails", "tempId"
    );

    /* PUBLIC STATIC METHODS */

    /**
     * Returns required custom field alphabets for the main entity context.
     *
     * <p>If a bonus context can be derived from the main DTO, it is included automatically.</p>
     *
     * @param main main custom fields lookup context
     * @return list of required field alphabet characters, or an empty list for invalid input
     */
    public static List<Character> getRequiredFieldsAlphabets(CustomFieldsSearchDto main) {
        if(main == null || main.isValid() == false) {
            Logger.error(CustomFieldsService.class, "Bad params for getRequiredFieldsAlphabets. Returning empty list.");
            return List.of();
        }

        return getRequiredFieldsAlphabets(main, tryGetBonus(main));
    }

    /**
     * Returns required custom field alphabets merged from global, main and optional bonus context.
     *
     * <p>Priority is applied in this order: global fields, main fields, bonus fields.</p>
     *
     * @param main main custom fields lookup context
     * @param bonus optional bonus context with highest priority
     * @return list of required field alphabet characters, or an empty list for invalid input
     */
    public static List<Character> getRequiredFieldsAlphabets(CustomFieldsSearchDto main, CustomFieldsSearchDto bonus) {

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

        // return only required fields
        Set<Character> requiredAlphabets = new LinkedHashSet<>();
        for(CustomFieldsEntity customField : classCustomFields) {
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