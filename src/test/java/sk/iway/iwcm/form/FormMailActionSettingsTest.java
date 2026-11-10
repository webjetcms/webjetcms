package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;

/** Verifies how persisted form settings override the submitted source page and recipient parameters. */
class FormMailActionSettingsTest {

    /** Keeps the submitted page for unset selections while retaining positive database overrides and trusted recipients. */
    @ParameterizedTest
    @CsvSource({"-1, 26180", "0, 26180", "42, 42"})
    void resolvesSourcePageBeforeCheckingRecipients(int configuredDocId, int expectedDocId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("useFormDocId", "26180");

        try (MockedStatic<FormSettingsService> settings = mockStatic(FormSettingsService.class);
             MockedStatic<PageLng> language = mockStatic(PageLng.class)) {
            language.when(() -> PageLng.getUserLng(request)).thenReturn("sk");
            language.when(() -> PageLng.getUserLngIso("sk")).thenReturn("sk-SK");
            settings.when(() -> FormSettingsService.load("autotest-settings")).thenReturn(Map.of(
                "useFormDocId", String.valueOf(configuredDocId),
                "recipients", "autotest@example.com"
            ));

            HttpServletRequest prepared = FormMailAction.fillRequestWithDatabaseOptions("autotest-settings", request, null);

            assertEquals(String.valueOf(expectedDocId), prepared.getParameter("useFormDocId"));
            assertEquals("autotest@example.com", prepared.getParameter("recipients"));
            assertEquals("true", prepared.getAttribute("DBrecipients"));
            if (configuredDocId > 0) {
                assertEquals("26180", prepared.getParameter("useFormDocIdOriginal"));
                assertEquals("true", prepared.getAttribute("DBuseFormDocId"));
            } else {
                assertNull(prepared.getParameter("useFormDocIdOriginal"));
                assertNull(prepared.getAttribute("DBuseFormDocId"));
            }
        }
    }
}
