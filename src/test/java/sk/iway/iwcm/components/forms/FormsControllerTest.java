package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;

class FormsControllerTest {

    private static final String ORIGINAL_FORM_ID_ATTRIBUTE = FormsController.class.getName() + ".originalFormId";

    @Test
    void rejectsDuplicationWhenUserCannotAccessSourceForm() {
        FormsServiceImpl formsService = mock(FormsServiceImpl.class);
        Identity restrictedUser = new Identity();
        restrictedUser.setEditableGroups("10");
        restrictedUser.setEditablePages("20");

        FormsEntity original = createOriginalForm(42L, "restricted-form");
        when(formsService.getById(42L)).thenReturn(original);
        when(formsService.isFormAccessible("restricted-form", restrictedUser)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        FormsController controller = createController(formsService, restrictedUser, request);

        assertThrows(AccessDeniedException.class, () -> controller.beforeDuplicate(new FormsEntity(), 42L));
        assertNull(request.getAttribute(ORIGINAL_FORM_ID_ATTRIBUTE));
        verify(formsService).isFormAccessible("restricted-form", restrictedUser);
    }

    @Test
    void storesSourceIdWhenUserCanAccessSourceForm() {
        FormsServiceImpl formsService = mock(FormsServiceImpl.class);
        Identity user = new Identity();

        FormsEntity original = createOriginalForm(42L, "accessible-form");
        when(formsService.getById(42L)).thenReturn(original);
        when(formsService.isFormAccessible("accessible-form", user)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        FormsController controller = createController(formsService, user, request);

        controller.beforeDuplicate(new FormsEntity(), 42L);

        assertEquals(42L, request.getAttribute(ORIGINAL_FORM_ID_ATTRIBUTE));
        verify(formsService).isFormAccessible("accessible-form", user);
    }

    private static FormsEntity createOriginalForm(Long id, String formName) {
        FormsEntity original = new FormsEntity();
        original.setId(id);
        original.setFormName(formName);
        return original;
    }

    private static FormsController createController(FormsServiceImpl formsService, Identity user, MockHttpServletRequest request) {
        FormsController controller = new FormsController(
            mock(FormsRepository.class),
            formsService,
            mock(FormsDuplicationService.class),
            mock(FormSettingsRepository.class),
            mock(FormStepsRepository.class),
            mock(FormItemsRepository.class)
        ) {
            @Override
            public Identity getUser() {
                return user;
            }
        };
        controller.setRequest(request);
        return controller;
    }
}
