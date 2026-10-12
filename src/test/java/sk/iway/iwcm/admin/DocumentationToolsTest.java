package sk.iway.iwcm.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;

/** Verifies documentation origins and language mappings for public and locally hosted documentation. */
class DocumentationToolsTest {

    /** Extracts the server from valid search URLs, including custom ports, local hostnames and IPv6 addresses. */
    @ParameterizedTest
    @CsvSource({
        "https://docs.webjetcms.sk/rest/rag/markdown/search, https://docs.webjetcms.sk",
        "https://docs.example.test/rest/search?lng=sk#result, https://docs.example.test",
        "http://docs-intranet:8080/search/, http://docs-intranet:8080",
        "https://localhost:8443/docs/search, https://localhost:8443",
        "http://[::1]:8080/search, http://[::1]:8080"
    })
    void extractsConfiguredOrigin(String endpoint, String expected) {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("adminDocumentationSearchUrl")).thenReturn(endpoint);
            assertEquals(expected, DocumentationTools.getBaseUrl());
        }
    }

    /** Each call reads the current endpoint so customer domain changes take effect immediately. */
    @Test
    void readsConfiguredEndpointOnEachCall() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("adminDocumentationSearchUrl"))
                .thenReturn("http://manual.example.test:8081/manual/rest/rag/markdown/search",
                    "https://new-manual.example.test/rest/rag/markdown/search");
            assertEquals("http://manual.example.test:8081", DocumentationTools.getBaseUrl());
            assertEquals("https://new-manual.example.test", DocumentationTools.getBaseUrl());
        }
    }

    /** Czech administration codes share one translation and unsupported languages use English. */
    @ParameterizedTest
    @CsvSource({ "sk, sk", "cz, cs", "cs, cs", "en, en", "de, en", "hu, en", "'', en" })
    void mapsDocumentationLanguages(String language, String expected) {
        assertEquals(expected, DocumentationTools.normalizeLanguage(language));
    }
}
