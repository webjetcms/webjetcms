package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.SetCharacterEncodingFilter;

class SetupCharacterEncodingFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "/login", "/logout", "/wjerrorpages/setup/setup", "/wjerrorpages/setup/perform-setup",
        "/wjerrorpages/setup/license", "/wjerrorpages/setup/save-license"
    })
    void setupAuthenticationEndpointsReachTheServletAndSecurityChains(String path) throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        var previousServletContext = Constants.getServletContext();

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            Constants.setServletContext(servletContext);
            initServlet.when(InitServlet::isWebjetInitialized).thenReturn(false);

            new SetCharacterEncodingFilter().doFilterInternal(
                request, response, (chainRequest, chainResponse) -> { }
            );
        } finally {
            SetCharacterEncodingFilter.unRegisterDataContext();
            Constants.setServletContext(previousServletContext);
        }

        assertEquals(path, response.getForwardedUrl());
        assertTrue(response.getContentAsString().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/admin/", "/products/example"})
    void uninitializedWebjetReturnsTheExistingServiceUnavailablePage(String path) throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        var previousServletContext = Constants.getServletContext();

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            Constants.setServletContext(servletContext);
            initServlet.when(InitServlet::isWebjetInitialized).thenReturn(false);

            new SetCharacterEncodingFilter().doFilterInternal(
                request, response, (chainRequest, chainResponse) -> chainCalled.set(true)
            );
        } finally {
            SetCharacterEncodingFilter.unRegisterDataContext();
            Constants.setServletContext(previousServletContext);
        }

        assertEquals(503, response.getStatus());
        assertFalse(response.getContentAsString().isEmpty());
        assertFalse(chainCalled.get());
    }
}
