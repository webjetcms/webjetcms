package sk.iway.iwcm.components.fields_settings.rest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapperImpl;
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
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Manages field settings through DataTables Editor and supplies class, field and tab autocomplete options.
 * Saves use the current domain, and successful changes invalidate the affected settings caches.
 * Access requires the {@code customFields} permission.
 */
@RestController
@RequestMapping("/admin/rest/settings/fields-settings")
@PreAuthorize("@WebjetSecurityService.hasPermission('customFields')")
@Datatable
public class FieldsSettingsRestController extends DatatableRestControllerV2<FieldsSettingsEntity, Long> {

    private final FieldsSettingsRepository repository;
    private final FieldsSettingsService fieldsSettingsService;

    @Autowired
    public FieldsSettingsRestController(FieldsSettingsRepository repository, FieldsSettingsService fieldsSettingsService) {
        super(repository, FieldsSettingsEntity.class);
        this.repository = repository;
        this.fieldsSettingsService = fieldsSettingsService;
    }

    @Override
    public FieldsSettingsEntity processToEntity(FieldsSettingsEntity entity, ProcessItemAction action) {
        entity.setDomainId(getDomainId());
        return entity;
    }

    /**
     * Inserts settings and invalidates the cache for the saved class and domain.
     */
    @Override
    public FieldsSettingsEntity insertItem(FieldsSettingsEntity entity) {
        FieldsSettingsEntity saved = super.insertItem(entity);
        fieldsSettingsService.clearCache(fieldsSettingsService.getCacheKey(saved.getClassName(), saved.getDomainId()));
        return saved;
    }

    /**
     * Updates settings and invalidates both the previous and saved cache keys when they differ.
     */
    @Override
    public FieldsSettingsEntity editItem(FieldsSettingsEntity entity, long id) {
        String oldCacheKey = getStoredCacheKey(id);
        FieldsSettingsEntity saved = super.editItem(entity, id);
        String newCacheKey = fieldsSettingsService.getCacheKey(saved.getClassName(), saved.getDomainId());
        if (oldCacheKey != null && oldCacheKey.equals(newCacheKey) == false) {
            fieldsSettingsService.clearCache(oldCacheKey);
        }
        fieldsSettingsService.clearCache(newCacheKey);
        return saved;
    }

    /**
     * Deletes settings and invalidates their stored cache key when deletion succeeds.
     */
    @Override
    public boolean deleteItem(FieldsSettingsEntity entity, long id) {
        String cacheKey = getStoredCacheKey(id);
        boolean deleted = super.deleteItem(entity, id);
        if (deleted && cacheKey != null) fieldsSettingsService.clearCache(cacheKey);
        return deleted;
    }

    /**
     * Resolves a record's cache key using its persisted class name and current domain.
     */
    private String getStoredCacheKey(long id) {
        return repository.findFirstByIdAndDomainId(id, getDomainId())
            .map(stored -> fieldsSettingsService.getCacheKey(stored.getClassName(), stored.getDomainId()))
            .orElse(null);
    }

    /**
     * Merges submitted settings while preserving nullable Boolean fields omitted from an import.
     * Omitted Boolean values are restored on the submitted entity before the inherited merge
     */
    @Override
    protected void copyEntityIntoOriginal(FieldsSettingsEntity entity, FieldsSettingsEntity original) {
        if (isImporting()) {
            Set<String> importedColumns = getImportedColumns();
            BeanWrapperImpl source = new BeanWrapperImpl(entity);
            BeanWrapperImpl stored = new BeanWrapperImpl(original);
            for (Field field : FieldsSettingsEntity.class.getDeclaredFields()) {
                if (field.getType() == Boolean.class && (importedColumns == null || importedColumns.contains(field.getName()) == false)) {
                    // Nullable radio fields must retain their stored values when omitted from an import.
                    source.setPropertyValue(field.getName(), stored.getPropertyValue(field.getName()));
                }
            }
        }
        super.copyEntityIntoOriginal(entity, original);
    }

    /**
     * Rejects self-referencing sort positions and duplicate class/field settings in the current domain.
     * Duplicate checks exclude the edited record and are left to the database constraint during imports.
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, FieldsSettingsEntity> target, Identity user, Errors errors, Long id, FieldsSettingsEntity entity) {
        super.validateEditor(request, target, user, errors, id, entity);
        if (errors.hasErrors() || (target.isInsert() == false && target.isUpdate() == false)) return;
        if (Tools.isEmpty(entity.getClassName()) || Tools.isEmpty(entity.getFieldName())) return;

        if (entity.getFieldName().equals(entity.getSortAfter())) {
            errors.rejectValue("errorField.sortAfter", null, getProp().getText("settings.fields-settings.sort_after_self-err"));
        }

        // Import request IDs are row indexes, so this check could flag the updated record itself as a duplicate.
        // The database unique constraint still rejects actual duplicates during import.
        if (isImporting()) return;

        Long excludedId = target.isUpdate() && id != null ? id : -1L;
        if (repository.existsByClassNameAndFieldNameAndDomainIdAndIdNot(entity.getClassName(), entity.getFieldName(), getDomainId(), excludedId)) {
            String message = getProp().getText("settings.fields-settings.duplicity-err");
            errors.rejectValue("errorField.className", null, message);
            errors.rejectValue("errorField.fieldName", null, message);
        }
    }

    @GetMapping("/autocomplete-class")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return fieldsSettingsService.getClassOptions(term);
    }

    @GetMapping("/autocomplete-field")
    public List<LabelValue> getAutocompleteField(@RequestParam String term, @RequestParam("DTE_Field_className") String className) {
        return fieldsSettingsService.getFieldOptions(term, className, getProp());
    }

    @GetMapping("/autocomplete-tab")
    public List<LabelValue> getAutocompleteTab(@RequestParam String term, @RequestParam("DTE_Field_className") String className) {
        return fieldsSettingsService.getTabOptions(term, className, getProp());
    }
}
