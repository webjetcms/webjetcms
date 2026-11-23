package sk.iway.iwcm.system.spring.webjet_component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartResolver;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;
import sk.iway.iwcm.users.UsersDB;

/** Verifies that a failed Spring application only replaces its own include in a page block. */
class WebjetComponentParserTest {
    private static final String APP_CLASS = FailingApp.class.getName();
    private static final String APP_INCLUDE = "!INCLUDE(" + APP_CLASS + ", cacheMinutes=5)!";
    private static final String ERROR_MESSAGE = "Error in Test application";
    private static final String JSP_INCLUDE = "!INCLUDE(/components/slider/slider.jsp, skin=Events)!";

    private final List<MockedStatic<?>> staticMocks = new ArrayList<>();
    private WebjetComponentParser parser;
    private WebjetComponentInterface failingApp;
    private Cache cache;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockedStatic<UsersDB> users;
    private MockedStatic<Adminlog> audit;

    @BeforeEach
    void setUp() {
        parser = new WebjetComponentParser();
        parser.context = mock(ApplicationContext.class);
        parser.componentResolver = mock(WebjetComponentResolver.class);
        request = new MockHttpServletRequest();
        request.setParameter("docid", "42");
        response = new MockHttpServletResponse();
        failingApp = register(APP_CLASS);
        cache = mock(Cache.class);

        MockedStatic<Constants> constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(Constants::getInstallName).thenReturn("test");
        mockStatic(Cache.class).when(Cache::getInstance).thenReturn(cache);
        mockStatic(PageLng.class).when(() -> PageLng.getUserLng(any())).thenReturn("en");
        users = mockStatic(UsersDB.class);
        audit = mockStatic(Adminlog.class);
        mockStatic(Logger.class);
        mockStatic(ExecutionTimeMonitor.class);

        Prop prop = mock(Prop.class);
        when(prop.getText("test.application.title")).thenReturn("Test application");
        when(prop.getText(eq("writetag.error"), anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> "Error in " + invocation.getArgument(3));
        mockStatic(Prop.class).when(() -> Prop.getInstance(any(MockHttpServletRequest.class))).thenReturn(prop);
    }

    @AfterEach
    void tearDown() {
        staticMocks.forEach(MockedStatic::close);
    }

    /** Bean creation and rendering failures preserve text, JSP includes and other Spring applications. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesPageContentWhenAnApplicationFails(boolean beanCreationFailure) throws Exception {
        WebjetComponentInterface before = register("example.BeforeApp");
        WebjetComponentInterface after = register("example.AfterApp");
        when(parser.componentResolver.render(same(request), same(response), same(before), any())).thenReturn("<b>Before</b>");
        when(parser.componentResolver.render(same(request), same(response), same(after), any())).thenReturn("<b>After</b>");
        if (beanCreationFailure) {
            when(parser.context.getBean(APP_CLASS, WebjetComponentInterface.class))
                .thenThrow(new BeanCreationException(APP_CLASS, "Cannot create application"));
        } else {
            when(parser.componentResolver.render(same(request), same(response), same(failingApp), any()))
                .thenThrow(new IllegalStateException("Cannot render application"));
        }
        String html = "<p>First application:</p>" + JSP_INCLUDE
            + "!INCLUDE(example.BeforeApp)!<p>Second application:</p>" + APP_INCLUDE
            + "!INCLUDE(example.AfterApp)!<p>End</p>";

        assertEquals("<p>First application:</p>" + JSP_INCLUDE
            + "<b>Before</b><p>Second application:</p>" + ERROR_MESSAGE + "<b>After</b><p>End</p>",
            parser.parse(request, response, html));
        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_JSPERROR),
            eq("ERROR: " + APP_INCLUDE + "\n\n" + (beanCreationFailure
                ? new BeanCreationException(APP_CLASS, "Cannot create application").getMessage()
                : "Cannot render application")), eq(-1), eq(-1)));
    }

    /** A failed render is retried on the next request and only successful output is cached. */
    @Test
    void doesNotCacheApplicationErrors() throws Exception {
        when(parser.componentResolver.render(same(request), same(response), same(failingApp), any()))
            .thenThrow(new IllegalStateException("Temporary failure"))
            .thenReturn("<b>Recovered</b>");

        assertEquals(ERROR_MESSAGE, parser.parse(request, response, APP_INCLUDE));
        verify(cache, never()).setObjectSeconds(anyString(), any(), anyInt(), eq(true));
        assertEquals("<b>Recovered</b>", parser.parse(request, response, APP_INCLUDE));
        verify(cache).setObjectSeconds(anyString(), eq("<b>Recovered</b>"), eq(300), eq(true));
    }

    /** Detailed errors remain restricted to administrators and respect the suppression attribute. */
    @ParameterizedTest
    @CsvSource({"false, false, false", "true, false, true", "true, true, false"})
    void preservesErrorDetailVisibility(boolean admin, boolean suppressDetails, boolean showDetails) throws Exception {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(admin);
        users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
        if (suppressDetails) request.setAttribute("writeTagDontShowError", true);
        when(parser.componentResolver.render(same(request), same(response), same(failingApp), any()))
            .thenThrow(new IllegalStateException("Internal <detail>"));

        String result = parser.parse(request, response, "<p>Before</p>" + APP_INCLUDE + "<p>After</p>");

        assertTrue(result.startsWith("<p>Before</p>" + ERROR_MESSAGE));
        assertTrue(result.endsWith("<p>After</p>"));
        assertEquals(showDetails, result.contains("component-error"));
        assertEquals(showDetails, result.contains("Internal &lt;detail&gt;"));
        assertFalse(result.contains("Internal <detail>"));
    }

    /** Multipart resources are released both after a failed application and an early redirect. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleansUpMultipartRequest(boolean redirect) throws Exception {
        MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
        MultipartResolver multipartResolver = mock(MultipartResolver.class);
        ReflectionTestUtils.setField(parser, "multipartResolver", multipartResolver);
        when(parser.componentResolver.render(same(multipartRequest), same(response), same(failingApp), any()))
            .thenAnswer(invocation -> {
                if (redirect) {
                    response.sendRedirect("/destination/");
                    return "";
                }
                throw new IllegalStateException("Cannot render upload application");
            });

        assertEquals(redirect ? "" : ERROR_MESSAGE, parser.parse(multipartRequest, response, APP_INCLUDE));
        verify(multipartResolver).cleanupMultipart(multipartRequest);
    }

    private WebjetComponentInterface register(String name) {
        WebjetComponentInterface component = mock(WebjetComponentInterface.class);
        when(parser.context.containsBean(name)).thenReturn(true);
        when(parser.context.getBean(name, WebjetComponentInterface.class)).thenReturn(component);
        return component;
    }

    private <T> MockedStatic<T> mockStatic(Class<T> type) {
        MockedStatic<T> mocked = Mockito.mockStatic(type);
        staticMocks.add(mocked);
        return mocked;
    }

    @WebjetAppStore(nameKey = "test.application.title")
    static class FailingApp extends WebjetComponentAbstract {
    }
}
