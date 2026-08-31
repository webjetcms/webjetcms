package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.datatable.RowReorderDto;

@Execution(ExecutionMode.SAME_THREAD)
class FormRowReorderScopeTest {

    private static final int DOMAIN_ID = 1;
    private static final int STEP_ID = 10;
    private static final String FORM_A = "form-a";
    private static final String FORM_B = "form-b";

    private FormStepsRepository formStepsRepository;
    private FormItemsRepository formItemsRepository;
    private MultistepFormsService multistepFormsService;
    private FormsServiceImpl formsService;
    private Identity user;

    private TestFormStepsRestController stepsController;
    private TestFormItemsRestController itemsController;

    @BeforeEach
    void setUp() {
        formStepsRepository = mock(FormStepsRepository.class);
        formItemsRepository = mock(FormItemsRepository.class);
        multistepFormsService = mock(MultistepFormsService.class);
        formsService = mock(FormsServiceImpl.class);
        user = mock(Identity.class);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);

            stepsController = new TestFormStepsRestController(
                formStepsRepository,
                formItemsRepository,
                multistepFormsService,
                formsService,
                user
            );
            itemsController = new TestFormItemsRestController(
                formItemsRepository,
                mock(RegExpRepository.class),
                multistepFormsService,
                mock(FormSettingsRepository.class),
                formStepsRepository,
                formsService,
                user
            );

