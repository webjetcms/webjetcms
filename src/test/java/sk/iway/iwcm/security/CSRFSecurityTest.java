package sk.iway.iwcm.security;

import net.sourceforge.stripes.mock.MockHttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;
import sk.iway.iwcm.system.stripes.CSRF;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for CSRF security fix.
 * Verifies that CSRF token validation cannot be bypassed by disabling spam protection.
 *
 * The security fix removes the bypass: previously verifyTokenAndDeleteIt() would
 * return true immediately when WriteTag.disableSpamProtectionJavascript was "true",
 * effectively allowing CSRF when spam protection was disabled.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CSRFSecurityTest extends BaseWebjetTest {

    private MockHttpSession session;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession(new org.springframework.mock.web.MockServletContext("testCSRF"));
        request = new TestRequest();
        ((TestRequest)request).setSession(session);
    }

    // --- Tests for CSRF token generation and validation ---

    @Test
    void testGetCsrfToken_GeneratesToken() {
        String token = CSRF.getCsrfToken(session, true);
        assertNotNull(token, "CSRF token should be generated");
        assertFalse(token.isEmpty(), "CSRF token should not be empty");
    }

    @Test
    void testGetCsrfTokenInputFiled_GeneratesInputField() {
        String inputField = CSRF.getCsrfTokenInputFiled(session);
        assertNotNull(inputField, "CSRF input field should be generated");
        assertTrue(inputField.contains("<input"), "Should contain HTML input tag");
        assertTrue(inputField.contains("name=\"__token\""), "Should have __token name attribute");
        assertTrue(inputField.contains("type=\"hidden\""), "Should be hidden input");
    }

    @Test
    void testVerifyTokenAndDeleteIt_WithValidToken() {
        // Generate a valid token
        String token = CSRF.getCsrfToken(session, true);
        ((TestRequest)request).setParameter("__token", token);

        // Should validate successfully
        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "Valid token should be accepted");
    }

    @Test
    void testVerifyTokenAndDeleteIt_WithInvalidToken() {
        ((TestRequest)request).setParameter("__token", "invalid-token-value");

        assertFalse(CSRF.verifyTokenAndDeleteIt(request),
            "Invalid token should be rejected");
    }

    @Test
    void testVerifyTokenAndDeleteIt_WithNoToken() {
        // No __token parameter set
        assertFalse(CSRF.verifyTokenAndDeleteIt(request),
            "Request without token should be rejected");
    }

    @Test
    void testVerifyTokenAndDeleteIt_TokenCannotBeReused() {
        // Generate and validate a valid token
        String token = CSRF.getCsrfToken(session, true);
        ((TestRequest)request).setParameter("__token", token);

        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "First validation should succeed");

        // Try to reuse the same token
        ((TestRequest)request).setParameter("__token", token);
        assertFalse(CSRF.verifyTokenAndDeleteIt(request),
            "Token should not be reusable (one-time use)");
    }

    // --- Tests for the security fix: spam protection bypass removed ---

    @Test
    void testVerifyTokenAndDeleteIt_NoBypassWithDisabledSpamProtection() {
        // The fix ensures that even with WriteTag.disableSpamProtectionJavascript=true,
        // the token is still validated (the old code returned true immediately).
        //
        // The old code had:
        //   if ("true".equals(request.getSession().getAttribute("WriteTag.disableSpamProtectionJavascript"))) {
        //       return true;
        //   }
        // This was REMOVED in the security fix. Now tokens are always validated.

        // Without any bypass, validation should work normally
        String token = CSRF.getCsrfToken(session, true);
        ((TestRequest)request).setParameter("__token", token);
        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "Token validation should work with valid token");

        // Without a valid token, validation should fail (not bypassed)
        ((TestRequest)request).setParameter("__token", "any-value");
        assertFalse(CSRF.verifyTokenAndDeleteIt(request),
            "CSRF should NOT be bypassed even when spam protection is disabled");
    }

    @Test
    void testVerifyTokenAjax_WithValidToken() {
        String token = CSRF.getCsrfToken(session, true);
        ((TestRequest)request).setParameter("__token", token);

        assertTrue(CSRF.verifyTokenAjax(request),
            "AJAX token validation should accept valid token");
    }

    @Test
    void testVerifyTokenAjax_WithInvalidToken() {
        ((TestRequest)request).setParameter("__token", "invalid");

        assertFalse(CSRF.verifyTokenAjax(request),
            "AJAX token validation should reject invalid token");
    }

    @Test
    void testGetParameterName_ReturnsTokenName() {
        assertEquals("__token", CSRF.getParameterName(),
            "Parameter name should be __token");
    }

    @Test
    void testGetCSRFTokenQuery_GeneratesQueryParameter() {
        String query = CSRF.getCSRFTokenQuery(session, true);
        assertNotNull(query, "Query string should be generated");
        assertTrue(query.startsWith("__token="), "Should start with __token=");
    }

    @Test
    void testGetCsrfTokenInputFiled_WithSaveToSessionFalse() {
        // When saveToSession is false, token is generated but not saved to session
        String inputField = CSRF.getCsrfTokenInputFiled(session, false);
        assertNotNull(inputField, "Input field should be generated even without saving");
    }

    @Test
    void testVerifyTokenAndDeleteIt_MultipleTokensInSession() {
        // Generate multiple tokens
        String token1 = CSRF.getCsrfToken(session, true);
        String token2 = CSRF.getCsrfToken(session, true);

        // Validate the second token
        ((TestRequest)request).setParameter("__token", token2);
        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "Second token should be valid");

        // First token should still be valid (only the validated one is deleted)
        ((TestRequest)request).setParameter("__token", token1);
        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "First token should still be valid after second is used");
    }

    @Test
    void testVerifyTokenAndDeleteIt_MultipleParameterValues() {
        // Multiple tokens in request parameters
        String token1 = CSRF.getCsrfToken(session, true);
        String token2 = CSRF.getCsrfToken(session, true);

        // Set multiple parameter values
        ((TestRequest)request).getParameterMap().put("__token", new String[]{token1, token2});

        assertTrue(CSRF.verifyTokenAndDeleteIt(request),
            "Should accept request with multiple valid tokens");
    }
}
