package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;

class ShowDocXssRedirectTest {

    private MockedStatic<Constants> constants;
    private MockedStatic<Adminlog> adminlog;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        adminlog = mockStatic(Adminlog.class);
        constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getBoolean("xssProtection")).thenReturn(true);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(() -> Constants.getString("xssTestValues")).thenReturn("<script");

        request = new MockHttpServletRequest("POST", "/");
        request.setAttribute("path_filter_orig_path", "/");
    }

    @AfterEach
    void tearDown() {
        if (constants != null) constants.close();
        if (adminlog != null) adminlog.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 10, 13, 31, 127, 133, 159})
    void dropsParameterNamesContainingControlCharacters(int controlCharacter) {
        request.addParameter("bad" + (char) controlCharacter + "name", "value");
        request.addParameter("keep", "safe");

        assertEquals("/?keep=safe", ShowDoc.getXssRedirectUrl(request));
    }

    @Test
    void dropsEmbeddedHttpRequestParameter() {
        request.addParameter(
            "GET /sitemap.xml HTTP/1.1\r\nHost: demo.webjetcms.sk\r\nCache-Control: max-age",
            "0\r\nSec-Ch-Ua: \"Chromium\";v=\"150\"");

        assertEquals("/?", ShowDoc.getXssRedirectUrl(request));
    }

    @Test
    void allowsSpacesInParameterNamesWithoutRedirecting() {
        request.addParameter("first name", "Peter");

        assertNull(ShowDoc.getXssRedirectUrl(request));
    }

    @ParameterizedTest
    @CsvSource({
        "first name, first%20name",
        "field+name, field%2Bname",
        "a&b=c#d, a%26b%3Dc%23d",
        "items[0].name, items%5B0%5D.name",
        "literal%0A, literal%250A"
    })
    void encodesNamesForBothRetainedAndRejectedValues(String name, String encodedName) {
        request.addParameter("bad\rname", "value");
        request.addParameter(name, "safe value");

        assertEquals("/?" + encodedName + "=safe%20value", ShowDoc.getXssRedirectUrl(request));

        request.setParameter(name, "<script>alert(1)</script>");

        assertEquals("/?" + encodedName + "=", ShowDoc.getXssRedirectUrl(request));
    }
}
