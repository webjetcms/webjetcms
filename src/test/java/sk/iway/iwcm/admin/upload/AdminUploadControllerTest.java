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

/**
 * Verifies validation performed by the administrative upload conflict endpoints.
 */
class AdminUploadControllerTest {

    /**
     * Verifies that overwrite and keep-both operations reject destinations outside approved upload roots.
     */
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

    /**
     * Verifies that a delayed archive upload cannot be used for an immediate replacement operation.
     */
    @Test
    void scheduledArchiveUploadRejectsImmediateReplacement() {
        String originalArchiveRoot = Constants.getString("fileArchivDefaultDirPath");
        try {
            Constants.setString("fileArchivDefaultDirPath", "/custom/archive/");

            Identity user = mock(Identity.class);
            when(user.isAdmin()).thenReturn(true);
            when(user.isEnabledItem("cmp_file_archiv")).thenReturn(true);
            when(user.isFolderWritable("/custom/archive/reports/")).thenReturn(true);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/admin/upload/overwrite");
            request.addHeader("referer", "https://example.test/apps/file-archive/admin/");
            request.getSession().setAttribute(Constants.USER_KEY, user);
            request.addParameter(FileArchiveBulkUploadOptions.PARAM_SAVE_LATER, "true");
            request.addParameter(FileArchiveBulkUploadOptions.PARAM_DATE_UPLOAD_LATER,
                Long.toString(System.currentTimeMillis() + 3_600_000L));
            request.addParameter(FileArchiveBulkUploadOptions.PARAM_EMAILS, "admin@example.com");

            JSONObject response = new JSONObject(new AdminUploadController().overwrite(
                "key", "/custom/archive/reports/", "document.pdf", "fileArchive", request));

            assertFalse(response.getBoolean("success"));
            assertEquals(FileArchiveBulkUploadOptions.ERROR_SAVE_LATER_REPLACE,
                response.getString("error"));
        } finally {
            Constants.setString("fileArchivDefaultDirPath", originalArchiveRoot);
        }
    }

    private void assertInvalidFolder(String json) {
        JSONObject response = new JSONObject(json);
        assertFalse(response.getBoolean("success"));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            response.getString("error"));
    }
}
