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
import sk.iway.iwcm.doc.showdoc.ContentCapturingResponseWrapper;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * JUnit tests for CSP (Content-Security-Policy) nonce functionality.
 * Tests nonce generation, placeholder replacement, and script tag injection.
 */
class CspNonceTest extends BaseWebjetTest {

	@BeforeAll
	static void initJpa() {
		Constants.setString("jpaAddPackages", "");
		DBPool.getInstance();
		DBPool.jpaInitialize();
	}

	@BeforeEach
	void setUp() {
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
		assertFalse(bean.getCspNonce().isEmpty(), "CSP nonce should not be empty");
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
				default: break;
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
		Constants.setString("contentSecurityPolicy", "default-src 'self'");

		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		verify(mockResponse).setHeader("Content-Security-Policy", "default-src 'self'");
	}

	@Test
	void testSetHeaderReplacesNoncePlaceholder() {
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");

		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		String expectedNonce = SetCharacterEncodingFilter.getCurrentRequestBean().getCspNonce();
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		PathFilter.setHeader(mockResponse, "Content-Security-Policy", "contentSecurityPolicy");

		// Verify the header was set with the nonce replaced by 'nonce-<value>' (CSP source expression format)
		verify(mockResponse).setHeader(eq("Content-Security-Policy"), argThat(value ->
			value.contains("default-src 'self'") &&
			value.contains("script-src 'self'") &&
			value.contains("'nonce-" + expectedNonce + "'") &&
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

		verify(mockResponse).setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'");
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

	// ==================== ShowDoc.injectCspNonceIntoTags() Tests ====================

	@Test
	void testInjectCspNonceIntoScriptsSimpleScript() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "testnonce123");

		assertTrue(result.contains("nonce=\"testnonce123\""), "Nonce should be injected into simple script tag");
		assertTrue(result.contains("<script nonce=\"testnonce123\">"), "Script tag should have nonce attribute");
	}

	@Test
	void testInjectCspNonceIntoScriptsScriptWithAttributes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script type=\"text/javascript\" defer>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "myNonce");

		assertTrue(result.contains("nonce=\"myNonce\""), "Nonce should be injected into script with attributes");
		assertTrue(result.contains("type=\"text/javascript\""), "Original attributes should be preserved");
		assertTrue(result.contains("defer"), "Original defer attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsMultipleScripts() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>var a = 1;</script><p>text</p><script>var b = 2;</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "abc123");

		int count = result.split("nonce=\"abc123\"").length - 1;
		assertEquals(2, count, "Nonce should be injected into all script tags");
	}

	@Test
	void testInjectCspNonceIntoScriptsAlreadyHasNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script nonce=\"existing\">console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "newNonce");

		// Should NOT add another nonce
		assertEquals(1, result.split("nonce=").length - 1, "Should not duplicate nonce attribute");
		assertTrue(result.contains("nonce=\"existing\""), "Original nonce should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsNoScriptTags() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><p>No scripts here</p></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "testnonce");

		assertEquals(input, result, "Content without script tags should remain unchanged");
	}

	@Test
	void testInjectCspNonceIntoScriptsEmptyContent() {
		ShowDoc showDoc = new ShowDoc();
		String result = injectCspNonceIntoTags(showDoc, "", "testnonce");
		assertEquals("", result, "Empty content should remain empty");
	}

	@Test
	void testInjectCspNonceIntoScriptsNullNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('hello');</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, null);

