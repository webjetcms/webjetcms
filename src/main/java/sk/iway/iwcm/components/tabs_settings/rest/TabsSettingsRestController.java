package sk.iway.iwcm.components.tabs_settings.rest;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.fields_settings.rest.FieldsSettingsService;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsEntity;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Manages domain-scoped tab settings and supplies class and tab autocomplete options.
 * Access requires the {@code customFields} permission.
 */
@RestController
@RequestMapping("/admin/rest/settings/tabs-settings")
@PreAuthorize("@WebjetSecurityService.hasPermission('customFields')")
@Datatable
public class TabsSettingsRestController extends DatatableRestControllerV2<TabsSettingsEntity, Long> {

    private final TabsSettingsRepository repository;
    private final FieldsSettingsService fieldsSettingsService;
    private final TabsSettingsService tabsSettingsService;

    @Autowired
    public TabsSettingsRestController(TabsSettingsRepository repository, FieldsSettingsService fieldsSettingsService, TabsSettingsService tabsSettingsService) {
        super(repository, TabsSettingsEntity.class);
        this.repository = repository;
        this.fieldsSettingsService = fieldsSettingsService;
        this.tabsSettingsService = tabsSettingsService;
    }

    @Override
    public TabsSettingsEntity processToEntity(TabsSettingsEntity entity, ProcessItemAction action) {
        entity.setDomainId(getDomainId());
        return entity;
    }

    /** Inserts settings and invalidates the saved class and domain's cache. */
    @Override
    public TabsSettingsEntity insertItem(TabsSettingsEntity entity) {
        TabsSettingsEntity saved = super.insertItem(entity);
        tabsSettingsService.clearCache(tabsSettingsService.getCacheKey(saved.getClassName(), saved.getDomainId()));
        return saved;
    }

    /** Updates settings and invalidates both cache keys when the target class changes. */
    @Override
    public TabsSettingsEntity editItem(TabsSettingsEntity entity, long id) {
        String oldCacheKey = getStoredCacheKey(id);
        TabsSettingsEntity saved = super.editItem(entity, id);
        String newCacheKey = tabsSettingsService.getCacheKey(saved.getClassName(), saved.getDomainId());
        if (oldCacheKey != null && oldCacheKey.equals(newCacheKey) == false) {
            tabsSettingsService.clearCache(oldCacheKey);
        }
        tabsSettingsService.clearCache(newCacheKey);
        return saved;
    }

    /** Invalidates the persisted settings' cache only after successful deletion. */
    @Override
    public boolean deleteItem(TabsSettingsEntity entity, long id) {
        String cacheKey = getStoredCacheKey(id);
        boolean deleted = super.deleteItem(entity, id);
        if (deleted && cacheKey != null) tabsSettingsService.clearCache(cacheKey);
        return deleted;
    }

    /** Resolves the cache key from persisted settings in the current domain. */
    private String getStoredCacheKey(long id) {
        return repository.findFirstByIdAndDomainId(id, getDomainId())
            .map(stored -> tabsSettingsService.getCacheKey(stored.getClassName(), stored.getDomainId()))
            .orElse(null);
    }

    /**
     * Preserves nullable Boolean settings omitted from an import while allowing explicit null values.
     */
    @Override
    protected void copyEntityIntoOriginal(TabsSettingsEntity entity, TabsSettingsEntity original) {
        if (isImporting()) {
            Set<String> importedColumns = getImportedColumns();
            if (importedColumns == null || importedColumns.contains("selected") == false) {
                entity.setSelected(original.getSelected());
            }
            if (importedColumns == null || importedColumns.contains("hideOnCreate") == false) {
                entity.setHideOnCreate(original.getHideOnCreate());
            }
        }
        super.copyEntityIntoOriginal(entity, original);
    }

    /**
     * Validates relative tab ordering and rejects duplicate class/tab settings in the current domain.
     * Imports validate ordering but rely on the database constraint for duplicates because their IDs are row indexes.
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, TabsSettingsEntity> target, Identity user, Errors errors, Long id, TabsSettingsEntity entity) {
        super.validateEditor(request, target, user, errors, id, entity);
        if (errors.hasErrors() || (target.isInsert() == false && target.isUpdate() == false)) return;
        if (Tools.isEmpty(entity.getClassName()) || Tools.isEmpty(entity.getTabId())) return;

        Long excludedSettingsId = isImporting() == false && target.isUpdate() ? id : null;
        if (entity.getTabId().equals(entity.getSortAfter())) {
            errors.rejectValue("errorField.sortAfter", null, getProp().getText("settings.tabs-settings.sort_after_self-err"));
        } else if (Tools.isNotEmpty(entity.getSortAfter()) && "FIRST".equals(entity.getSortAfter()) == false
                && fieldsSettingsService.hasTab(entity.getClassName(), entity.getSortAfter(), getDomainId(), excludedSettingsId) == false
                && hasPendingImportTab(target, entity) == false) {
            errors.rejectValue("errorField.sortAfter", null, getProp().getText("settings.tabs-settings.unsupported_tab-err"));
        }

        if (isImporting()) return;

        Long excludedId = target.isUpdate() && id != null ? id : -1L;
        if (repository.existsByClassNameAndTabIdAndDomainIdAndIdNot(entity.getClassName(), entity.getTabId(), getDomainId(), excludedId)) {
            String message = getProp().getText("settings.tabs-settings.duplicity-err");
            errors.rejectValue("errorField.className", null, message);
            errors.rejectValue("errorField.tabId", null, message);
        }
    }

    /** Allows references within an append batch only when validation failures reject the entire batch. */
    private boolean hasPendingImportTab(DatatableRequest<Long, TabsSettingsEntity> target, TabsSettingsEntity entity) {
        if (isImporting() == false || target.isInsert() == false || "append".equals(target.getImportMode()) == false
                || target.isSkipWrongData() || target.getData() == null) return false;
        return target.getData().values().stream().anyMatch(candidate -> candidate != null && candidate != entity
            && entity.getClassName().equals(candidate.getClassName()) && entity.getSortAfter().equals(candidate.getTabId()));
    }

    @GetMapping("/autocomplete-class")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return fieldsSettingsService.getClassOptions(term);
    }

    @GetMapping("/autocomplete-tab")
    public List<LabelValue> getAutocompleteTab(@RequestParam String term, @RequestParam("DTE_Field_className") String className) {
        return fieldsSettingsService.getTabOptions(term, className, getProp());
    }
}
