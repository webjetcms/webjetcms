package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;

/** Verifies inline error pages and redirect fallback after a failed form submission. */
class MultistepFormForwardTest {

    /** Keeps the error status and message while choosing inline content or the configured redirect. */
    @ParameterizedTest
    @CsvSource({
        "true, true, /error.html",
        "false, true, /error.html",
        ", true, /error.html",
        "true, false, /error.html",
        "true, true,"
    })
    void handlesFailedSubmission(Boolean forwardAsContent, boolean pageAvailable, String errorRedirect) throws Exception {
        FormSettingsRepository settingsRepository = mock(FormSettingsRepository.class);
        FormSettingsEntity settings = new FormSettingsEntity();
        settings.setForwardAsContent(forwardAsContent);
        settings.setForward("/success.html");
        settings.setForwardFail(errorRedirect);
        when(settingsRepository.findByFormNameAndDomainId("contact", 1)).thenReturn(settings);
        MultistepFormsService service = spy(new MultistepFormsService(null, null, null, null, settingsRepository));
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new SaveFormException("Email delivery failed.", false, errorRedirect))
            .when(service).saveFormStep(eq("contact"), eq(1L), eq(request), any(JSONObject.class));

        DocDB docDB = mock(DocDB.class);
        DocDetails page = new DocDetails();
        page.setAvailable(pageAvailable);
        page.setGroupId(10);
        page.setData("<p>Unable to send the form.</p>");
        when(docDB.getDocIdFromURLImpl("/error.html", "example.com")).thenReturn(42);
        when(docDB.getDoc(42, -1, false)).thenReturn(page);
        GroupsDB groupsDB = mock(GroupsDB.class);
        when(groupsDB.getGroup(10)).thenReturn(new GroupDetails());

        try (
            MockedStatic<Constants> constants = mockStatic(Constants.class, invocation ->
                invocation.getMethod().getReturnType() == String.class ? "" : RETURNS_DEFAULTS.answer(invocation));
            MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
            MockedStatic<DocDB> documents = mockStatic(DocDB.class);
            MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
            MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
            MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class);
            MockedStatic<Adminlog> audit = mockStatic(Adminlog.class);
            MockedStatic<Logger> logger = mockStatic(Logger.class)
        ) {
            cloud.when(CloudToolsForCore::getDomainId).thenReturn(1);
            cloud.when(() -> CloudToolsForCore.isGroupFromMyDomain(10)).thenReturn(true);
            documents.when(DocDB::getInstance).thenReturn(docDB);
            documents.when(() -> DocDB.getDomain(request)).thenReturn("example.com");
            documents.when(() -> DocDB.canAccess(page, null)).thenReturn(true);
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            editor.when(() -> EditorToolsForCore.renderIncludes(page, false, request)).thenReturn(page.getData());

            ResponseEntity<String> result = new MultistepFormsRestController(service).saveForm("contact", 1L, "en", request);
            JSONObject response = new JSONObject(result.getBody());
            assertEquals(400, result.getStatusCode().value());
            assertEquals("Email delivery failed.", response.getString("err_msg"));
            assertFalse(response.getBoolean("end_try"));
            if (Boolean.TRUE.equals(forwardAsContent) && errorRedirect != null && pageAvailable) {
                assertEquals(page.getData(), response.getString("errorHtml"));
            } else {
                assertFalse(response.has("errorHtml"));
            }
            if (!Boolean.TRUE.equals(forwardAsContent) && errorRedirect != null) {
                assertEquals(errorRedirect, response.getString("err_redirect"));
            } else {
                assertFalse(response.has("err_redirect"));
            }
            assertFalse(response.has("thankYouHtml"));
        }
    }
}
