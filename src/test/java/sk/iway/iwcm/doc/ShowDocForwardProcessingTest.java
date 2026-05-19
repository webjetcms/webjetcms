package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.showdoc.StyleToHeadHelper;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Integration tests for ShowDoc.forwardWithBodyProcessing() method.
 *
 * Verifies that when showDocMoveStyleToHead is enabled the method correctly:
 *  - inserts collected style tags into the head section
 *  - propagates content-type and content-length to the outer response
 *  - re-sends redirects captured by the wrapper to the outer response
 *  - re-sends errors captured by the wrapper to the outer response
 *  - falls back to a plain forward when the feature is disabled
 */
class ShowDocForwardProcessingTest extends BaseWebjetTest {

    private static final String FORWARD_PATH = "/templates/test.jsp";

    private ShowDoc showDoc;
    private Method forwardWithBodyProcessing;

    @BeforeEach
    void setUp() throws Exception {
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();

        showDoc = new ShowDoc();

        forwardWithBodyProcessing = ShowDoc.class.getDeclaredMethod(
                "forwardWithBodyProcessing",
                String.class,
                jakarta.servlet.http.HttpServletRequest.class,
                HttpServletResponse.class);
        forwardWithBodyProcessing.setAccessible(true);
    }

    // ---- helpers ----

    /**
     * Creates a MockHttpServletRequest whose RequestDispatcher writes the given
     * htmlContent to the response when include() is called, and records whether
     * forward() was called instead.
     */
    private static class DispatchCapturingRequest extends MockHttpServletRequest {
        private final String htmlToWrite;
        boolean forwardCalled = false;
        boolean includeCalled = false;

        DispatchCapturingRequest(String htmlToWrite) {
            this.htmlToWrite = htmlToWrite;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return new RequestDispatcher() {
                @Override
                public void forward(ServletRequest req, ServletResponse res)
                        throws IOException, ServletException {
                    forwardCalled = true;
                    if (htmlToWrite != null) {
                        res.getWriter().write(htmlToWrite);
                        res.getWriter().flush();
                    }
                }

                @Override
                public void include(ServletRequest req, ServletResponse res)
                        throws IOException, ServletException {
                    includeCalled = true;
                    if (htmlToWrite != null) {
                        res.getWriter().write(htmlToWrite);
                        res.getWriter().flush();
                    }
                }
            };
        }
    }

    /** Creates a DispatchCapturingRequest whose dispatcher sends a redirect. */
    private static class RedirectDispatchRequest extends MockHttpServletRequest {
        private final String redirectUrl;

        RedirectDispatchRequest(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return new RequestDispatcher() {
                @Override
                public void forward(ServletRequest req, ServletResponse res) {}

                @Override
                public void include(ServletRequest req, ServletResponse res)
                        throws IOException {
                    ((HttpServletResponse) res).sendRedirect(redirectUrl);
                }
            };
        }
    }

    /** Creates a DispatchCapturingRequest whose dispatcher sends an error. */
    private static class ErrorDispatchRequest extends MockHttpServletRequest {
        private final int errorCode;
        private final String errorMessage;

        ErrorDispatchRequest(int errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return new RequestDispatcher() {
                @Override
                public void forward(ServletRequest req, ServletResponse res) {}

                @Override
                public void include(ServletRequest req, ServletResponse res)
                        throws IOException {
                    if (errorMessage != null) {
                        ((HttpServletResponse) res).sendError(errorCode, errorMessage);
                    } else {
                        ((HttpServletResponse) res).sendError(errorCode);
                    }
                }
            };
        }
    }

    private void invoke(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        forwardWithBodyProcessing.invoke(showDoc, FORWARD_PATH, request, response);
    }

    // ---- tests ----

    @Test
    @DisplayName("Styles collected from components are inserted into the head section")
    void testStylesInsertedIntoHead() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        String html = "<html><head><title>Test</title></head><body><p>Content</p></body></html>";
        DispatchCapturingRequest request = new DispatchCapturingRequest(html);

