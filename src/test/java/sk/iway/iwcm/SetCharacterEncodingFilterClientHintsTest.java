package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifies the HTTPS opt-in needed before browsers send detailed platform hints. */
class SetCharacterEncodingFilterClientHintsTest {
    /** HTTPS responses request the platform version while preserving other requested hints. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void requestsPlatformVersionOnlyForSecureConnections(boolean secure) {
        var request = new MockHttpServletRequest("GET", "/admin/logon/");
        var response = new MockHttpServletResponse();
        response.setHeader("Accept-CH", "Sec-CH-UA-Arch");
        try (var paths = mockStatic(PathFilter.class); var tools = mockStatic(Tools.class);
             var constants = mockStatic(Constants.class)) {
            tools.when(() -> Tools.isSecure(request)).thenReturn(secure);
            SetCharacterEncodingFilter.setCommonHeaders(response, request);
        }

        assertEquals(secure ? List.of("Sec-CH-UA-Arch", "Sec-CH-UA-Platform-Version") : List.of("Sec-CH-UA-Arch"),
            List.copyOf(response.getHeaders("Accept-CH")));
        assertNull(response.getHeader("Critical-CH"));
    }
}
