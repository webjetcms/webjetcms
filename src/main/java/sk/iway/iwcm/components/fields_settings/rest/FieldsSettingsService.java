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
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsRepository;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsEntity;
import sk.iway.iwcm.components.tabs_settings.rest.TabsSettingsService;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Caches field settings by class and domain and discovers annotated entities for autocomplete.
 * Autocomplete metadata comes from class annotations and domain-scoped tab settings.
 */
@Service
public class FieldsSettingsService {

    private static final String CLASS_OPTIONS_KEY = "FieldsSettingsService_classOptions";
    private static final String SETTINGS_CACHE_PREFIX = "FieldSettings_";
    private static final int SETTINGS_CACHE_SECONDS = 3600;

    private final FieldsSettingsRepository repository;
    private final TabsSettingsService tabsSettingsService;

    public FieldsSettingsService(FieldsSettingsRepository repository, TabsSettingsService tabsSettingsService) {
        this.repository = repository;
        this.tabsSettingsService = tabsSettingsService;
    }

    /**
     * Loads settings for a class and domain, caching the result for one hour.
     *
     * @param className fully qualified entity class name
     * @param domainId domain owning the settings
     * @return the cached settings array, empty when no overrides exist
     */
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

    /**
     * Builds a fixed-length cache key from the class name and domain ID.
     *
     * @param className fully qualified entity class name
     * @param domainId domain owning the settings
     * @return the settings prefix followed by the SHA-256 hash of the class/domain pair
     */
    public String getCacheKey(String className, int domainId) {
        return SETTINGS_CACHE_PREFIX + DigestUtils.sha256Hex((className + "\n" + domainId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Removes cached settings locally and requests invalidation on the other cluster nodes.
     *
     * @param cacheKey key returned by {@link #getCacheKey(String, int)}
     */
    public void clearCache(String cacheKey) {
        Cache.getInstance().removeObject(cacheKey, true);
    }

    /**
     * Holds annotation titles used to build localized autocomplete options.
     *
     * @param fieldTitles field paths mapped to title translation keys, excluding ID columns
     * @param tabTitles declared tab IDs mapped to title translation keys
     * @param inferredTabFields columns used to infer tabs when no tabs are explicitly declared
     */
    private record ClassOptions(Map<String, String> fieldTitles, Map<String, String> tabTitles, List<Field> inferredTabFields) {}

    /**
     * Returns sorted entity class names containing the search term and at least one datatable column.
     *
     * @param term search text, or {@code %} or {@code *} to return all eligible class names
     * @return case-insensitively sorted matching class names, or an empty list for an empty term
     */
    public List<String> getClassOptions(String term) {
        if (Tools.isEmpty(term)) return List.of();
        return filterOptions(getClassOptionsMap().keySet(), term);
    }

    /**
     * Returns localized column titles with field paths as labels and raw field paths as values, excluding ID columns.
     *
     * @param term search text, or {@code %} or {@code *} to return all field names
     * @param className fully qualified selected class name
     * @param prop translations for the current request
     * @return options matching the title or field name, or an empty list for empty input or an unknown class
     */
    public List<LabelValue> getFieldOptions(String term, String className, Prop prop) {
        if (Tools.isEmpty(term) || Tools.isEmpty(className)) return List.of();
        ClassOptions options = getClassOptionsMap().get(className);
        return options == null ? List.of() : getLabeledOptions(options.fieldTitles(), term, prop);
    }

    /**
     * Returns localized tab titles from the selected class's annotations and current-domain settings with raw IDs as values.
     *
     * @param term search text, or {@code %} or {@code *} to return all tab IDs
     * @param className fully qualified selected class name
     * @param prop translations for the current request
     * @return options matching the title or tab ID, or an empty list for empty input or no matching tabs
     */
    public List<LabelValue> getTabOptions(String term, String className, Prop prop) {
        if (Tools.isEmpty(term) || Tools.isEmpty(className)) return List.of();
        return getLabeledOptions(getTabTitles(className, CloudToolsForCore.getDomainId(), null), term, prop);
    }

    /**
     * Checks a tab ID against the entity's editor metadata and settings in the supplied domain.
     *
     * @param className fully qualified selected class name
     * @param tabId raw tab ID to validate
     * @param domainId domain owning the settings
     * @return whether the tab exists for this entity and domain
     */
    public boolean hasTab(String className, String tabId, int domainId) {
        return hasTab(className, tabId, domainId, null);
    }

    /**
     * Checks a tab ID while excluding a persisted setting that is being changed.
     * Declared or inferred tabs remain available even if their saved override is excluded.
     *
     * @param className fully qualified selected class name
     * @param tabId raw tab ID to validate
     * @param domainId domain owning the settings
     * @param excludedSettingsId persisted tab setting being edited, or null to include all settings
     * @return whether the tab still exists without the excluded setting
     */
    public boolean hasTab(String className, String tabId, int domainId, Long excludedSettingsId) {
        if (Tools.isEmpty(className) || Tools.isEmpty(tabId)) return false;
        return getTabTitles(className, domainId, excludedSettingsId).containsKey(tabId);
    }

    /** Combines discovered editor tabs with saved tabs and title overrides from one domain. */
    private Map<String, String> getTabTitles(String className, int domainId, Long excludedSettingsId) {
        ClassOptions options = getClassOptionsMap().get(className);
        Map<String, String> tabTitles = options == null ? new HashMap<>() : new HashMap<>(options.tabTitles());
        if (options != null && options.inferredTabFields().isEmpty() == false) {
            Map<String, FieldsSettingsEntity> fieldSettings = new HashMap<>();
            for (FieldsSettingsEntity setting : getFieldsSettings(className, domainId)) {
                fieldSettings.put(setting.getFieldName(), setting);
            }
            for (Field field : options.inferredTabFields()) {
                String tab = field.getAnnotation(DataTableColumn.class).tab();
                FieldsSettingsEntity setting = fieldSettings.get(field.getName());
                if (setting != null && Tools.isNotEmpty(setting.getTab())) tab = setting.getTab();
                if (Tools.isNotEmpty(tab)) tabTitles.putIfAbsent(tab, tab);
            }
        }
        for (TabsSettingsEntity setting : tabsSettingsService.getTabsSettings(className, domainId)) {
            if (excludedSettingsId != null && excludedSettingsId.equals(setting.getId())) continue;
            if (Tools.isNotEmpty(setting.getTitle())) tabTitles.put(setting.getTabId(), setting.getTitle());
            else tabTitles.putIfAbsent(setting.getTabId(), "");
        }
        return tabTitles;
    }

    /**
     * Builds localized options, filters their labels and sorts them without regard to case.
     *
     * @param titles raw values mapped to title translation keys
     * @param term search text; a trimmed {@code %} or {@code *} includes every option
     * @param prop translations for the current request
     * @return matching labels paired with their original values
     */
    private List<LabelValue> getLabeledOptions(Map<String, String> titles, String term, Prop prop) {
        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
        boolean showAll = "%".equals(normalizedTerm) || "*".equals(normalizedTerm);
        return titles.entrySet().stream()
            .map(option -> new LabelValue(getOptionLabel(option.getKey(), option.getValue(), prop), option.getKey()))
            .filter(option -> showAll || option.getLabel().toLowerCase(Locale.ROOT).contains(normalizedTerm))
            .sorted(Comparator.comparing(LabelValue::getLabel, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /**
     * Formats a translated title with its raw value, falling back to the value for an empty title.
     *
     * @param value field path or tab ID
     * @param titleKey translation key, optionally wrapped in {@code [[#{...}]]} or followed by semicolon-separated parameters
     * @param prop translations for the current request
     * @return translated title followed by the value in parentheses, or only the value
     */
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

    /**
     * Filters and sorts string options without regard to case.
     *
     * @param options available values
     * @param term search text; a trimmed {@code %} or {@code *} includes every value
     * @return sorted values containing the trimmed search text, or all values for a wildcard
     */
    private List<String> filterOptions(Collection<String> options, String term) {
        String normalizedTerm = term.trim().toLowerCase(Locale.ROOT);
        boolean showAll = "%".equals(normalizedTerm) || "*".equals(normalizedTerm);
        return options.stream()
            .filter(option -> showAll || option.toLowerCase(Locale.ROOT).contains(normalizedTerm))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    /**
     * Loads discovered entity metadata from the shared cache, scanning application packages on a cache miss.
     *
     * @return class names mapped to field and tab titles, cached for 60 minutes
     */
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
     * Scans configured application packages for classes annotated with {@link Entity} or {@link DataTableTabs}.
     *
     * @return classes with their annotated fields (including inherited and nested fields) and editor tab IDs
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
        provider.addIncludeFilter(new AnnotationTypeFilter(DataTableTabs.class));

        for (String packageName : AppManager.getPackageNames()) {
            for (BeanDefinition beanDefinition : provider.findCandidateComponents(packageName)) {
                String className = beanDefinition.getBeanClassName();
                if (className == null || foundClasses.containsKey(className)) continue;

                try {
                    Class<?> type = Class.forName(className, false, ClassUtils.getDefaultClassLoader());
                    Map<String, String> fieldTitles = new HashMap<>();
                    if (collectFieldTitles(type, "", fieldTitles, new HashSet<>()) == false) continue;
                    foundClasses.put(className, new ClassOptions(fieldTitles, collectTabTitles(type), collectInferredTabFields(type)));
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

    /** Discovers explicitly declared tabs, including generated application common settings. */
    private Map<String, String> collectTabTitles(Class<?> type) {
        Map<String, String> tabTitles = new HashMap<>();
        WebjetAppStore appStore = type.getAnnotation(WebjetAppStore.class);
        boolean includeCommonSettings = appStore != null && appStore.commonSettings();
        DataTableTabs tabs = type.getAnnotation(DataTableTabs.class);
        if (tabs != null) {
            for (DataTableTab tab : tabs.tabs()) {
                if (Tools.isNotEmpty(tab.id())) tabTitles.putIfAbsent(tab.id(), tab.title());
            }
            if (includeCommonSettings) tabTitles.putIfAbsent("commonSettings", "commonSettings");
        }
        return tabTitles;
    }

    /** Keeps columns eligible for tab inference before current-domain field overrides are applied. */
    private List<Field> collectInferredTabFields(Class<?> type) {
        if (type.isAnnotationPresent(DataTableTabs.class)) return List.of();
        WebjetAppStore appStore = type.getAnnotation(WebjetAppStore.class);
        boolean includeCommonSettings = appStore != null && appStore.commonSettings();
        return Arrays.stream(AuditEntityListener.getDeclaredFieldsTwoLevels(type))
            .filter(field -> field.isAnnotationPresent(DataTableColumn.class))
            .filter(field -> includeCommonSettings || "commonSettings".equals(field.getAnnotation(DataTableColumn.class).tab()) == false)
            .toList();
    }
}
