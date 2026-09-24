package sk.iway.iwcm.security;

import net.sourceforge.stripes.mock.MockHttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;
import sk.iway.iwcm.common.WriteTagToolsForCore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for WriteTagToolsForCore CSRF token generation fix.
 *
 * The security fix adds CSRF token injection inside the disableSpamProtectionJavascript
 * code path in preventSpam(). Previously, when spam protection JS was disabled for a
 * session (WriteTag.disableSpamProtectionJavascript=true), no CSRF token was generated
 * for formmail forms AND verifyTokenAndDeleteIt() bypassed validation. Both holes are
 * now closed: tokens are generated here, and the bypass in CSRF was removed.
 *
 * NOTE: The relevant method is preventSpam(), not fixXhtml(). fixXhtml() performs
 * only structural HTML transformations (target=_blank rewriting, multi-domain paths)
 * and never injects CSRF tokens.
 */
@SuppressWarnings("java:S5976")
@Execution(ExecutionMode.SAME_THREAD)
class WriteTagToolsCSRFTest extends BaseWebjetTest {

    private TestRequest request;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession(new MockServletContext("testWriteTag"));
        request = new TestRequest();
        request.setSession(session);
        // preventSpam reads path_filter_orig_path; prevent NPE in PathFilter.getOrigPath()
        request.setAttribute("path_filter_orig_path", "/test.do");
        Constants.setString("spamProtectionJavascript", "");
        Constants.setBoolean("spamProtection", true);
    }

    // --- Core security fix: CSRF token injected when spam protection JS is disabled ---

    @Test
    void testPreventSpam_DisabledSpamProtectionJs_InjectsCsrfTokenInFormmailForm() {
        // Security regression test: when disableSpamProtectionJavascript=true the old
        // code generated no CSRF token. The fix injects __token into formmail forms.
        session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");

        StringBuilder text = new StringBuilder(
            "<form action='/formmail.do' method='post'>" +
            "<input type='text' name='field1' />" +
            "</form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(text, request);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.toString().contains("name=\"__token\""),
            "CSRF __token field must be injected into formmail form when spam protection JS is disabled");
    }

    @Test
    void testPreventSpam_DisabledSpamProtectionJs_InjectsLngHiddenField() {
        // The fix also adds a __lng hidden field alongside the CSRF token
        session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");

        StringBuilder text = new StringBuilder(
            "<form action='/formmail.do' method='post'>" +
            "<input type='text' name='field1' />" +
            "</form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(text, request);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.toString().contains("name=\"__lng\""),
            "Language hidden field __lng must be injected alongside CSRF token");
    }

    @Test
    void testPreventSpam_DisabledSpamProtectionJs_OnlyFormmailFormGetsToken() {
        // When a page contains both a formmail form and a regular form, only the
        // formmail form must receive the CSRF token injection.
        session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");

        StringBuilder text = new StringBuilder(
            "<form action='/formmail.do' method='post'><input name='f1' /></form>" +
            "<form action='/other.do' method='post'><input name='f2' /></form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(text, request);

        assertNotNull(result, "Result should not be null");
        String html = result.toString();
        int tokenCount = countOccurrences(html, "name=\"__token\"");
        assertEquals(1, tokenCount,
            "Exactly one CSRF token must be injected — only for the formmail form, not the regular form");
    }

    @Test
    void testPreventSpam_DisabledSpamProtectionJs_NonFormmailFormReceivesNoToken() {
        // Forms with no formmail.do action must not have a CSRF token injected
        session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");

        StringBuilder text = new StringBuilder(
            "<form action='/other/action.do' method='post'>" +
            "<input type='text' name='field1' />" +
            "</form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(text, request);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.toString().contains("name=\"__token\""),
            "CSRF token must NOT be injected into a non-formmail form");
    }

    @Test
    void testPreventSpam_GlobalSpamProtectionDisabled_NoCsrfTokenInjected() {
        // When the global spamProtection flag is false, preventSpam returns immediately
        // before reaching the CSRF injection block — no token must be added.
        Constants.setBoolean("spamProtection", false);
        session.setAttribute("WriteTag.disableSpamProtectionJavascript", "true");

        StringBuilder text = new StringBuilder(
            "<form action='/formmail.do' method='post'><input name='f1' /></form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(new StringBuilder(text), request);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.toString().contains("name=\"__token\""),
            "No CSRF injection should occur when global spam protection is disabled (early return)");
    }

    @Test
    void testPreventSpam_BypassAttributeNotSet_NoCsrfInjectedViaBypassPath() {
        // When disableSpamProtectionJavascript is not set, the CSRF injection block
        // is not entered. The normal spam-protection path runs (or falls through
        // when spamProtectionJavascript is empty) without injecting a token.
        // disableSpamProtectionJavascript is intentionally NOT set here.
        Constants.setString("spamProtectionJavascript", "");

        StringBuilder text = new StringBuilder(
            "<form action='/formmail.do' method='post'>" +
            "<input type='text' name='field1' />" +
            "</form>"
        );

        StringBuilder result = WriteTagToolsForCore.preventSpam(text, request);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.toString().contains("name=\"__token\""),
            "CSRF token must not be injected via the bypass code path when the attribute is not set");
    }

    // --- fixXhtml tests (tests actual fixXhtml structural behaviour) ---

    @Test
    void testFixXhtml_NullInput() {
        assertNull(WriteTagToolsForCore.fixXhtml(null, request),
            "Null input should return null");
    }

    @Test
    void testFixXhtml_EmptyInput() {
        StringBuilder result = WriteTagToolsForCore.fixXhtml(new StringBuilder(""), request);
        assertNotNull(result, "Empty input should return non-null result");
    }

    // --- Helpers ---

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }
}
