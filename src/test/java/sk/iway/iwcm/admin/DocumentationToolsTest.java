package sk.iway.iwcm.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConstantsV9;

/** Verifies documentation origins and language mappings for public and locally hosted documentation. */
class DocumentationToolsTest {

    /** Registers the same configuration defaults as application startup. */
    @BeforeAll
    static void registerDefaultConfiguration() {
        ConstantsV9.clearValuesWebJet9();
    }

    /** Keeps only scheme, host and port, including local hostnames and IPv6 addresses. */
    @ParameterizedTest
    @CsvSource({
        "https://docs.example.test/rest/search?lng=sk#result, https://docs.example.test",
        "http://docs-intranet:8080/search/, http://docs-intranet:8080",
        "https://localhost:8443/docs/search, https://localhost:8443",
        "http://[::1]:8080/search, http://[::1]:8080",
        "https://user:secret@docs.example.test/search, https://docs.example.test",
        "'  HTTPS://docs.example.test/search  ', https://docs.example.test"
    })
    void extractsConfiguredOrigin(String endpoint, String expected) {
        assertEquals(expected, DocumentationTools.resolveBaseUrl(endpoint, null));
    }

    /** Invalid configuration must not produce unsafe protocols or broken documentation links. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "not a url", "javascript:alert(1)", "ftp://docs.example.test/search",
        "https:///search", "//docs.example.test/search", "/rest/search", "https://docs.example.test:65536/search" })
    void fallsBackToPublicDocumentation(String endpoint) {
        assertEquals("https://docs.webjetcms.sk", DocumentationTools.resolveBaseUrl(endpoint, null));
    }

    /** Both entry points read the current endpoint so customer domain changes take effect immediately. */
    @Test
    void readsConfiguredEndpointOnEachCall() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("adminDocumentationSearchUrl"))
                .thenReturn("http://manual.example.test:8081/manual/rest/rag/markdown/search",
                    "https://new-manual.example.test/rest/rag/markdown/search");
            assertEquals("http://manual.example.test:8081", DocumentationTools.getBaseUrl());
            assertEquals("https://new-manual.example.test", DocumentationTools.getBaseUrl(null));
        }
    }

    /** The fallback origin comes from the registered endpoint default instead of a separate domain constant. */
    @Test
    void derivesFallbackFromRegisteredDefaultEndpoint() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(Constants::getAllValues).thenReturn(List.of(new ConfDetails("adminDocumentationSearchUrl",
                "https://default-manual.example.test:8443/manual/rest/rag/markdown/search")));
            assertEquals("https://default-manual.example.test:8443", DocumentationTools.resolveBaseUrl("", null));
            assertEquals("https://default-manual.example.test:8443", DocumentationTools.resolveBaseUrl("not a url", null));
        }
    }

    /** A root-relative search endpoint uses the request origin and ignores its deployment context path. */
    @Test
    void resolvesRootRelativeEndpointAgainstCurrentRequest() {
        RequestBean request = new RequestBean();
        request.setBaseHref("https://cms.example.test:8443/webjet");
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            constants.when(() -> Constants.getString("adminDocumentationSearchUrl")).thenReturn("/rest/search");
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(request);
            assertEquals("https://cms.example.test:8443", DocumentationTools.getBaseUrl());
        }
    }

    /** Czech administration codes share one translation and unsupported languages use English. */
    @ParameterizedTest
    @CsvSource({ "sk, sk", "cz, cs", "cs, cs", "en, en", "de, en", "hu, en", "'', en" })
    void mapsDocumentationLanguages(String language, String expected) {
        assertEquals(expected, DocumentationTools.normalizeLanguage(language));
    }
}