            stepsController.setRequest(new MockHttpServletRequest());
            itemsController.setRequest(new MockHttpServletRequest());
        }
    }

    @Test
    void stepsRejectMixedFormWithoutSavingOrRecalculating() {
        FormStepEntity first = step(1L, FORM_A, 10);
        FormStepEntity second = step(2L, FORM_B, 20);
        when(formStepsRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.FALSE,
                stepsController.rowReorder(request(FORM_A, null), reorder(1L, 2L)).getBody());
        }

        assertEquals(10, first.getSortPriority());
        assertEquals(20, second.getSortPriority());
        verify(formStepsRepository, never()).saveAll(any());
        verify(multistepFormsService, never()).updateStepsPositions(anyString());
    }

    @Test
    void stepsValidScopeAndAclSavesAndRecalculatesValidatedForm() {
        FormStepEntity first = step(1L, FORM_A, 10);
        FormStepEntity second = step(2L, FORM_A, 20);
        List<FormStepEntity> entities = List.of(first, second);
        when(formStepsRepository.findAllById(List.of(1L, 2L))).thenReturn(entities);
        when(formStepsRepository.findFirstByIdAndDomainId(1L, DOMAIN_ID)).thenReturn(Optional.of(first));
        when(formStepsRepository.findFirstByIdAndDomainId(2L, DOMAIN_ID)).thenReturn(Optional.of(second));
        when(formsService.isFormAccessible(FORM_A, user)).thenReturn(true);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.TRUE,
                stepsController.rowReorder(request(FORM_A, null), reorder(1L, 2L)).getBody());
        }

        assertEquals(20, first.getSortPriority());
        assertEquals(10, second.getSortPriority());
        verify(formStepsRepository).saveAll(entities);
        verify(multistepFormsService).updateStepsPositions(FORM_A);
        verify(multistepFormsService, never()).updateStepsPositions(FORM_B);
    }

    @Test
    void stepsRejectHomogeneousInaccessibleFormWithoutSavingOrRecalculating() {
        FormStepEntity first = step(1L, FORM_B, 10);
        FormStepEntity second = step(2L, FORM_B, 20);
        when(formStepsRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(formsService.isFormAccessible(FORM_B, user)).thenReturn(false);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.FALSE,
                stepsController.rowReorder(request(FORM_B, null), reorder(1L, 2L)).getBody());
        }

        assertEquals(10, first.getSortPriority());
        assertEquals(20, second.getSortPriority());
        verify(formsService).isFormAccessible(FORM_B, user);
        verify(formStepsRepository, never()).saveAll(any());
        verify(multistepFormsService, never()).updateStepsPositions(anyString());
    }

    @Test
    void itemsRejectMixedFormWithoutSavingOrUpdatingPattern() {
        FormItemEntity first = item(1L, FORM_A, STEP_ID, 10);
        FormItemEntity second = item(2L, FORM_B, STEP_ID, 20);
        when(formItemsRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.FALSE,
                itemsController.rowReorder(request(FORM_A, STEP_ID), reorder(1L, 2L)).getBody());
        }

        assertEquals(10, first.getSortPriority());
        assertEquals(20, second.getSortPriority());
        verify(formItemsRepository, never()).saveAll(any());
        verify(multistepFormsService, never()).updateFormPattern(anyString());
    }

    @Test
    void itemsRejectMixedStepWithoutSavingOrUpdatingPattern() {
        FormItemEntity first = item(1L, FORM_A, STEP_ID, 10);
        FormItemEntity second = item(2L, FORM_A, STEP_ID + 1, 20);
        when(formItemsRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.FALSE,
                itemsController.rowReorder(request(FORM_A, STEP_ID), reorder(1L, 2L)).getBody());
        }

        assertEquals(10, first.getSortPriority());
        assertEquals(20, second.getSortPriority());
        verify(formItemsRepository, never()).saveAll(any());
        verify(multistepFormsService, never()).updateFormPattern(anyString());
    }

    @Test
    void itemsValidScopeAndAclSavesBatch() {
        FormItemEntity first = item(1L, FORM_A, STEP_ID, 10);
        FormItemEntity second = item(2L, FORM_A, STEP_ID, 20);
        List<FormItemEntity> entities = List.of(first, second);
        FormStepEntity validStep = step((long) STEP_ID, FORM_A, 10);
        when(formItemsRepository.findAllById(List.of(1L, 2L))).thenReturn(entities);
        when(formItemsRepository.findFirstByIdAndDomainId(1L, DOMAIN_ID)).thenReturn(Optional.of(first));
        when(formItemsRepository.findFirstByIdAndDomainId(2L, DOMAIN_ID)).thenReturn(Optional.of(second));
        when(formStepsRepository.getValidStep(FORM_A, (long) STEP_ID, DOMAIN_ID))
            .thenReturn(Optional.of(validStep));
        when(formsService.isFormAccessible(FORM_A, user)).thenReturn(true);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.TRUE,
                itemsController.rowReorder(request(FORM_A, STEP_ID), reorder(1L, 2L)).getBody());
        }

        assertEquals(20, first.getSortPriority());
        assertEquals(10, second.getSortPriority());
        verify(formItemsRepository).saveAll(entities);
        verify(multistepFormsService, never()).updateFormPattern(anyString());
    }

    @Test
    void itemsRejectHomogeneousInaccessibleFormWithoutSavingOrUpdatingPattern() {
        FormItemEntity first = item(1L, FORM_B, STEP_ID, 10);
        FormItemEntity second = item(2L, FORM_B, STEP_ID, 20);
        FormStepEntity validStep = step((long) STEP_ID, FORM_B, 10);
        when(formItemsRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(formStepsRepository.getValidStep(FORM_B, (long) STEP_ID, DOMAIN_ID))
            .thenReturn(Optional.of(validStep));
        when(formsService.isFormAccessible(FORM_B, user)).thenReturn(false);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertEquals(Boolean.FALSE,
                itemsController.rowReorder(request(FORM_B, STEP_ID), reorder(1L, 2L)).getBody());
        }

        assertEquals(10, first.getSortPriority());
        assertEquals(20, second.getSortPriority());
        verify(formsService).isFormAccessible(FORM_B, user);
        verify(formItemsRepository, never()).saveAll(any());
        verify(multistepFormsService, never()).updateFormPattern(anyString());
    }

    @Test
    void stepsCheckItemPermsRejectsForgedFormForPersistedTarget() {
        FormStepEntity submitted = step(91L, FORM_A, 10);
        FormStepEntity persisted = step(91L, FORM_B, 10);
        when(formStepsRepository.findFirstByIdAndDomainId(91L, DOMAIN_ID)).thenReturn(Optional.of(persisted));
        when(formsService.isFormAccessible(FORM_A, user)).thenReturn(true);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertFalse(stepsController.checkItemPerms(submitted, 91L));
        }

        verify(formStepsRepository).findFirstByIdAndDomainId(91L, DOMAIN_ID);
        verify(formsService, never()).isFormAccessible(anyString(), any());
    }

    @Test
    void stepsCheckItemPermsRejectsPersistedInaccessibleForm() {
        FormStepEntity submitted = step(93L, FORM_B, 10);
        FormStepEntity persisted = step(93L, FORM_B, 10);
        when(formStepsRepository.findFirstByIdAndDomainId(93L, DOMAIN_ID)).thenReturn(Optional.of(persisted));
        when(formsService.isFormAccessible(FORM_B, user)).thenReturn(false);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertFalse(stepsController.checkItemPerms(submitted, 93L));
        }

        verify(formStepsRepository).findFirstByIdAndDomainId(93L, DOMAIN_ID);
        verify(formsService).isFormAccessible(FORM_B, user);
    }

    @Test
    void stepsRejectCrossFormDuplicateBeforeItemSideEffects() {
        long sourceId = 95L;
        FormStepEntity submitted = step(96L, FORM_A, 10);
        submitted.setIdForDuplication(sourceId);
        FormStepEntity source = step(sourceId, FORM_B, 10);
        when(formStepsRepository.findFirstByIdAndDomainId(sourceId, DOMAIN_ID)).thenReturn(Optional.of(source));

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertThrows(ConstraintViolationException.class, () -> stepsController.beforeDuplicate(submitted));
        }

        verifyNoDuplicateItemSideEffects();
    }

    @Test
    void stepsRejectInaccessibleDuplicateBeforeItemSideEffects() {
        long sourceId = 97L;
        FormStepEntity submitted = step(98L, FORM_B, 10);
        submitted.setIdForDuplication(sourceId);
        FormStepEntity source = step(sourceId, FORM_B, 10);
        when(formStepsRepository.findFirstByIdAndDomainId(sourceId, DOMAIN_ID)).thenReturn(Optional.of(source));
        when(formsService.isFormAccessible(FORM_B, user)).thenReturn(false);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertThrows(ConstraintViolationException.class, () -> stepsController.beforeDuplicate(submitted));
        }

        verifyNoDuplicateItemSideEffects();
    }

    @Test
    void stepsAllowAccessibleSameFormDuplicateItemFlow() {
        long sourceId = 99L;
        int userId = 7;
        FormStepEntity submitted = step(100L, FORM_A, 10);
        submitted.setIdForDuplication(sourceId);
        FormStepEntity source = step(sourceId, FORM_A, 10);
        FormItemEntity sourceItem = item(101L, FORM_A, (int) sourceId, 10);
        sourceItem.setItemFormId("source-item");
        List<FormItemEntity> sourceItems = List.of(sourceItem);
        when(formStepsRepository.findFirstByIdAndDomainId(sourceId, DOMAIN_ID)).thenReturn(Optional.of(source));
        when(formsService.isFormAccessible(FORM_A, user)).thenReturn(true);
        when(user.getUserId()).thenReturn(userId);
        when(formItemsRepository.getAllStepItems(sourceId, DOMAIN_ID)).thenReturn(sourceItems);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            stepsController.beforeDuplicate(submitted);
        }

        verify(formItemsRepository).deleteAllByFormNameAndStepIdAndDomainId(FORM_A, Long.valueOf(-userId), DOMAIN_ID);
        verify(formItemsRepository).getAllStepItems(sourceId, DOMAIN_ID);
        verify(formItemsRepository).saveAll(sourceItems);
        assertNull(sourceItem.getId());
        assertEquals(-userId, sourceItem.getStepId());
        assertEquals("", sourceItem.getItemFormId());
    }

    @Test
    void itemsCheckItemPermsRejectsForgedFormForPersistedTarget() {
        FormItemEntity submitted = item(92L, FORM_A, STEP_ID, 10);
        FormItemEntity persisted = item(92L, FORM_B, STEP_ID, 10);
        when(formItemsRepository.findFirstByIdAndDomainId(92L, DOMAIN_ID)).thenReturn(Optional.of(persisted));
        when(formsService.isFormAccessible(FORM_A, user)).thenReturn(true);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertFalse(itemsController.checkItemPerms(submitted, 92L));
        }

        verify(formItemsRepository).findFirstByIdAndDomainId(92L, DOMAIN_ID);
        verify(formsService, never()).isFormAccessible(anyString(), any());
    }

    @Test
    void itemsCheckItemPermsRejectsPersistedInaccessibleForm() {
        FormItemEntity submitted = item(94L, FORM_B, STEP_ID, 10);
        FormItemEntity persisted = item(94L, FORM_B, STEP_ID, 10);
        FormStepEntity validStep = step((long) STEP_ID, FORM_B, 10);
        when(formItemsRepository.findFirstByIdAndDomainId(94L, DOMAIN_ID)).thenReturn(Optional.of(persisted));
        when(formStepsRepository.getValidStep(FORM_B, (long) STEP_ID, DOMAIN_ID))
            .thenReturn(Optional.of(validStep));
        when(formsService.isFormAccessible(FORM_B, user)).thenReturn(false);

        try (MockedStatic<CloudToolsForCore> cloudTools = currentDomain()) {
            assertFalse(itemsController.checkItemPerms(submitted, 94L));
        }

        verify(formItemsRepository).findFirstByIdAndDomainId(94L, DOMAIN_ID);
        verify(formsService).isFormAccessible(FORM_B, user);
    }

    private static MockedStatic<CloudToolsForCore> currentDomain() {
        MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
        cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(DOMAIN_ID);
        return cloudTools;
    }

    private static MockHttpServletRequest request(String formName, Integer stepId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("formName", formName);
        if(stepId != null) request.setParameter("stepId", String.valueOf(stepId));
        return request;
    }

    private static RowReorderDto reorder(long firstId, long secondId) {
        RowReorderDto dto = new RowReorderDto();
        dto.setDataSrc("sortPriority");
        dto.setValues(List.of(
            new RowReorderDto.RowReorderValue(firstId, 10, 20),
            new RowReorderDto.RowReorderValue(secondId, 20, 10)
        ));
        return dto;
    }

    private static FormStepEntity step(long id, String formName, int sortPriority) {
        FormStepEntity entity = new FormStepEntity();
        entity.setId(id);
        entity.setFormName(formName);
        entity.setSortPriority(sortPriority);
        entity.setDomainId(DOMAIN_ID);
        return entity;
    }

    private static FormItemEntity item(long id, String formName, int stepId, int sortPriority) {
        FormItemEntity entity = new FormItemEntity();
        entity.setId(id);
        entity.setFormName(formName);
        entity.setStepId(stepId);
        entity.setSortPriority(sortPriority);
        entity.setDomainId(DOMAIN_ID);
        return entity;
    }

    private void verifyNoDuplicateItemSideEffects() {
        verify(formItemsRepository, never()).deleteAllByFormNameAndStepIdAndDomainId(anyString(), any(), any());
        verify(formItemsRepository, never()).getAllStepItems(any(), any());
        verify(formItemsRepository, never()).saveAll(any());
    }

    private static final class TestFormStepsRestController extends FormStepsRestController {

        private final Identity user;

        private TestFormStepsRestController(FormStepsRepository formStepsRepository,
                FormItemsRepository formItemsRepository, MultistepFormsService multistepFormsService,
                FormsServiceImpl formsService, Identity user) {
            super(formStepsRepository, formItemsRepository, multistepFormsService, formsService);
            this.user = user;
        }

        @Override
        public Identity getUser() {
            return user;
        }

        @Override
        public void throwConstraintViolation(String errorKey) {
            throw new ConstraintViolationException(errorKey, null);
        }
    }

    private static final class TestFormItemsRestController extends FormItemsRestController {

        private final Identity user;

        private TestFormItemsRestController(FormItemsRepository formItemsRepository,
                RegExpRepository regExpRepository, MultistepFormsService multistepFormsService,
                FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository,
                FormsServiceImpl formsService, Identity user) {
            super(formItemsRepository, regExpRepository, multistepFormsService, formSettingsRepository,
                formStepsRepository, formsService);
            this.user = user;
        }

        @Override
        public Identity getUser() {
            return user;
        }
    }
}