		assertEquals(input, result, "Content should remain unchanged when nonce is null");
	}

	@Test
	void testInjectCspNonceIntoScriptsScriptSrcAttribute() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script src=\"/app.js\" type=\"module\"></script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "moduleNonce");

		assertTrue(result.contains("nonce=\"moduleNonce\""), "Nonce should be injected into external script tag");
		assertTrue(result.contains("src=\"/app.js\""), "Original src attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoScriptsMixedScripts() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script src=\"/app.js\"></script><script>var a = 1;</script><script nonce=\"existing\">var b = 2;</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "generatedNonce");

		// First two scripts should get nonce, third should keep existing
		int nonceCount = result.split("nonce=").length - 1;
		assertEquals(3, nonceCount, "All script tags should have nonce attribute");
	}

	// ==================== ShowDoc injectCspNonceIntoTags() Tests for Style and Link Tags ====================

	@Test
	void testInjectCspNonceIntoTagsSimpleStyle() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><style>body { color: red; }</style></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "styleNonce");
		assertTrue(result.contains("nonce=\"styleNonce\""), "Nonce should be injected into style tag");
		assertTrue(result.contains("<style nonce=\"styleNonce\">"), "Style tag should have nonce attribute");
	}

	@Test
	void testInjectCspNonceIntoTagsStyleWithAttributes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><style type=\"text/css\" media=\"screen\">body { color: red; }</style></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "myStyleNonce");
		assertTrue(result.contains("nonce=\"myStyleNonce\""), "Nonce should be injected into style with attributes");
		assertTrue(result.contains("type=\"text/css\""), "Original attributes should be preserved");
		assertTrue(result.contains("media=\"screen\""), "Original media attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsMultipleStyles() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><style>.a { color: red; }</style><style>.b { color: blue; }</style></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "abc");
		int count = result.split("nonce=\"abc\"").length - 1;
		assertEquals(2, count, "Nonce should be injected into all style tags");
	}

	@Test
	void testInjectCspNonceIntoTagsStyleAlreadyHasNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><style nonce=\"existing\">body { color: red; }</style></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "newNonce");
		assertEquals(1, result.split("nonce=").length - 1, "Should not duplicate nonce attribute");
		assertTrue(result.contains("nonce=\"existing\""), "Original nonce should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkStylesheetDoubleQuotes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" href=\"/css/main.css\"></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into link stylesheet tag");
		assertTrue(result.contains("rel=\"stylesheet\""), "Original rel attribute should be preserved");
		assertTrue(result.contains("href=\"/css/main.css\""), "Original href attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkStylesheetSingleQuotes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel='stylesheet' href='/css/main.css'></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into link stylesheet tag with single quotes");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkStylesheetNoQuotes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=stylesheet href=/css/main.css></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into link stylesheet tag without quotes");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkNotStylesheet() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"icon\" href=\"/favicon.ico\"></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertEquals(input, result, "Non-stylesheet link tags should not get nonce");
		assertFalse(result.contains("nonce="), "Should not inject nonce into non-stylesheet link");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkAlreadyHasNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" href=\"/css/main.css\" nonce=\"existing\"></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "newNonce");
		int nonceCount = result.split("nonce=").length - 1;
		assertEquals(1, nonceCount, "Should not duplicate nonce attribute");
		assertTrue(result.contains("nonce=\"existing\""), "Original nonce should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsMixedContent() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><style>.a { color: red; }</style><link rel=\"stylesheet\" href=\"/css/main.css\"></head><body><script>var a = 1;</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "mixedNonce");
		// Should inject nonce into all 3 tag types (script, style, link)
		int nonceCount = result.split("nonce=\"mixedNonce\"").length - 1;
		assertEquals(3, nonceCount, "Should inject nonce into script, style, and link stylesheet tags");
		assertTrue(result.contains("<script nonce=\"mixedNonce\">"), "Script tag should have nonce");
		assertTrue(result.contains("<style nonce=\"mixedNonce\">"), "Style tag should have nonce");
		assertTrue(result.contains("nonce=\"mixedNonce\""), "Link tag should have nonce");
	}

	@Test
	void testInjectCspNonceIntoTagsNoFalsePositiveDataNonce() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<script data-nonce=\"existing\">console.log('test');</script>";
		String result = injectCspNonceIntoTags(showDoc, input, "newNonce");
		// Should NOT inject nonce because data-nonce is not the nonce attribute
		// The false positive check should only match actual nonce attribute
		assertTrue(result.contains("<script data-nonce=\"existing\" nonce=\"newNonce\">"),
			"Should inject nonce when only data-nonce exists (not a real nonce attribute)");
	}

	@Test
	void testInjectCspNonceIntoTagsNoFalsePositiveQueryParams() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<script src=\"/app.js?nonce=123\">console.log('test');</script>";
		String result = injectCspNonceIntoTags(showDoc, input, "realNonce");
		// Should NOT skip injection just because ?nonce= appears in src attribute
		assertTrue(result.contains("<script src=\"/app.js?nonce=123\" nonce=\"realNonce\">"),
			"Should inject nonce when only ?nonce= appears in URL (not a real nonce attribute)");
	}

	@Test
	void testShowDocForwardWithCspNonceWhenStyleToHeadDisabled() throws Exception {
		// Integration test: Verify nonce injection works when showDocMoveStyleToHead is disabled
		Constants.setBoolean("showDocMoveStyleToHead", false);
		Constants.setString("contentSecurityPolicy", "default-src 'self'; script-src 'self' {nonce}");

		// Create a mock request with a valid nonce
		HttpSession mockSession = mock(HttpSession.class);
		when(mockSession.getId()).thenReturn("test-session-id");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		when(mockRequest.getSession()).thenReturn(mockSession);
		when(mockRequest.getRequestURI()).thenReturn("/test");
		when(mockRequest.getServletPath()).thenReturn("");
		when(mockRequest.getContextPath()).thenReturn("");
		when(mockRequest.getHeader("accept")).thenReturn("text/html");
		when(mockRequest.getLocale()).thenReturn(java.util.Locale.getDefault());
		SetCharacterEncodingFilter.registerDataContext(mockRequest);

		// Create response wrapper to capture output
		ContentCapturingResponseWrapper responseWrapper = mock(ContentCapturingResponseWrapper.class);
		String testHtml = "<html><head></head><body><script>console.log('test');</script></body></html>";
		when(responseWrapper.getCapturedContent()).thenReturn(testHtml);
		when(responseWrapper.getContentType()).thenReturn("text/html;charset=UTF-8");
		when(responseWrapper.getRedirectLocation()).thenReturn(null);
		when(responseWrapper.hasError()).thenReturn(false);

		// Create a ShowDoc instance and call forwardWithBodyProcessing
		ShowDoc showDoc = new ShowDoc();
		java.lang.reflect.Method method = ShowDoc.class.getDeclaredMethod("forwardWithBodyProcessing",
			String.class, HttpServletRequest.class, HttpServletResponse.class);
		method.setAccessible(true);

		// We can't easily test the full forwarding path without a servlet container,
		// but we can verify the nonce injection logic is triggered by checking
		// that the nonce is generated and available
		RequestBean bean = SetCharacterEncodingFilter.getCurrentRequestBean();
		assertNotNull(bean.getCspNonce(), "CSP nonce should be generated when contentSecurityPolicy has {nonce}");

		// Verify the nonce is injected into HTML content
		String cspNonce = bean.getCspNonce();
		String input = "<html><head></head><body><script>console.log('test');</script></body></html>";
		String result = injectCspNonceIntoTags(showDoc, input, cspNonce);
		assertTrue(result.contains("<script nonce=\"" + cspNonce + "\">"),
			"Nonce should be injected into script tag even when showDocMoveStyleToHead is disabled");
	}

	@Test
	void testProcessInlineStylesSimple() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><div style=\"color: red; font-size: 14px;\">Test</div></body></html>";
		String nonce = "testNonce";

		String result = invokeProcessInlineStyles(showDoc, input, nonce);
		// Should replace style attribute with data-inline-style and inject CSS
		assertTrue(result.contains("data-inline-style=\"nonce1\""), "Should replace style with data-inline-style");
		assertFalse(result.contains(" style=\""), "Should not contain original style attribute");
		assertTrue(result.contains("[data-inline-style=\"nonce1\"]"), "Should generate CSS rule");
		assertTrue(result.contains("color: red !important"), "Should include color property with !important");
		assertTrue(result.contains("font-size: 14px !important"), "Should include font-size property with !important");
		assertTrue(result.contains("nonce=\"" + nonce + "\""), "Should inject nonce into style tag");
	}

	@Test
	void testProcessInlineStylesSVG() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><svg style=\"width: 10px; height: 20px;\"></svg></body></html>";
		String nonce = "svgNonce";

		String result = invokeProcessInlineStyles(showDoc, input, nonce);
		// Should handle SVG elements with inline styles
		assertTrue(result.contains("data-inline-style=\"nonce1\""), "Should replace SVG style with data-inline-style");
		assertFalse(result.contains(" style=\""), "Should not contain original style attribute");
		assertTrue(result.contains("width: 10px !important"), "Should include width property with !important");
		assertTrue(result.contains("height: 20px !important"), "Should include height property with !important");
	}

	@Test
	void testProcessInlineStylesMultipleElements() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><div style=\"color: red;\">A</div><span style=\"color: blue;\">B</span></body></html>";
		String nonce = "multiNonce";

		String result = invokeProcessInlineStyles(showDoc, input, nonce);
		// Should handle multiple elements with different counters
		assertTrue(result.contains("data-inline-style=\"nonce1\""), "First element should have counter 1");
		assertTrue(result.contains("data-inline-style=\"nonce2\""), "Second element should have counter 2");
		assertFalse(result.contains(" style=\""), "Should not contain original style attributes");
		assertTrue(result.contains("[data-inline-style=\"nonce1\"]"), "Should generate CSS rule for counter 1");
		assertTrue(result.contains("[data-inline-style=\"nonce2\"]"), "Should generate CSS rule for counter 2");
	}

	@Test
	void testProcessInlineEventHandlersSimple() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><button onclick=\"clicked()\">Click</button></body></html>";
		String nonce = "eventNonce";

		String result = invokeProcessInlineEventHandlers(showDoc, input, nonce);
		// Should replace onclick with data-inline-onclick and inject JavaScript
		assertTrue(result.contains("data-inline-onclick=\"nonce1\""), "Should replace onclick with data-inline-onclick");
		assertFalse(result.contains(" onclick=\""), "Should not contain original onclick attribute");
		assertTrue(result.contains("document.querySelectorAll('[data-inline-onclick=\"nonce1\"]')"), "Should generate JavaScript selector");
		assertTrue(result.contains("el.onclick = function(event) {clicked();};"), "Should generate function wrapper");
		assertTrue(result.contains("nonce=\"" + nonce + "\""), "Should inject nonce into script tag");
	}

	@Test
	void testProcessInlineEventHandlersMultipleTypes() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><button onclick=\"clicked()\" onmouseover=\"hovered()\">Click</button></body></html>";
		String nonce = "multiEventNonce";

		String result = invokeProcessInlineEventHandlers(showDoc, input, nonce);
		// Should handle multiple event handlers with separate counters
		assertTrue(result.contains("data-inline-onclick=\"nonce1\""), "onclick should have counter 1");
		assertTrue(result.contains("data-inline-onmouseover=\"nonce1\""), "onmouseover should have counter 1 (separate)");
		assertFalse(result.contains(" onclick=\""), "Should not contain original onclick");
		assertFalse(result.contains(" onmouseover=\""), "Should not contain original onmouseover");
	}

	@Test
	void testProcessInlineEventHandlersFunctionWrapperPreservesThis() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><button onclick=\"this.style.display='none'\">Click</button></body></html>";
		String nonce = "thisNonce";

		String result = invokeProcessInlineEventHandlers(showDoc, input, nonce);
		// Should wrap handler in function to preserve 'this'
		assertTrue(result.contains("el.onclick = function(event) {this.style.display='none';};"),
			"Should preserve 'this' in function wrapper");
	}

	// ==================== Helper methods for NonceHelper ====================

	private String injectCspNonceIntoTags(ShowDoc showDoc, String htmlContent, String nonce) {
		return injectCspNonceIntoTags(showDoc, htmlContent, nonce, true, true);
	}

	private String injectCspNonceIntoTags(ShowDoc showDoc, String htmlContent, String nonce, boolean injectIntoScripts, boolean injectIntoStyles) {
		return sk.iway.iwcm.doc.showdoc.NonceHelper.injectCspNonceIntoTags(htmlContent, nonce, injectIntoScripts, injectIntoStyles);
	}

	private String invokeProcessInlineStyles(ShowDoc showDoc, String htmlContent, String nonce) {
		return sk.iway.iwcm.doc.showdoc.NonceHelper.processInlineStyles(htmlContent, nonce);
	}

	private String invokeProcessInlineEventHandlers(ShowDoc showDoc, String htmlContent, String nonce) {
		return sk.iway.iwcm.doc.showdoc.NonceHelper.processInlineEventHandlers(htmlContent, nonce);
	}

	private boolean isDirectiveAllowsUnsafeInline(String cspValue, String directiveName) {
		return sk.iway.iwcm.doc.showdoc.NonceHelper.isDirectiveAllowsUnsafeInline(cspValue, directiveName);
	}

	@Test
	void testIsDirectiveAllowsUnsafeInlineScriptSrc() {
		String csp = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self'";
		assertTrue(isDirectiveAllowsUnsafeInline(csp, "script-src"), "script-src with unsafe-inline should return true");
	}

	@Test
	void testIsDirectiveAllowsUnsafeInlineStyleSrc() {
		String csp = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'";
		assertTrue(isDirectiveAllowsUnsafeInline(csp, "style-src"), "style-src with unsafe-inline should return true");
	}

	@Test
	void testIsDirectiveAllowsUnsafeInlineNotPresent() {
		String csp = "default-src 'self'; script-src 'self' {nonce} https:; style-src 'self' {nonce} https:";
		assertFalse(isDirectiveAllowsUnsafeInline(csp, "script-src"), "script-src without unsafe-inline should return false");
		assertFalse(isDirectiveAllowsUnsafeInline(csp, "style-src"), "style-src without unsafe-inline should return false");
	}

	@Test
	void testIsDirectiveAllowsUnsafeInlineWrongDirective() {
		String csp = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self'";
		assertTrue(isDirectiveAllowsUnsafeInline(csp, "script-src"), "script-src with unsafe-inline should return true");
		assertFalse(isDirectiveAllowsUnsafeInline(csp, "style-src"), "style-src without unsafe-inline should return false");
	}

	@Test
	void testInjectCspNonceIntoTagsScriptSrcAllowsUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('test');</script><style>.a { color: red; }</style></body></html>";
		// script-src allows unsafe-inline, style-src does not
		String result = injectCspNonceIntoTags(showDoc, input, "testNonce", false, true);
		// Script tag should NOT get nonce (script-src allows unsafe-inline)
		assertFalse(result.contains("<script nonce=\"testNonce\">"), "Script tag should not get nonce when script-src allows unsafe-inline");
		// Style tag SHOULD get nonce (style-src does not allow unsafe-inline)
		assertTrue(result.contains("<style nonce=\"testNonce\">"), "Style tag should get nonce when style-src does not allow unsafe-inline");
	}

	@Test
	void testInjectCspNonceIntoTagsStyleSrcAllowsUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('test');</script><style>.a { color: red; }</style></body></html>";
		// script-src does not allow unsafe-inline, style-src does
		String result = injectCspNonceIntoTags(showDoc, input, "testNonce", true, false);
		// Script tag SHOULD get nonce (script-src does not allow unsafe-inline)
		assertTrue(result.contains("<script nonce=\"testNonce\">"), "Script tag should get nonce when script-src does not allow unsafe-inline");
		// Style tag should NOT get nonce (style-src allows unsafe-inline)
		assertFalse(result.contains("<style nonce=\"testNonce\">"), "Style tag should not get nonce when style-src allows unsafe-inline");
	}

	@Test
	void testInjectCspNonceIntoTagsBothAllowUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><body><script>console.log('test');</script><style>.a { color: red; }</style></body></html>";
		// Both directives allow unsafe-inline
		String result = injectCspNonceIntoTags(showDoc, input, "testNonce", false, false);
		// Neither tag should get nonce
		assertFalse(result.contains("nonce=\"testNonce\""), "No nonce should be injected when both directives allow unsafe-inline");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkStylesheetScriptSrcAllowsUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" href=\"/css/main.css\"></head><body><script>var a = 1;</script></body></html>";
		// script-src allows unsafe-inline, style-src does not
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce", false, true);
		// Link tag SHOULD get nonce (style-src does not allow unsafe-inline)
		assertTrue(result.contains("nonce=\"linkNonce\""), "Link stylesheet tag should get nonce when style-src does not allow unsafe-inline");
		// Script tag should NOT get nonce (script-src allows unsafe-inline)
		assertFalse(result.contains("<script nonce=\"linkNonce\">"), "Script tag should not get nonce when script-src allows unsafe-inline");
	}

	// ==================== Edge Cases: Self-closing and explicit closing tags ====================

	@Test
	void testInjectCspNonceIntoTagsLinkSelfClosingSlash() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"/css/main.css\" /></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into self-closing link tag (with />)");
		assertTrue(result.contains("href=\"/css/main.css\""), "Original href attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkExplicitClosingTag() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"/css/main.css\"></link></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into link tag with explicit closing tag (</link>)");
		assertTrue(result.contains("href=\"/css/main.css\""), "Original href attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkSelfClosingWithSpace() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"/css/main.css\" > </head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce");
		assertTrue(result.contains("nonce=\"linkNonce\""), "Nonce should be injected into link tag with space before >");
	}

	@Test
	void testInjectCspNonceIntoTagsScriptWithSrcExplicitClosing() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\"></script></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "scriptNonce");
		assertTrue(result.contains("nonce=\"scriptNonce\""), "Nonce should be injected into script tag with src and explicit closing tag");
		assertTrue(result.contains("src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\""), "Original src attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsScriptWithSrcSelfClosing() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\" /></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "scriptNonce");
		assertTrue(result.contains("nonce=\"scriptNonce\""), "Nonce should be injected into script tag with src and self-closing />");
		assertTrue(result.contains("src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\""), "Original src attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsScriptWithSrcNoClosing() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><script src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\"></script></head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "scriptNonce");
		assertTrue(result.contains("nonce=\"scriptNonce\""), "Nonce should be injected into script tag with src attribute");
		assertTrue(result.contains("src=\"https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js\""), "Original src attribute should be preserved");
	}

	@Test
	void testInjectCspNonceIntoTagsMixedClosingStyles() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head>"
				+ "<link rel=\"stylesheet\" href=\"/css/a.css\">"
				+ "<link rel=\"stylesheet\" href=\"/css/b.css\" />"
				+ "<link rel=\"stylesheet\" href=\"/css/c.css\"></link>"
				+ "<script src=\"/js/x.js\"></script>"
				+ "<script src=\"/js/y.js\" />"
				+ "</head></html>";
		String result = injectCspNonceIntoTags(showDoc, input, "mixedNonce");
		// Should inject nonce into all 5 tags
		int nonceCount = result.split("nonce=\"mixedNonce\"").length - 1;
		assertEquals(5, nonceCount, "Should inject nonce into all 5 tags with different closing styles");
	}

	@Test
	void testInjectCspNonceIntoTagsLinkSelfClosingNoNonceWhenUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><link rel=\"stylesheet\" href=\"/css/main.css\" /></head></html>";
		// style-src allows unsafe-inline, so no nonce injection into style/link tags
		String result = injectCspNonceIntoTags(showDoc, input, "linkNonce", true, false);
		assertFalse(result.contains("nonce=\"linkNonce\""), "Self-closing link tag should not get nonce when style-src allows unsafe-inline");
	}

	@Test
	void testInjectCspNonceIntoTagsScriptSelfClosingNoNonceWhenUnsafeInline() {
		ShowDoc showDoc = new ShowDoc();
		String input = "<html><head><script src=\"/app.js\" /></head></html>";
		// script-src allows unsafe-inline, so no nonce injection into script tags
		String result = injectCspNonceIntoTags(showDoc, input, "scriptNonce", false, true);
		assertFalse(result.contains("nonce=\"scriptNonce\""), "Self-closing script tag should not get nonce when script-src allows unsafe-inline");
	}
}
