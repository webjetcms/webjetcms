package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;

/**
 * Tests identifier generation and condition-field loading in {@link MultistepFormsService}.
 */
class MultistepFormsServiceTest {

    /**
     * Verifies that item form IDs remain unique across fields with different types.
     */
    @Test
    void generatesUniqueItemFormIdAcrossFieldTypes() {
        FormItemsRepository formItemsRepository = mock(FormItemsRepository.class);
        MultistepFormsService service = new MultistepFormsService(null, null, formItemsRepository, null, null);
        FormItemEntity emailField = new FormItemEntity();
        emailField.setFormName("contact-form");
        emailField.setFieldType("email");
        emailField.setLabel("Kontakt");

        when(formItemsRepository.getItemFormIds("contact-form", 1)).thenReturn(List.of("kontakt-1"));

        assertEquals("kontakt-2", service.getValidItemFormId(emailField));
        verify(formItemsRepository).getItemFormIds("contact-form", 1);
    }

    /**
     * Verifies that condition-field loading preserves step IDs larger than the integer range.
     */
    @Test
    void keepsLongStepIdsWhenLoadingConditionFields() {
        long stepId = (long) Integer.MAX_VALUE + 10L;
        FormItemsRepository formItemsRepository = mock(FormItemsRepository.class);
        FormStepsRepository formStepsRepository = mock(FormStepsRepository.class);
        Prop prop = mock(Prop.class);
        MultistepFormsService service = new MultistepFormsService(null, null, formItemsRepository, formStepsRepository, null);

        FormStepEntity step = new FormStepEntity();
        step.setId(stepId);
        step.setCurrentPosition(2);

        when(formStepsRepository.getStepsUpToPosition("contact-form", 2, 1)).thenReturn(List.of(step));
        when(formItemsRepository.findAllByFormNameAndStepIdInAndDomainId("contact-form", List.of(stepId), 1)).thenReturn(List.of());

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedConstruction<SimpleQuery> simpleQueries = mockConstruction(
                SimpleQuery.class,
                (query, context) -> when(query.forInt(
                    "SELECT current_position FROM form_steps WHERE id = ? AND domain_id = ?",
                    stepId,
                    1
                )).thenReturn(2)
            )
        ) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);

            assertEquals(List.of(), service.getAvailableConditionFields("contact-form", stepId, prop));
            verify(formItemsRepository).findAllByFormNameAndStepIdInAndDomainId("contact-form", List.of(stepId), 1);
            assertEquals(1, simpleQueries.constructed().size());
        }
    }
}
