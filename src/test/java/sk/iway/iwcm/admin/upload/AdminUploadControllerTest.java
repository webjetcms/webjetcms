package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;

class AdminUploadControllerTest {

    @Test
    void overwriteAndKeepBothRejectDestinationOutsideUploadRoots() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/upload/overwrite");
        request.getSession().setAttribute(Constants.USER_KEY, user);
        AdminUploadController controller = new AdminUploadController();

        assertInvalidFolder(controller.overwrite(
            "key", "/templates/", "shell.jsp", "file", request));
        request.setRequestURI("/admin/upload/keepboth");
        assertInvalidFolder(controller.keepboth(
            "key", "/templates/", "shell.jsp", "file", request));
    }

    private void assertInvalidFolder(String json) {
        JSONObject response = new JSONObject(json);
        assertFalse(response.getBoolean("success"));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            response.getString("error"));
    }
}
