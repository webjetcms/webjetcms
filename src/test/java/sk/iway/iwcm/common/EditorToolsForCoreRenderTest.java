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
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.spring.webjet_component.WebjetComponentParser;
import sk.iway.iwcm.system.spring.webjet_component.WebjetComponentResolver;

class EditorToolsForCoreRenderTest {

    private RequestAttributes originalAttributes;

    @BeforeEach
    void isolateSpringRequestContext() {
        originalAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void restoreSpringRequestContext() {
        RequestContextHolder.setRequestAttributes(originalAttributes);
    }

    @Configuration
    static class SpringApps {
        @Bean
        @RequestScope
        public WebjetComponentParser webjetComponentParser() {
            return new WebjetComponentParser();
        }

        @Bean
        @RequestScope
        public SpringApp springApp(javax.servlet.http.HttpServletRequest request, HttpServletResponse response) {
            return new SpringApp(request, response);
        }
    }

    static class SpringApp implements DisposableBean {
        private final javax.servlet.http.HttpServletRequest request;
        private final HttpServletResponse response;

        SpringApp(javax.servlet.http.HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        public void render() throws IOException {
            response.getWriter().write("<p>" + request.getAttribute("message") + "</p>");
        }

        @Override
        public void destroy() {
            ((AtomicInteger) request.getAttribute("destroyedApps")).incrementAndGet();
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void rendersRequestScopedSpringAppsAndRestoresCallerContext(boolean fail, boolean hasCallerContext) throws IOException {
        AtomicInteger destroyedApps = new AtomicInteger();
        AtomicInteger destroyedCallerApps = new AtomicInteger();
        try (AnnotationConfigWebApplicationContext spring = new AnnotationConfigWebApplicationContext()) {
            RequestDispatcher dispatcher = new RequestDispatcher() {
                @Override
                public void forward(ServletRequest req, ServletResponse res) throws ServletException {
                    throw new ServletException("Only an include is expected");
                }

                @Override
                public void include(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    assertSame(req, attributes.getRequest());
                    WebjetComponentParser parser = spring.getBean("webjetComponentParser", WebjetComponentParser.class);
                    assertNull(parser.getRedirectLocation((HttpServletResponse) res));
                    spring.getBean(SpringApp.class).render();
                    if (fail) throw new ServletException("Application rendering failed");
                }
            };
            MockServletContext servletContext = new MockServletContext() {
                @Override
                public RequestDispatcher getRequestDispatcher(String path) {
                    assertEquals("/components/_common/fulltext_preview.jsp", path);
                    return dispatcher;
                }
            };
            spring.setServletContext(servletContext);
            spring.register(SpringApps.class);
            spring.addBeanFactoryPostProcessor(factory -> {
                factory.registerSingleton("componentResolver", org.mockito.Mockito.mock(WebjetComponentResolver.class));
                factory.registerSingleton("multipartResolver", org.mockito.Mockito.mock(MultipartResolver.class));
            });
            spring.refresh();

            ServletRequestAttributes callerAttributes = null;
            MockHttpServletResponse callerResponse = new MockHttpServletResponse();
            if (hasCallerContext) {
                MockHttpServletRequest callerRequest = new MockHttpServletRequest();
                callerRequest.setAttribute("message", "Admin request");
                callerRequest.setAttribute("destroyedApps", destroyedCallerApps);
                callerAttributes = new ServletRequestAttributes(callerRequest, callerResponse);
                RequestContextHolder.setRequestAttributes(callerAttributes);
                spring.getBean(SpringApp.class).render();
            }

            try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                    MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
                constants.when(() -> Constants.getString(anyString())).thenReturn("");
                constants.when(Constants::getServletContext).thenReturn(servletContext);
                tools.when(() -> Tools.downloadUrl(anyString())).thenThrow(new AssertionError("Loopback must not be used"));
                tools.clearInvocations();

                for (String message : List.of("First indexed page", "Second indexed page")) {
                    net.sourceforge.stripes.mock.MockHttpServletRequest request =
                            new net.sourceforge.stripes.mock.MockHttpServletRequest("", "/indexed-page.html");
                    request.setSession(new net.sourceforge.stripes.mock.MockHttpSession(servletContext));
                    request.setAttribute("message", message);
                    request.setAttribute("destroyedApps", destroyedApps);

                    String result = EditorToolsForCore.renderIncludes("source", false, request);
                    if (fail) assertNull(result);
                    else assertEquals("<p>" + message + "</p>", result);
                    assertSame(callerAttributes, RequestContextHolder.getRequestAttributes());
                    assertEquals(0, destroyedCallerApps.get(), "The caller's Spring request scope must remain active");
                }
                assertEquals(2, destroyedApps.get(), "Each temporary Spring request scope must be completed");
                if (hasCallerContext) assertEquals("<p>Admin request</p>", callerResponse.getContentAsString());
                tools.verify(() -> Tools.downloadUrl(anyString()), never());
            } finally {
                if (callerAttributes != null) callerAttributes.requestCompleted();
            }
            if (hasCallerContext) assertEquals(1, destroyedCallerApps.get());
        }
    }

    @Test
    void preservesAnExistingSpringRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                return org.mockito.Mockito.mock(RequestDispatcher.class);
            }
        };
        ServletRequestAttributes attributes = new ServletRequestAttributes(request, new MockHttpServletResponse());
        AtomicInteger completed = new AtomicInteger();
        attributes.registerDestructionCallback("existing", completed::incrementAndGet, RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attributes);
        try {
            EditorToolsForCore.renderIncludes("source", false, request);
            assertSame(attributes, RequestContextHolder.getRequestAttributes());
            assertEquals(0, completed.get());
        } finally {
            attributes.requestCompleted();
        }
    }

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
