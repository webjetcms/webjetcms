package sk.iway.iwcm.components.forms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;

/**
 * Duplicates multistep forms together with their settings, steps, items, and item conditions.
 *
 * The service preserves relationships between copied records by mapping original step and item IDs
 * to their newly generated IDs. Duplication is performed in a single transaction.
 */
@Service
public class FormsDuplicationService {

    private final FormsRepository formsRepository;
    private final FormSettingsRepository formSettingsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;
    private final FormItemsConditionsRepository formItemsConditionsRepository;

    @Autowired
    public FormsDuplicationService(
        FormsRepository formsRepository,
        FormSettingsRepository formSettingsRepository,
        FormStepsRepository formStepsRepository,
        FormItemsRepository formItemsRepository,
        FormItemsConditionsRepository formItemsConditionsRepository
    ) {
        this.formsRepository = formsRepository;
        this.formSettingsRepository = formSettingsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
        this.formItemsConditionsRepository = formItemsConditionsRepository;
    }

    /**
     * Creates a copy of a multistep form and all records that define its structure and behavior.
     *
     * The supplied form provides the new form name and may provide replacement settings. Serialized
     * form data is copied from the persisted original, while counters and database identifiers are reset.
     *
     * @param duplicate  form entity to populate and persist as the copy
     * @param originalId  database identifier of the form to copy
     * @param domainId  domain in which the original is resolved and the copy is created
     * @return persisted form entity representing the copy
     * @throws IllegalStateException if the input is incomplete, the original is not a multistep form,
     *                               or copied relationships cannot be reconstructed
     */
    @Transactional(transactionManager = "webjet2022TransactionManager")
    public FormsEntity duplicateMultistepForm(FormsEntity duplicate, Long originalId, int domainId) {
        if (duplicate == null || Tools.isEmpty(duplicate.getFormName())) {
            throw duplicationError("New form or formName is not present.");
        }
        if (originalId == null) throw duplicationError("Original form ID is not present.");

        FormsEntity original = formsRepository.findFirstByIdAndDomainId(originalId, domainId).orElse(null);
        if (original == null || Tools.isEmpty(original.getFormName())) {
            throw duplicationError("Original form or formName is not present.");
        }
        if (FormsService.FORM_TYPE.MULTISTEP.value().equals(original.getFormType()) == false) {
            throw duplicationError("Only multistep forms can be duplicated.");
        }

        String originalFormName = original.getFormName();
        String duplicateFormName = duplicate.getFormName();

        duplicate.setId(null);
        duplicate.setData(original.getData());
        duplicate.setFormType(FormsService.FORM_TYPE.MULTISTEP.value());
        duplicate.setDomainId(domainId);
        FormsEntity savedForm = formsRepository.save(duplicate);

        copySettings(duplicate, originalFormName, duplicateFormName, domainId);
        Map<Long, Long> stepIds = copySteps(originalFormName, duplicateFormName, domainId);
        Map<Long, Long> itemIds = copyItems(originalFormName, duplicateFormName, domainId, stepIds);
        copyConditions(duplicateFormName, domainId, itemIds);

        return savedForm;
    }

    /**
     * Copies the form settings and resets usage counters for the new form.
     *
     * Settings supplied with the duplicate take precedence over settings loaded from the original form.
     *
     * @param duplicate  form entity that receives the persisted settings copy
     * @param originalFormName  name used to load the original settings when no replacement is supplied
     * @param duplicateFormName  name assigned to the copied settings
     * @param domainId  domain assigned to the copied settings
     */
    private void copySettings(FormsEntity duplicate, String originalFormName, String duplicateFormName, int domainId) {
        FormSettingsEntity source = duplicate.getFormSettings();
        if (source == null) {
            source = formSettingsRepository.findByFormNameAndDomainId(originalFormName, domainId);
        }
        if (source == null) return;

        FormSettingsEntity copy = new FormSettingsEntity();
        BeanUtils.copyProperties(source, copy, "id", "formName", "domainId", "viewCount", "responseAttempts");
        copy.setId(null);
        copy.setFormName(duplicateFormName);
        copy.setDomainId(domainId);
        copy.setViewCount(0);
        copy.setResponseAttempts(0);
        FormSettingsService.prepareSettingsForSave(copy, FormsService.FORM_TYPE.MULTISTEP.value(), formSettingsRepository);
        copy.setId(null);
        duplicate.setFormSettings(formSettingsRepository.save(copy));
    }

