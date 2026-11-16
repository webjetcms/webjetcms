package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.spring.webjet_component.WebjetComponentParser;

class EditorToolsForCoreRenderTest {

    @Test
    void rendersComponentAndReadsResponseHeadersInTheSameRequestWithoutLoopback() {
        Thread caller = Thread.currentThread();
        WebjetComponentParser parser = new WebjetComponentParser();
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                assertEquals("/components/_common/fulltext_preview.jsp", path);
                return new RequestDispatcher() {
                    @Override
                    public void forward(ServletRequest req, ServletResponse res) throws ServletException {
                        throw new ServletException("Only an include is expected");
                    }

                    @Override
                    public void include(ServletRequest req, ServletResponse res) throws IOException {
                        assertSame(caller, Thread.currentThread());
                        assertSame(requestInstance(), req);
                        HttpServletResponse response = (HttpServletResponse) res;
                        assertNull(parser.getRedirectLocation(response));
                        response.setHeader("Location", "/redirect-target");
                        assertEquals("/redirect-target", parser.getRedirectLocation(response));
                        assertEquals("/redirect-target", response.getHeader("location"));
                        response.setHeader("Location", null);
                        assertNull(parser.getRedirectLocation(response));
                        response.setHeader("X-Component", "first");
                        response.addHeader("X-Component", "second");
                        assertEquals(List.of("first", "second"), List.copyOf(response.getHeaders("x-component")));
                        assertEquals(List.of(), List.copyOf(response.getHeaders("missing")));
                        assertTrue(response.getHeaderNames().containsAll(List.of("Location", "X-Component")));
                        response.getWriter().write("<p>Rendered component output</p>");
                    }
                };
            }

            private ServletRequest requestInstance() {
                return this;
            }
        };

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString(anyString())).thenReturn("");
            tools.when(() -> Tools.downloadUrl(anyString())).thenThrow(new AssertionError("Loopback must not be used"));
            tools.clearInvocations();

            assertEquals("<p>Rendered component output</p>", EditorToolsForCore.renderIncludes("source", false, request));
            assertNull(request.getAttribute("renderingIncludes"));
            tools.verify(() -> Tools.downloadUrl(anyString()), never());
        }
    }
}
