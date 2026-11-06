package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.datatable.EditorException;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Tests condition cleanup after step deletion and deletion guards for orphaned and existing condition owners. */
@Execution(ExecutionMode.SAME_THREAD)
class FormConditionsDeletionTest {

    private static final int DOMAIN_ID = 7;
    private static final long STEP_ID = 10L;
    private static final String FORM_NAME = "condition-deletion-form";

    private FormItemsRepository items;
    private FormItemsConditionsRepository conditions;
    private MultistepFormsService service;
    private FormStepsRestController stepsController;
    private FormItemsRestController itemsController;
    private Prop prop;
    private MockedStatic<CloudToolsForCore> domains;

    @BeforeEach
    void setUp() {
        items = mock(FormItemsRepository.class);
        conditions = mock(FormItemsConditionsRepository.class);
        service = mock(MultistepFormsService.class);
        prop = mock(Prop.class);
        FormStepsRepository steps = mock(FormStepsRepository.class);
        FormsServiceImpl forms = mock(FormsServiceImpl.class);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<InitServlet> init = mockStatic(InitServlet.class)) {
            stepsController = new FormStepsRestController(steps, items, service, forms);
            itemsController = new FormItemsRestController(items, mock(RegExpRepository.class), service,
                    mock(FormSettingsRepository.class), conditions, steps, forms) {
                @Override
                public Prop getProp() {
                    return prop;
                }
            };
        }

        domains = mockStatic(CloudToolsForCore.class);
        domains.when(CloudToolsForCore::getDomainId).thenReturn(DOMAIN_ID);
        ReflectionTestUtils.invokeMethod(itemsController, "clearThreadData");
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.invokeMethod(itemsController, "clearThreadData");
        domains.close();
    }

    /** Verifies that deleting a populated step removes its owned conditions and both condition caches before its items. */
    @Test
    void deletesStepConditionsAndEvictsCachesBeforeDeletingItems() {
        FormStepEntity step = new FormStepEntity();
        step.setId(STEP_ID);
        step.setFormName(FORM_NAME);
        step.setDomainId(DOMAIN_ID);
        when(items.getAllStepItems(STEP_ID, DOMAIN_ID)).thenReturn(List.of(item(101L, "first"), item(102L, "second")));
        Cache cache = mock(Cache.class);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
                MockedStatic<Cache> caches = mockStatic(Cache.class);
                MockedStatic<ClusterDB> clusters = mockStatic(ClusterDB.class)) {
            tools.when(() -> Tools.getSpringBean("formItemsConditionsRepository", FormItemsConditionsRepository.class))
                .thenReturn(conditions);
            caches.when(Cache::getInstance).thenReturn(cache);

            stepsController.afterDelete(step, STEP_ID);

            verify(items).getAllStepItems(STEP_ID, DOMAIN_ID);
            var order = inOrder(conditions, cache, items);
            order.verify(conditions).deleteAllByFormItemIdInAndDomainId(List.of(101L, 102L), DOMAIN_ID);
            for (long ownerId : List.of(101L, 102L)) {
                order.verify(cache).removeObject("multistep_form.conditions." + DOMAIN_ID + "." + ownerId + ".VISIBILITY", true);
                order.verify(cache).removeObject("multistep_form.conditions." + DOMAIN_ID + "." + ownerId + ".REQUIREMENT", true);
            }
            order.verify(items).deleteAllByStepIdAndDomainId(STEP_ID, DOMAIN_ID);
            order.verify(cache).removeObject(startsWith("multistep_form.validationFields." + DOMAIN_ID + "."), eq(false));
            verifyNoMoreInteractions(conditions, cache, items);
            verify(service).updateFormPattern(FORM_NAME);
            verify(service).updateStepsPositions(FORM_NAME);
            clusters.verify(() -> ClusterDB.addRefresh(MultistepFormsService.class, (long) DOMAIN_ID));
        }
    }

    /** Verifies that conditions belonging only to missing items do not block deleting their referenced field. */
    @Test
    void permitsDeletionWhenAllConditionOwnersAreOrphaned() {
        FormItemEntity referenced = item(101L, "referenced");
        when(conditions.getDependedItems(FORM_NAME, "referenced", DOMAIN_ID)).thenReturn(List.of(999L));
        when(items.findAllByFormNameAndIdInAndDomainId(FORM_NAME, List.of(999L), DOMAIN_ID)).thenReturn(List.of());

        assertTrue(itemsController.beforeDelete(referenced));

        verify(conditions).getDependedItems(FORM_NAME, "referenced", DOMAIN_ID);
        verify(items).findAllByFormNameAndIdInAndDomainId(FORM_NAME, List.of(999L), DOMAIN_ID);
        verifyNoMoreInteractions(conditions, items);
        verifyNoInteractions(service, prop);
    }

    /** Verifies that an existing condition owner still blocks deletion when orphaned owners are also returned. */
    @Test
    void rejectsDeletionWhenAnExistingConditionOwnerRemains() {
        FormItemEntity referenced = item(101L, "referenced");
        FormItemEntity dependent = item(102L, "dependent");
        when(conditions.getDependedItems(FORM_NAME, "referenced", DOMAIN_ID)).thenReturn(List.of(999L, 102L));
        when(items.findAllByFormNameAndIdInAndDomainId(FORM_NAME, List.of(999L, 102L), DOMAIN_ID))
            .thenReturn(List.of(dependent));
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        when(service.prepareConditionFieldOptions(List.of(dependent), FORM_NAME, prop))
            .thenReturn(List.of(new LabelValue("Dependent field", "dependent")));

        EditorException error = assertThrows(EditorException.class, () -> itemsController.beforeDelete(referenced));

        assertEquals("components.form_items.bind_condition.delete_err", error.getMessage());
        assertEquals(1, error.getNotifyBeans().size());
        assertTrue(error.getNotifyBeans().get(0).getText().contains("Dependent field"));
        verify(conditions).getDependedItems(FORM_NAME, "referenced", DOMAIN_ID);
        verify(items).findAllByFormNameAndIdInAndDomainId(FORM_NAME, List.of(999L, 102L), DOMAIN_ID);
        verifyNoMoreInteractions(conditions, items);
    }

    private static FormItemEntity item(long id, String itemFormId) {
        FormItemEntity item = new FormItemEntity();
        item.setId(id);
        item.setFormName(FORM_NAME);
        item.setStepId(STEP_ID);
        item.setDomainId(DOMAIN_ID);
        item.setItemFormId(itemFormId);
        return item;
    }
}
