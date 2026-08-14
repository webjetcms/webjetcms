package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.ConditionType;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;

class FormsDuplicationServiceTest {

    private FormsRepository formsRepository;
    private FormSettingsRepository formSettingsRepository;
    private FormStepsRepository formStepsRepository;
    private FormItemsRepository formItemsRepository;
    private FormItemsConditionsRepository formItemsConditionsRepository;
    private FormsDuplicationService service;

    @BeforeEach
    void setUp() {
        formsRepository = mock(FormsRepository.class);
        formSettingsRepository = mock(FormSettingsRepository.class);
        formStepsRepository = mock(FormStepsRepository.class);
        formItemsRepository = mock(FormItemsRepository.class);
        formItemsConditionsRepository = mock(FormItemsConditionsRepository.class);
        service = new FormsDuplicationService(
            formsRepository,
            formSettingsRepository,
            formStepsRepository,
            formItemsRepository,
            formItemsConditionsRepository
        );
    }

    @Test
    void duplicatesConditionsUsingNewStepAndItemIdsWithoutMutatingSourceEntities() {
        FormsEntity original = new FormsEntity();
        original.setId(1L);
        original.setFormName("source-form");
        original.setFormType(FormsService.FORM_TYPE.MULTISTEP.value());
        original.setData("source-data");

        FormsEntity duplicate = new FormsEntity();
        duplicate.setFormName("duplicate-form");
        FormSettingsEntity requestedSettings = new FormSettingsEntity();
        requestedSettings.setId(2L);
        requestedSettings.setFormName("source-form");
        requestedSettings.setRecipients("test@example.com");
        requestedSettings.setViewCount(123);
        requestedSettings.setResponseAttempts(45);
        duplicate.setFormSettings(requestedSettings);

        FormStepEntity firstStep = createStep(11L, 10);
        FormStepEntity secondStep = createStep(12L, 20);
        FormItemEntity controllingItem = createItem(21L, 11L, "contact-1");
        FormItemEntity conditionalItem = createItem(22L, 12L, "email-1");
        controllingItem.setErrorCount(6);
        conditionalItem.setErrorCount(7);

        FormItemsConditionEntity condition = new FormItemsConditionEntity();
        condition.setId(31L);
        condition.setFormName("source-form");
        condition.setFormItemId(22L);
        condition.setItemFormId("contact-1");
        condition.setConditionType(ConditionType.VISIBILITY);
        condition.setValue("yes");
        condition.setDomainId(1);

        when(formsRepository.findFirstByIdAndDomainId(1L, 1)).thenReturn(Optional.of(original));
        when(formStepsRepository.findAllByFormNameAndDomainIdOrderBySortPriorityAsc("source-form", 1))
            .thenReturn(List.of(firstStep, secondStep));
        when(formItemsRepository.findAllByFormNameAndDomainId("source-form", 1))
            .thenReturn(List.of(controllingItem, conditionalItem));
        when(formItemsConditionsRepository.findAllByFormItemIdInAndDomainIdOrderByFormItemIdAscSortPriorityAsc(eq(List.of(21L, 22L)), eq(1)))
            .thenReturn(List.of(condition));

        when(formsRepository.save(any(FormsEntity.class))).thenAnswer(invocation -> {
            FormsEntity saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(formSettingsRepository.save(any(FormSettingsEntity.class))).thenAnswer(invocation -> {
            FormSettingsEntity saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });
        List<FormStepEntity> savedStepCopies = new ArrayList<>();
        AtomicLong stepId = new AtomicLong(201L);
        when(formStepsRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<FormStepEntity> saved = invocation.getArgument(0);
            saved.forEach(step -> step.setId(stepId.getAndIncrement()));
            savedStepCopies.addAll(saved);
            return saved;
        });
        List<FormItemEntity> savedItemCopies = new ArrayList<>();
        AtomicLong itemId = new AtomicLong(301L);
        when(formItemsRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<FormItemEntity> saved = invocation.getArgument(0);
            saved.forEach(item -> item.setId(itemId.getAndIncrement()));
            savedItemCopies.addAll(saved);
            return saved;
        });

        FormsEntity savedForm = service.duplicateMultistepForm(duplicate, 1L, 1);

        assertEquals(101L, savedForm.getId());
        assertEquals("source-data", savedForm.getData());
        assertEquals(FormsService.FORM_TYPE.MULTISTEP.value(), savedForm.getFormType());

        ArgumentCaptor<FormSettingsEntity> settingsCaptor = ArgumentCaptor.forClass(FormSettingsEntity.class);
        verify(formSettingsRepository).save(settingsCaptor.capture());
        assertEquals("duplicate-form", settingsCaptor.getValue().getFormName());
        assertEquals("test@example.com", settingsCaptor.getValue().getRecipients());
        assertEquals(0, settingsCaptor.getValue().getViewCount());
        assertEquals(0, settingsCaptor.getValue().getResponseAttempts());
        assertNotSame(requestedSettings, settingsCaptor.getValue());

        verify(formStepsRepository).saveAllAndFlush(anyList());
        assertEquals(List.of(201L, 202L), savedStepCopies.stream().map(FormStepEntity::getId).toList());
        assertEquals(List.of("duplicate-form", "duplicate-form"), savedStepCopies.stream().map(FormStepEntity::getFormName).toList());

        verify(formItemsRepository).saveAllAndFlush(anyList());
        assertEquals(List.of(201L, 202L), savedItemCopies.stream().map(FormItemEntity::getStepId).toList());
        assertEquals(List.of(301L, 302L), savedItemCopies.stream().map(FormItemEntity::getId).toList());
        assertEquals(List.of(0, 0), savedItemCopies.stream().map(FormItemEntity::getErrorCount).toList());

        ArgumentCaptor<FormItemsConditionEntity> conditionCaptor = ArgumentCaptor.forClass(FormItemsConditionEntity.class);
        verify(formItemsConditionsRepository).save(conditionCaptor.capture());
        FormItemsConditionEntity savedCondition = conditionCaptor.getValue();
        assertEquals("duplicate-form", savedCondition.getFormName());
        assertEquals(302L, savedCondition.getFormItemId());
        assertEquals("contact-1", savedCondition.getItemFormId());
        assertEquals(ConditionType.VISIBILITY, savedCondition.getConditionType());
        assertEquals("yes", savedCondition.getValue());

        assertEquals(11L, firstStep.getId());
        assertEquals("source-form", firstStep.getFormName());
        assertEquals(21L, controllingItem.getId());
        assertEquals(11L, controllingItem.getStepId());
        assertEquals("source-form", controllingItem.getFormName());
        assertEquals(22L, condition.getFormItemId());
        assertEquals("source-form", condition.getFormName());
    }