    /**
     * Copies the ordered form steps and maps each original step ID to its generated copy ID.
     *
     * @param originalFormName  name of the form whose steps are copied
     * @param duplicateFormName  name assigned to the copied steps
     * @param domainId  domain used to load and persist the steps
     * @return mapping from original step IDs to copied step IDs
     * @throws IllegalStateException if a source or copied step has no ID, or persistence changes the
     *                               step count
     */
    private Map<Long, Long> copySteps(String originalFormName, String duplicateFormName, int domainId) {
        List<FormStepEntity> sourceSteps = formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc(originalFormName, domainId);
        Map<Long, Long> stepIds = new LinkedHashMap<>();
        List<FormStepEntity> copies = new ArrayList<>();

        for (FormStepEntity source : sourceSteps) {
            if (source.getId() == null) throw duplicationError("Original step ID is not present.");

            FormStepEntity copy = new FormStepEntity();
            BeanUtils.copyProperties(source, copy, "id", "formName", "domainId", "idForDuplication");
            copy.setId(null);
            copy.setFormName(duplicateFormName);
            copy.setDomainId(domainId);
            copies.add(copy);
        }

        List<FormStepEntity> savedSteps = formStepsRepository.saveAllAndFlush(copies);
        if (savedSteps.size() != sourceSteps.size()) {
            throw duplicationError("Duplicated steps do not have the same size as the original.");
        }

        for (int i = 0; i < sourceSteps.size(); i++) {
            FormStepEntity source = sourceSteps.get(i);
            FormStepEntity saved = savedSteps.get(i);
            if (saved == null || saved.getId() == null) throw duplicationError("Duplicated step ID is not present.");
            stepIds.put(source.getId(), saved.getId());
        }

        return stepIds;
    }

    /**
     * Copies form items and reconnects them to the corresponding copied steps.
     *
     * @param originalFormName  name of the form whose items are copied
     * @param duplicateFormName  name assigned to the copied items
     * @param domainId  domain used to load and persist the items
     * @param stepIds  mapping from original step IDs to copied step IDs
     * @return mapping from original item IDs to copied item IDs
     * @throws IllegalStateException if an item ID or mapped step ID is unavailable, or persistence changes
     *                               the item count
     */
    private Map<Long, Long> copyItems(
        String originalFormName,
        String duplicateFormName,
        int domainId,
        Map<Long, Long> stepIds
    ) {
        List<FormItemEntity> sourceItems = formItemsRepository.findAllByFormNameAndDomainId(originalFormName, domainId);
        Map<Long, Long> itemIds = new LinkedHashMap<>();
        List<FormItemEntity> copies = new ArrayList<>();

        for (FormItemEntity source : sourceItems) {
            if (source.getId() == null) throw duplicationError("Original item ID is not present.");

            Long duplicateStepId = stepIds.get(source.getStepId());
            if (duplicateStepId == null) {
                throw duplicationError("Unknown step ID while duplicating items: " + source.getStepId());
            }

            FormItemEntity copy = new FormItemEntity();
            BeanUtils.copyProperties(
                source,
                copy,
                "id",
                "stepId",
                "formName",
                "domainId",
                "errorCount",
                "visibilityConditions",
                "requirementConditions"
            );
            copy.setId(null);
            copy.setStepId(duplicateStepId);
            copy.setFormName(duplicateFormName);
            copy.setDomainId(domainId);
            copy.setErrorCount(0);
            copies.add(copy);
        }

        List<FormItemEntity> savedItems = formItemsRepository.saveAllAndFlush(copies);
        if (savedItems.size() != sourceItems.size()) {
            throw duplicationError("Duplicated items do not have the same size as the original.");
        }

        for (int i = 0; i < sourceItems.size(); i++) {
            FormItemEntity source = sourceItems.get(i);
            FormItemEntity saved = savedItems.get(i);
            if (saved == null || saved.getId() == null) throw duplicationError("Duplicated item ID is not present.");
            itemIds.put(source.getId(), saved.getId());
        }

        return itemIds;
    }

    /**
     * Copies item conditions and reconnects them to the corresponding copied items.
     *
     * @param duplicateFormName  name assigned to the copied conditions
     * @param domainId  domain used to load and persist the conditions
     * @param itemIds  mapping from original item IDs to copied item IDs
     * @throws IllegalStateException if a condition references an item without a copied ID
     */
    private void copyConditions(String duplicateFormName, int domainId, Map<Long, Long> itemIds) {
        if (itemIds.isEmpty()) return;

        List<FormItemsConditionEntity> sourceConditions = formItemsConditionsRepository
            .findAllByFormItemIdInAndDomainIdOrderByFormItemIdAscSortPriorityAsc(List.copyOf(itemIds.keySet()), domainId);

        for (FormItemsConditionEntity source : sourceConditions) {
            Long duplicateItemId = itemIds.get(source.getFormItemId());
            if (duplicateItemId == null) {
                throw duplicationError("Unknown item ID while duplicating conditions: " + source.getFormItemId());
            }

            FormItemsConditionEntity copy = new FormItemsConditionEntity();
            BeanUtils.copyProperties(source, copy, "id", "formName", "formItemId", "domainId", "infolabel");
            copy.setId(null);
            copy.setFormName(duplicateFormName);
            copy.setFormItemId(duplicateItemId);
            copy.setDomainId(domainId);
            formItemsConditionsRepository.save(copy);
        }
    }

    private IllegalStateException duplicationError(String debugError) {
        Logger.debug(this, debugError);
        return new IllegalStateException("Form duplication failed: " + debugError);
    }
}