        // Simulate a component having already collected a style tag
        StyleToHeadHelper.extractAndCollectStyles(
                new StringBuilder("<style>.video { color: red; }</style>"),
                request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        String body = response.getContentAsString();
        assertTrue(body.contains(".video { color: red; }"),
                "Style rule should be present in output");
        assertTrue(body.indexOf(".video { color: red; }") < body.indexOf("</head>"),
                "Style should appear before </head>");
        assertTrue(request.includeCalled, "RequestDispatcher.include() should be used when feature is enabled");
    }

    @Test
    @DisplayName("Content-type and content-length are propagated to the outer response")
    void testContentTypeAndContentLengthPropagated() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        String html = "<html><head></head><body>Hello</body></html>";
        DispatchCapturingRequest request = new DispatchCapturingRequest(null) {
            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                return new RequestDispatcher() {
                    @Override
                    public void forward(ServletRequest req, ServletResponse res) {}

                    @Override
                    public void include(ServletRequest req, ServletResponse res)
                            throws IOException {
                        res.setContentType("text/html; charset=UTF-8");
                        res.getWriter().write(html);
                        res.getWriter().flush();
                    }
                };
            }
        };

        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertEquals("text/html; charset=UTF-8", response.getContentType(),
                "Content-type from inner response should be forwarded to outer response");
        assertTrue(response.getContentLength() > 0,
                "Content-length should be set to the byte length of the processed body");
        assertEquals(html.getBytes("UTF-8").length, response.getContentLength(),
                "Content-length must equal the exact byte count of the output");
    }

    @Test
    @DisplayName("Redirect captured by wrapper is re-sent to the outer response")
    void testRedirectIsReSentToOuterResponse() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        RedirectDispatchRequest request = new RedirectDispatchRequest("/new-location");
        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertEquals("/new-location", response.getRedirectedUrl(),
                "Redirect URL must be forwarded to outer response");
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request),
                "Collected styles must be cleared after redirect");
    }

    @Test
    @DisplayName("Error with message captured by wrapper is re-sent to the outer response")
    void testErrorWithMessageIsReSentToOuterResponse() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        ErrorDispatchRequest request = new ErrorDispatchRequest(404, "Not Found");
        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertEquals(404, response.getStatus(),
                "HTTP error status must be forwarded to outer response");
        assertEquals("Not Found", response.getErrorMessage(),
                "Error message must be forwarded to outer response");
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request),
                "Collected styles must be cleared after error");
    }

    @Test
    @DisplayName("Error without message captured by wrapper is re-sent to the outer response")
    void testErrorWithoutMessageIsReSentToOuterResponse() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        ErrorDispatchRequest request = new ErrorDispatchRequest(500, null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertEquals(500, response.getStatus(),
                "HTTP error status must be forwarded to outer response");
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request),
                "Collected styles must be cleared after error");
    }

    @Test
    @DisplayName("When feature is disabled, plain forward is used and content is untouched")
    void testFeatureDisabledUsesPlainForward() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", false);

        String html = "<html><head></head><body><style>.ignored{}</style></body></html>";
        DispatchCapturingRequest request = new DispatchCapturingRequest(html);

        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertTrue(request.forwardCalled, "Standard RequestDispatcher.forward() should be called when feature is disabled");
        assertFalse(request.includeCalled, "RequestDispatcher.include() must NOT be called when feature is disabled");
    }

    @Test
    @DisplayName("When no styles collected, body is written unchanged")
    void testNoStylesCollectedBodyUnchanged() throws Exception {
        Constants.setBoolean("showDocMoveStyleToHead", true);

        String html = "<html><head><title>T</title></head><body><p>Text</p></body></html>";
        DispatchCapturingRequest request = new DispatchCapturingRequest(html);

        MockHttpServletResponse response = new MockHttpServletResponse();
        invoke(request, response);

        assertEquals(html, response.getContentAsString(),
                "Body must be unchanged when no styles were collected");
    }
}
