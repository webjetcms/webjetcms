package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;

class MultistepFormsServiceTest {

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
}
