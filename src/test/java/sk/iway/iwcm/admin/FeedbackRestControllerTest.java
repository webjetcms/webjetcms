package sk.iway.iwcm.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.admin.layout.SystemInfo;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UsersDB;

/** Verifies feedback formatting, empty-message validation, and HTML sanitization. */
class FeedbackRestControllerTest extends BaseWebjetTest {

    @Test
    void preservesEditorFormatting() {
        String result = FeedbackRestController.prepareFeedbackText(
            "<p><strong>Bold</strong> <em>Italic</em> <u>Underlined</u></p><ul><li>Item</li></ul>");
        assertTrue(result.contains("<strong>Bold</strong>"));
        assertTrue(result.contains("<em>Italic</em>"));
        assertTrue(result.contains("<u>Underlined</u>"));
        assertTrue(result.contains("<ul>"));
        assertTrue(result.contains("<li>Item</li>"));
    }

    @Test
    void removesExecutableHtml() {
        String result = FeedbackRestController.prepareFeedbackText(
            "<p onclick='alert(1)'>Message<script>alert(1)</script><img src='https://example.com/image.png' onerror='alert(1)'></p>"
                + "<a href='javascript:alert(1)'>Unsafe link</a><a href='https://example.com'>Safe link</a>");
        assertFalse(result.contains("onclick"));
        assertFalse(result.contains("script"));
        assertFalse(result.contains("onerror"));
        assertTrue(result.contains("href=\"https://example.com\""));
    }

    /** Verifies clipboard diagnostics and the generated email HTML without delivering any email. */
    @Test
    void attachesSharedDiagnosticsWithPermissionFiltering() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("data[text]", "<p>Feedback diagnostics autotest</p>");
        Identity user = new Identity();
        user.setDisabledItemsTable(new HashMap<>());
        BrowserDetector detector = mock(BrowserDetector.class);
        when(detector.getBrowserName()).thenReturn("Firefox");
        when(detector.getBrowserVersionShort()).thenReturn("143");
        when(detector.getBrowserPlatform()).thenReturn("Windows");
        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
             MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
             MockedStatic<BrowserDetector> browsers = mockStatic(BrowserDetector.class);
             MockedStatic<SpamProtection> spam = mockStatic(SpamProtection.class);
             MockedConstruction<MailHelper> mail = mockConstruction(MailHelper.class,
                 withSettings().defaultAnswer(RETURNS_SELF), (helper, context) -> when(helper.send()).thenReturn(true))) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            domains.when(CloudToolsForCore::getDomainName).thenReturn("content.example.com");
            browsers.when(() -> BrowserDetector.getInstance(request)).thenReturn(detector);
            spam.when(() -> SpamProtection.canPost(eq("form"), anyString(), eq(request))).thenReturn(true);
            for (boolean permitted : new boolean[] { true, false }) {
                request.setParameter("data[pageUrl]", "https://admin.example.com:8443/admin/v9/web-pages/?docid=123#editor");
                if (!permitted) user.addDisabledItem("menuConfig");
                String diagnostics = FeedbackRestController.prepareSystemInfo(request);
                assertEquals(new SystemInfo(request, "content.example.com").getText(request.getParameter("data[pageUrl]")), diagnostics);
                assertTrue(diagnostics.contains("content.example.com"));
                assertTrue(diagnostics.contains("CMS doména: https://admin.example.com:8443"));
                assertTrue(diagnostics.contains("Firefox 143 · Windows"));
                assertTrue(diagnostics.contains("/admin/v9/web-pages/?docid=123#editor"));
                if (!permitted) assertFalse(diagnostics.contains("Server:"));
                for (String origin : new String[] { "http://iwcm.interway.sk", "https://admin.example.com:8443" }) {
                    request.setParameter("data[pageUrl]", origin + "/admin/v9/web-pages/?docid=123&view=all#editor");
                    assertEquals("OK", new FeedbackRestController().sendFeedback(request));
                    MailHelper helper = mail.constructed().get(mail.constructed().size() - 1);
                    ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
                    verify(helper).setMessage(message.capture());
                    verify(helper).send();
                    assertTrue(message.getValue().contains("CMS doména: " + origin + "<br/>"));
                    assertTrue(message.getValue().contains("/admin/v9/web-pages/?docid=123&amp;view=all#editor<br/>"));
                    if (!permitted) assertFalse(message.getValue().contains("Server:"));
                }
            }
        }
    }
}
