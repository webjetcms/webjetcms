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

    private void copySettings(FormsEntity duplicate, String originalFormName, String duplicateFormName, int domainId) {
        FormSettingsEntity source = duplicate.getFormSettings();
        if (source == null) {
            source = formSettingsRepository.findByFormNameAndDomainId(originalFormName, domainId);
        }
        if (source == null) return;

        FormSettingsEntity copy = new FormSettingsEntity();
        BeanUtils.copyProperties(source, copy, "id", "formName", "domainId");
        copy.setId(null);
        copy.setFormName(duplicateFormName);
        copy.setDomainId(domainId);
        FormSettingsService.prepareSettingsForSave(copy, FormsService.FORM_TYPE.MULTISTEP.value(), formSettingsRepository);
        copy.setId(null);
        duplicate.setFormSettings(formSettingsRepository.save(copy));
    }

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
                "visibilityConditions",
                "requirementConditions"
            );
            copy.setId(null);
            copy.setStepId(duplicateStepId);
            copy.setFormName(duplicateFormName);
            copy.setDomainId(domainId);
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
