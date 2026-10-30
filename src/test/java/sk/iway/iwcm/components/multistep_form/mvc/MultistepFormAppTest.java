package sk.iway.iwcm.components.multistep_form.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/** Verifies public rendering of the multistep form application. */
class MultistepFormAppTest {

    /** A failed statistics update must not prevent the form from being rendered. */
    @Test
    void rendersWhenViewCounterCannotBePersisted() {
        FormStepsRepository steps = mock(FormStepsRepository.class);
        FormSettingsRepository settings = mock(FormSettingsRepository.class);
        FormsRepository forms = mock(FormsRepository.class);
        MultistepFormApp app = new MultistepFormApp(steps, settings, forms);
        app.setFormName("contact-form");
        when(steps.getFirstStepId("contact-form", 1)).thenReturn(Optional.of(7L));
        when(settings.incrementFormViewCount("contact-form", 1))
            .thenThrow(new DataAccessResourceFailureException("form_settings is read-only"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("docid", "42");
        ExtendedModelMap model = new ExtendedModelMap();

        try (
            MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
            MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
            MockedStatic<CSRF> csrf = mockStatic(CSRF.class);
            MockedStatic<PageLng> languages = mockStatic(PageLng.class);
            MockedStatic<ClusterDB> cluster = mockStatic(ClusterDB.class);
            MockedStatic<Logger> logs = mockStatic(Logger.class)
        ) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            cluster.when(ClusterDB::isPublicNode).thenReturn(false);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(null);
            csrf.when(() -> CSRF.getCsrfToken(request.getSession(), true)).thenReturn("csrf-token");
            languages.when(() -> PageLng.getUserLng(request)).thenReturn("sk");

            assertEquals("/apps/form/mvc/multistep-form", app.view(model, request));
        }

        assertEquals("contact-form", model.get("formName"));
        assertEquals(7L, model.get("stepId"));
        assertEquals("csrf-token", model.get("csrf"));
        verify(settings).incrementFormViewCount("contact-form", 1);
    }
}
