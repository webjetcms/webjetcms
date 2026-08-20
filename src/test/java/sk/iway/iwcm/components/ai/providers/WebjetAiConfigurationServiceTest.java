package sk.iway.iwcm.components.ai.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.webjetcms.ai.AiProviderConfig;

import sk.iway.iwcm.Constants;

class WebjetAiConfigurationServiceTest {

    private static final String[] MODIFIED_KEYS = {
        WebjetAiConfigKeys.OPENAI_API_KEY,
        WebjetAiConfigKeys.GEMINI_API_KEY,
        WebjetAiConfigKeys.OPENROUTER_API_KEY
    };

    private final WebjetAiConfigurationService service = new WebjetAiConfigurationService();
    private final Map<String, String> originalValues = new HashMap<>();
    private final Map<String, Integer> originalIntValues = new HashMap<>();

    @BeforeEach
    void rememberConstants() {
        for (String key : MODIFIED_KEYS) {
            originalValues.put(key, Constants.getString(key));
        }
        originalIntValues.put(
            WebjetAiConfigKeys.PROVIDER_CONNECT_TIMEOUT_SECONDS,
            Constants.getInt(WebjetAiConfigKeys.PROVIDER_CONNECT_TIMEOUT_SECONDS)
        );
        originalIntValues.put(
            WebjetAiConfigKeys.PROVIDER_RESPONSE_TIMEOUT_SECONDS,
            Constants.getInt(WebjetAiConfigKeys.PROVIDER_RESPONSE_TIMEOUT_SECONDS)
        );
    }

    @AfterEach
    void restoreConstants() {
        originalValues.forEach(Constants::setString);
        originalIntValues.forEach(Constants::setInt);
    }

    @Test
    void mapsExistingConstantsToTypedProviderConfiguration() {
        Constants.setString(WebjetAiConfigKeys.OPENAI_API_KEY, "openai-secret");
        Constants.setString(WebjetAiConfigKeys.GEMINI_API_KEY, "gemini-secret");
        Constants.setString(WebjetAiConfigKeys.OPENROUTER_API_KEY, "router-secret");
        Constants.setInt(WebjetAiConfigKeys.PROVIDER_CONNECT_TIMEOUT_SECONDS, 7);
        Constants.setInt(WebjetAiConfigKeys.PROVIDER_RESPONSE_TIMEOUT_SECONDS, 0);

        AiProviderConfig openAi = service.resolve("openai");
        AiProviderConfig gemini = service.resolve("gemini");
        AiProviderConfig openRouter = service.resolve("openrouter");

        assertEquals("openai-secret", openAi.apiKey());
        assertEquals("gemini-secret", gemini.apiKey());
        assertEquals("router-secret", openRouter.apiKey());
        assertEquals("https://www.webjetcms.com/", gemini.trustedHeaders().get("Referer"));
        assertEquals("WebJET CMS", openRouter.trustedHeaders().get("X-Title"));
        assertEquals(7000, openAi.connectTimeoutMillis());
        assertEquals(0, openAi.responseTimeoutMillis());
        assertFalse(openAi.toString().contains("openai-secret"));
    }

    @Test
    void availabilityAndCacheRevisionFollowRuntimeKeyChanges() {
        Constants.setString(WebjetAiConfigKeys.OPENAI_API_KEY, "");
        assertFalse(service.isConfigured("openai"));

        Constants.setString(WebjetAiConfigKeys.OPENAI_API_KEY, "first-secret");
        assertTrue(service.isConfigured("openai"));
        String firstRevision = service.modelCacheDiscriminator("openai");
        assertEquals(firstRevision, service.modelCacheDiscriminator("openai"));

        Constants.setString(WebjetAiConfigKeys.OPENAI_API_KEY, "second-secret");
        String secondRevision = service.modelCacheDiscriminator("openai");
        assertNotEquals(firstRevision, secondRevision);
        assertFalse(secondRevision.contains("second-secret"));
    }

    @Test
    void usesOnlySameDomainRefererFromRequest() {
        MockHttpServletRequest sameDomainRequest = new MockHttpServletRequest();
        sameDomainRequest.setServerName("customer.example");
        sameDomainRequest.setSecure(true);
        sameDomainRequest.addHeader("Referer", "https://customer.example/admin/v9/");

        AiProviderConfig gemini = service.resolve("gemini", sameDomainRequest);
        AiProviderConfig openRouter = service.resolve("openrouter", sameDomainRequest);

        assertEquals(
            "https://customer.example/",
            gemini.trustedHeaders().get("Referer")
        );
        assertEquals(
            "https://customer.example/",
            openRouter.trustedHeaders().get("HTTP-Referer")
        );

        MockHttpServletRequest foreignRequest = new MockHttpServletRequest();
        foreignRequest.setServerName("customer.example");
        foreignRequest.setSecure(true);
        foreignRequest.addHeader("Referer", "https://attacker.example/collect");

        AiProviderConfig safeFallback = service.resolve("openrouter", foreignRequest);
        assertEquals(
            "https://customer.example/",
            safeFallback.trustedHeaders().get("HTTP-Referer")
        );
    }

    @Test
    void keepsModelCacheRevisionStableWhenRequestsAlternateDomains() {
        Constants.setString(WebjetAiConfigKeys.OPENAI_API_KEY, "shared-secret");
        MockHttpServletRequest firstDomain = new MockHttpServletRequest();
        firstDomain.setServerName("first.example");
        firstDomain.setSecure(true);
        MockHttpServletRequest secondDomain = new MockHttpServletRequest();
        secondDomain.setServerName("second.example");
        secondDomain.setSecure(true);

        String firstRevision = service.modelCacheDiscriminator("openai", firstDomain);
        String secondRevision = service.modelCacheDiscriminator("openai", secondDomain);

        assertNotEquals(firstRevision, secondRevision);
        assertEquals(firstRevision, service.modelCacheDiscriminator("openai", firstDomain));
        assertEquals(secondRevision, service.modelCacheDiscriminator("openai", secondDomain));
    }
}