    @Test
    void rejectsDuplicationOfNonMultistepFormBeforeWritingAnything() {
        FormsEntity original = new FormsEntity();
        original.setId(1L);
        original.setFormName("simple-form");
        original.setFormType(FormsService.FORM_TYPE.SIMPLE.value());

        FormsEntity duplicate = new FormsEntity();
        duplicate.setFormName("duplicate-form");

        when(formsRepository.findFirstByIdAndDomainId(1L, 1)).thenReturn(Optional.of(original));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.duplicateMultistepForm(duplicate, 1L, 1)
        );

        assertEquals("Form duplication failed: Only multistep forms can be duplicated.", exception.getMessage());
        verify(formsRepository, never()).save(any(FormsEntity.class));
        verifyNoInteractions(formSettingsRepository, formStepsRepository, formItemsRepository, formItemsConditionsRepository);
    }

    @Test
    void usesWebjetTransactionManager() throws NoSuchMethodException {
        Transactional transactional = FormsDuplicationService.class
            .getMethod("duplicateMultistepForm", FormsEntity.class, Long.class, int.class)
            .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals("webjet2022TransactionManager", transactional.transactionManager());
    }

    private FormStepEntity createStep(Long id, int sortPriority) {
        FormStepEntity step = new FormStepEntity();
        step.setId(id);
        step.setFormName("source-form");
        step.setSortPriority(sortPriority);
        step.setDomainId(1);
        return step;
    }

    private FormItemEntity createItem(Long id, Long stepId, String itemFormId) {
        FormItemEntity item = new FormItemEntity();
        item.setId(id);
        item.setStepId(stepId);
        item.setFormName("source-form");
        item.setItemFormId(itemFormId);
        item.setFieldType("text");
        item.setDomainId(1);
        return item;
    }
}
