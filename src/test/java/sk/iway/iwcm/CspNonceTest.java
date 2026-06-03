package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * JUnit tests for CSP (Content-Security-Policy) nonce functionality.
 * Tests nonce generation, placeholder replacement, and script tag injection.
 */
class CspNonceTest extends BaseWebjetTest {

	@BeforeAll
	public static void initJpa() {
		Constants.setString("jpaAddPackages", "");
		DBPool.getInstance();
		DBPool.jpaInitialize();
	}

	@BeforeEach
	public void setUp() {
		// Reset the request context for each test
		SetCharacterEncodingFilter.unRegisterDataContext();
		Constants.setString("contentSecurityPolicy", "");
	}

	// ==================== RequestBean Tests ====================

	@Test
	void testRequestBeanCspNonceDefaultNull() {
		RequestBean bean = new RequestBean();
		assertNull(bean.getCspNonce(), "Default cspNonce should be null");
	}

	@Test
	void testRequestBeanCspNonceSetAndGet() {
		RequestBean bean = new RequestBean();
		bean.setCspNonce("abc123test");
		assertEquals("abc123test", bean.getCspNonce(), "Should return the set nonce value");
	}

	@Test
	void testRequestBeanCspNonceEmptyString() {
		RequestBean bean = new RequestBean();
		bean.setCspNonce("");
		assertEquals("", bean.getCspNonce(), "Should allow empty string");
	}

	// ==================== SetCharacterEncodingFilter Tests ====================

