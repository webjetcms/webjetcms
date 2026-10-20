package sk.iway.iwcm.components.fields_settings.rest;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import jakarta.persistence.Entity;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsRepository;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class FieldsSettingsService {

    private static final String CLASS_OPTIONS_KEY = "FieldsSettingsService_classOptions";
    private static final String SETTINGS_CACHE_PREFIX = "FieldSettings_";
    private static final int SETTINGS_CACHE_SECONDS = 1800;

    private final FieldsSettingsRepository repository;

    public FieldsSettingsService(FieldsSettingsRepository repository) {
        this.repository = repository;
    }

    public FieldsSettingsEntity[] getFieldsSettings(String className, int domainId) {
        Cache cache = Cache.getInstance();
        String cacheKey = getCacheKey(className, domainId);
        FieldsSettingsEntity[] settings = cache.getObject(cacheKey, FieldsSettingsEntity[].class);
        if (settings == null) {
            settings = repository.findAllByClassNameAndDomainId(className, domainId).toArray(FieldsSettingsEntity[]::new);
            cache.setObjectSeconds(cacheKey, settings, SETTINGS_CACHE_SECONDS, false);
        }
        return settings;
    }

    public String getCacheKey(String className, int domainId) {
        return SETTINGS_CACHE_PREFIX + DigestUtils.sha256Hex((className + "\n" + domainId).getBytes(StandardCharsets.UTF_8));
    }

    public void clearCache(String cacheKey) {
        Cache.getInstance().removeObject(cacheKey, true);
    }

    private record ClassOptions(Map<String, String> fieldTitles, Map<String, String> tabTitles) {
    }

    /**
     * Returns sorted entity class names containing the search term and at least one datatable column.
     *
     * @param term search text
     * @return case-insensitively sorted matching class names
     */
    public List<String> getClassOptions(String term) {
        if (Tools.isEmpty(term)) return List.of();
        return filterOptions(getClassOptionsMap().keySet(), term);
    }

    /**
     * Returns localized column titles with field paths as labels and raw field paths as values, excluding ID columns.
     *
     * @param term search text, or {@code %} to return all field names
     * @param className fully qualified selected class name
     * @param prop translations for the current request
     * @return options matching the title or field name, or an empty list for an unknown class
     */
    public List<LabelValue> getFieldOptions(String term, String className, Prop prop) {
        if (Tools.isEmpty(term) || Tools.isEmpty(className)) return List.of();
        ClassOptions options = getClassOptionsMap().get(className);
        return options == null ? List.of() : getLabeledOptions(options.fieldTitles(), term, prop);
    }

    /**
     * Returns localized tab titles from the selected class's {@link DataTableTabs} annotation with raw IDs as values.
     *
     * @param term search text, or {@code %} to return all tab IDs
     * @param className fully qualified selected class name
     * @param prop translations for the current request
     * @return options matching the title or tab ID, or an empty list when the class has no declared tabs
     */
    public List<LabelValue> getTabOptions(String term, String className, Prop prop) {
        if (Tools.isEmpty(term) || Tools.isEmpty(className)) return List.of();
        ClassOptions options = getClassOptionsMap().get(className);
        return options == null ? List.of() : getLabeledOptions(options.tabTitles(), term, prop);
    }

    private List<LabelValue> getLabeledOptions(Map<String, String> titles, String term, Prop prop) {
        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
        boolean showAll = "%".equals(normalizedTerm) || "*".equals(normalizedTerm);
        return titles.entrySet().stream()
            .map(option -> new LabelValue(getOptionLabel(option.getKey(), option.getValue(), prop), option.getKey()))
            .filter(option -> showAll || option.getLabel().toLowerCase(Locale.ROOT).contains(normalizedTerm))
            .sorted(Comparator.comparing(LabelValue::getLabel, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private String getOptionLabel(String value, String titleKey, Prop prop) {
        if (titleKey.startsWith("[[#{") && titleKey.endsWith("}]]")) {
            titleKey = titleKey.substring(4, titleKey.length() - 3);
        }
        if (titleKey.isBlank() || "&nbsp;".equals(titleKey)) return value;
        String[] titleParts = titleKey.split(";");
        String title = titleParts.length > 1
            ? prop.getTextWithParams(titleParts[0], Arrays.copyOfRange(titleParts, 1, titleParts.length))
            : prop.getText(titleKey);
        if (Tools.isEmpty(title) || "&nbsp;".equals(title)) return value;
        return title + " (" + value + ")";
    }

    private List<String> filterOptions(Collection<String> options, String term) {
        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
        boolean showAll = "%".equals(normalizedTerm) || "*".equals(normalizedTerm);
        return options.stream()
            .filter(option -> showAll || option.toLowerCase(Locale.ROOT).contains(normalizedTerm))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ClassOptions> getClassOptionsMap() {
        Cache cache = Cache.getInstance();
        Map<String, ClassOptions> options = cache.getObject(CLASS_OPTIONS_KEY, Map.class);
        if (options == null) {
            options = findClassesWithAnnotation();
            cache.setObject(CLASS_OPTIONS_KEY, options, 60);
        }
        return options;
    }

    /**
     * Scans configured application packages for classes annotated with {@link Entity}.
     *
     * @return classes with their annotated fields (including inherited and nested fields) and declared tab IDs
     */
    private Map<String, ClassOptions> findClassesWithAnnotation() {
        Map<String, ClassOptions> foundClasses = new HashMap<>();
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (String packageName : AppManager.getPackageNames()) {
            for (BeanDefinition beanDefinition : provider.findCandidateComponents(packageName)) {
                String className = beanDefinition.getBeanClassName();
                if (className == null || foundClasses.containsKey(className)) continue;

                try {
                    Class<?> type = Class.forName(className, false, ClassUtils.getDefaultClassLoader());
                    Map<String, String> fieldTitles = new HashMap<>();
                    if (collectFieldTitles(type, "", fieldTitles, new HashSet<>()) == false) continue;

                    Map<String, String> tabTitles = new HashMap<>();
                    DataTableTabs tabs = type.getAnnotation(DataTableTabs.class);
                    if (tabs != null) {
                        for (DataTableTab tab : tabs.tabs()) {
                            if (Tools.isNotEmpty(tab.id())) tabTitles.putIfAbsent(tab.id(), tab.title());
                        }
                    }
                    foundClasses.put(className, new ClassOptions(fieldTitles, tabTitles));
                } catch (Exception e) {
                    Logger.error(FieldsSettingsService.class, "Error while scanning class for field settings: " + className, e);
                } catch (LinkageError e) {
                    // Keep separate handlers for compatibility with exception-handler weaving.
                    Logger.error(FieldsSettingsService.class, "Error while scanning class for field settings: " + className, e);
                }
            }
        }

        return foundClasses;
    }

    /**
     * Collects column titles using datatable field prefixes, following nested types without revisiting ancestors.
     *
     * @param type class whose fields and direct parent's fields are inspected
     * @param prefix path prefix for the current nested object
     * @param fieldTitles collected field paths and translation keys
     * @param ancestors classes on the current traversal path, used to prevent circular references
     * @return whether any datatable column was found, including ID columns
     */
    private boolean collectFieldTitles(Class<?> type, String prefix, Map<String, String> fieldTitles, Set<Class<?>> ancestors) {
        if (ancestors.add(type) == false) return false;
        boolean hasColumns = false;
        for (Field field : AuditEntityListener.getDeclaredFieldsTwoLevels(type)) {
            DataTableColumn column = field.getAnnotation(DataTableColumn.class);
            if (column != null) {
                hasColumns = true;
                if (Arrays.asList(column.inputType()).contains(DataTableColumnType.ID) == false) {
                    fieldTitles.putIfAbsent(prefix + field.getName(), column.title());
                }
            }
            DataTableColumnNested nested = field.getAnnotation(DataTableColumnNested.class);
            if (nested != null) {
                String nestedPrefix = "auto".equals(nested.prefix()) ? field.getName() + "." : nested.prefix();
                hasColumns |= collectFieldTitles(field.getType(), prefix + nestedPrefix, fieldTitles, ancestors);
            }
        }
        ancestors.remove(type);
        return hasColumns;
    }
}
