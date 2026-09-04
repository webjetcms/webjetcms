package sk.iway.iwcm.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.Model;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

class LicenseControllerTest {

    private static final String ADMIN_IP_PREFIX = "192.0.2.";
    private static final String DENIED_IP = "198.51.100.25";

    @Test
    void getFromAddressOutsideConfiguredAdminIpsReturnsNotFound() {
        MockHttpServletRequest request = requestFromDeniedIp();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = mock(Model.class);

        try (MockedStatic<Constants> constants = configuredAdminIps();
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<SetupCompletionState> completionState = mockStatic(SetupCompletionState.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            String view = new LicenseController().setup(model, request, response);

            assertNull(view);
            assertEquals(MockHttpServletResponse.SC_NOT_FOUND, response.getStatus());
            completionState.verify(
                () -> SetupCompletionState.rejectIfCompleted(request, response), never()
            );
            initServlet.verify(InitServlet::isValid, never());
        }
    }

    @Test
    void postFromAddressOutsideConfiguredAdminIpsReturnsNotFound() {
        MockHttpServletRequest request = requestFromDeniedIp();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = mock(Model.class);

        try (MockedStatic<Constants> constants = configuredAdminIps();
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<SetupCompletionState> completionState = mockStatic(SetupCompletionState.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            String view = new LicenseController().save(
                new LicenseFormBean(), model, request, response
            );

            assertNull(view);
            assertEquals(MockHttpServletResponse.SC_NOT_FOUND, response.getStatus());
            completionState.verify(
                () -> SetupCompletionState.tryStart(request, response), never()
            );
            initServlet.verify(InitServlet::isValid, never());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN_IP_PREFIX, "" })
    void allowedAdminIpConfigurationContinuesToControllerStateCheck(String adminEnableIps) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = mock(Model.class);

        try (MockedStatic<Constants> constants = configuredAdminIps(adminEnableIps);
                MockedStatic<SetupCompletionState> completionState = mockStatic(SetupCompletionState.class)) {
            completionState.when(() -> SetupCompletionState.rejectIfCompleted(request, response))
                .thenReturn(true);

            String view = new LicenseController().setup(model, request, response);

            assertNull(view);
            assertEquals(MockHttpServletResponse.SC_OK, response.getStatus());
            completionState.verify(() -> SetupCompletionState.rejectIfCompleted(request, response));
            verify(model, never()).addAttribute("isLicense", true);
        }
    }

    @Test
    void configuredAdminIpsAreRecheckedWhenSessionContinuesFromAnotherAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");
        request.getSession();
        Model model = mock(Model.class);
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();

        try (MockedStatic<Constants> constants = configuredAdminIps();
                MockedStatic<SetupCompletionState> completionState = mockStatic(SetupCompletionState.class)) {
            completionState.when(() -> SetupCompletionState.rejectIfCompleted(request, allowedResponse))
                .thenReturn(true);

            assertNull(new LicenseController().setup(model, request, allowedResponse));
            assertEquals(MockHttpServletResponse.SC_OK, allowedResponse.getStatus());

            request.setRemoteAddr(DENIED_IP);
            MockHttpServletResponse deniedResponse = new MockHttpServletResponse();

            assertNull(new LicenseController().save(
                new LicenseFormBean(), model, request, deniedResponse
            ));
            assertEquals(MockHttpServletResponse.SC_NOT_FOUND, deniedResponse.getStatus());
            completionState.verify(
                () -> SetupCompletionState.tryStart(request, deniedResponse), never()
            );
        }
    }

    private MockHttpServletRequest requestFromDeniedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(DENIED_IP);
        return request;
    }

    private MockedStatic<Constants> configuredAdminIps() {
        return configuredAdminIps(ADMIN_IP_PREFIX);
    }

    private MockedStatic<Constants> configuredAdminIps(String adminEnableIps) {
        MockedStatic<Constants> constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString("clusterMyNodeType")).thenReturn("admin");
        constants.when(() -> Constants.getString("adminEnableIPs")).thenReturn(adminEnableIps);
        return constants;
    }
}