	@Test
	void testGenerateCspNonceWhenNoPlaceholder() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self'");
		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);
		RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
		assertNotNull(bean, "RequestBean should not be null");
		assertNull(bean.getCspNonce(), "CSP nonce should be null when contentSecurityPolicy has no {nonce} placeholder");
	}

	@Test
	void testGenerateCspNonceWhenPlaceholderPresent() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");
		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);
		RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
		assertNotNull(bean, "RequestBean should not be null");
		assertNotNull(bean.getCspNonce(), "CSP nonce should be generated when contentSecurityPolicy contains {nonce}");
		assertTrue(bean.getCspNonce().length() > 0, "CSP nonce should not be empty");
	}

	@Test
	void testGenerateCspNonceUniqueness() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");
		String nonce1 = null, nonce2 = null, nonce3 = null;
		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");

		// Generate 3 nonces and verify they are all different
		for (int i = 0; i < 3; i++) {
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getSession()).thenReturn(mockSession);
			SetCharacterEncodingFilter.registerDataContext(mockRequest);
			RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
			switch (i) {
				case 0: nonce1 = bean.getCspNonce(); break;
				case 1: nonce2 = bean.getCspNonce(); break;
				case 2: nonce3 = bean.getCspNonce(); break;
			}
		}

		assertNotEquals(nonce1, nonce2, "First and second nonces should be different");
		assertNotEquals(nonce2, nonce3, "Second and third nonces should be different");
		assertNotEquals(nonce1, nonce3, "First and third nonces should be different");
	}

	@Test
	void testGenerateCspNonceBase64Format() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");
		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);
		RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
		String nonce = bean.getCspNonce();

		// Base64 without padding contains only A-Z, a-z, 0-9, +, /
		assertTrue(nonce.matches("^[A-Za-z0-9+/]+$"), "CSP nonce should be valid base64 without padding");
	}

	// ==================== PathFilter.setHeader() Tests ====================

	@Test
	void testSetHeaderNoNoncePlaceholderWhenDisabled() {
		Constants.setBoolean("cspNonceEnabled", false);
		Constants.setString("contentSecurityPolicy", "default-src 'self'");

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		verify(mockResponse).setHeader(eq("Content-Security-Policy"), eq("default-src 'self'"));
	}

	@Test
	void testSetHeaderReplacesNoncePlaceholder() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");

		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		// Verify the header was set with the nonce replaced
		verify(mockResponse).setHeader(eq("Content-Security-Policy"), argThat(value ->
			value.contains("default-src 'self'") &&
			value.contains("script-src 'self'") &&
			value.contains(SetCharacterEncodingFilter.getCurrentRequestBean().getCspNonce()) &&
			!value.contains("{nonce}")
		));
	}

	@Test
	void testSetHeaderNoPlaceholderUnchanged() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self'");

		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		verify(mockResponse).setHeader(eq("Content-Security-Policy"), eq("default-src 'self'; script-src 'self'"));
	}

	@Test
	void testSetHeaderMultipleNoncePlaceholders() {
		Constants.setString("contentSecurityPolicy", "script-src 'self' {nonce}; style-src 'self' {nonce}");

		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		String expectedNonce = SetCharacterEncodingFilter.getCurrentRequestBean().getCspNonce();
		// Use a simpler assertion that just checks the nonce appears twice in the header
		verify(mockResponse).setHeader(eq("Content-Security-Policy"), argThat(value -> {
			int count = 0;
			int idx = 0;
			while ((idx = value.indexOf(expectedNonce, idx)) != -1) {
				count++;
				idx += expectedNonce.length();
			}
			return count == 2 && !value.contains("{nonce}");
		}));
	}

	@Test
	void testSetHeaderEmptyConfigReturnsEarly() {
		Constants.setString("contentSecurityPolicy", "");

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	// ==================== ShowDoc.injectCspNonceIntoScripts() Tests ====================

	@Test
	void testInjectCspNonceIntoScriptsSimpleScript() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "testnonce123");

		assertTrue(result.contains("nonce=\"testnonce123\""), "Nonce should be injected into simple script tag");
		assertTrue(result.contains("<script nonce=\"testnonce123\">"), "Script tag should have nonce attribute");
	}

	@Test
	void testInjectCspNonceIntoScriptsScriptWithAttributes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script type=\"text/javascript\" defer>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "myNonce");

		assertTrue(result.contains("nonce=\"myNonce\""), "Nonce should be injected into script with attributes");
		assertTrue(result.contains("type=\"text/javascript\""), "Original attributes should be preserved");
		assertTrue(result.contains("defer"), "Original defer attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsMultipleScripts() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>var a = 1;</script><p>text</p><script>var b = 2;</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "abc123");

		int count = result.split("nonce=\"abc123\"").length - 1;
		assertEquals(2, count, "Nonce should be injected into all script tags");
	}

	@Test
	void testInjectCspNonceIntoScriptsAlreadyHasNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script nonce=\"existing\">console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "newNonce");

		// Should NOT add another nonce
		assertEquals(1, result.split("nonce=").length - 1, "Should not duplicate nonce attribute");
		assertTrue(result.contains("nonce=\"existing\""), "Original nonce should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsNoScriptTags() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><p>No scripts here</p></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "testnonce");

		assertEquals(input, result, "Content without script tags should remain unchanged");
	}

	@Test
	void testInjectCspNonceIntoScriptsEmptyContent() {
		ShowDoc showDoc = new ShowDoc();
		String result = injectCspNonceIntoScripts(showDoc, "", "testnonce");
		assertEquals("", result, "Empty content should remain empty");
	}

	@Test
	void testInjectCspNonceIntoScriptsNullNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, null);

		assertEquals(input, result, "Content should remain unchanged when nonce is null");
	}

	@Test
	void testInjectCspNonceIntoScriptsScriptSrcAttribute() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script src=\"/app.js\" type=\"module\"></script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "moduleNonce");

		assertTrue(result.contains("nonce=\"moduleNonce\""), "Nonce should be injected into external script tag");
		assertTrue(result.contains("src=\"/app.js\""), "Original src attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsMixedScripts() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script src=\"/app.js\"></script><script>var a = 1;</script><script nonce=\"existing\">var b = 2;</script></body></html>";
		String result = injectCspNonceIntoScripts(showDoc, input, "generatedNonce");

		// First two scripts should get nonce, third should keep existing
		int nonceCount = result.split("nonce=").length - 1;
		assertEquals(3, nonceCount, "All script tags should have nonce attribute");
	}

	// Helper method to access private method via reflection
	private String injectCspNonceIntoScripts(ShowDoc showDoc, String htmlContent, String nonce) {
		try {
			java.lang.reflect.Method method = ShowDoc.class.getDeclaredMethod("injectCspNonceIntoScripts", String.class, String.class);
			method.setAccessible(true);
			return (String) method.invoke(showDoc, htmlContent, nonce);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke injectCspNonceIntoScripts", e);
		}
	}
}
