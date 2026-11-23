package sk.iway.iwcm.components.ai.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.providers.gemini.GeminiService;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;
import sk.iway.iwcm.components.ai.providers.openrouter.OpenRouterService;

class WebjetAiConfigurationServiceTest {

    private static final String DOMAIN_NAME = "customer.example";
    private static final String CONFIGURE_KEY = "ai_testProviderConfiguration";
    private static final String[] MODIFIED_KEYS = {
        OpenAiService.API_KEY,
        GeminiService.API_KEY,
        OpenRouterService.API_KEY,
        DOMAIN_NAME + "-" + OpenAiService.API_KEY,
        DOMAIN_NAME + "-" + CONFIGURE_KEY
    };
    private static final String[] MODIFIED_INT_KEYS = {
        WebjetAiConfigurationService.PROVIDER_CONNECT_TIMEOUT_SECONDS,
        WebjetAiConfigurationService.PROVIDER_RESPONSE_TIMEOUT_SECONDS,
        DOMAIN_NAME + "-" + WebjetAiConfigurationService.PROVIDER_CONNECT_TIMEOUT_SECONDS,
        DOMAIN_NAME + "-" + WebjetAiConfigurationService.PROVIDER_RESPONSE_TIMEOUT_SECONDS
    };

    private final Map<String, String> originalValues = new HashMap<>();
    private final Map<String, Integer> originalIntValues = new HashMap<>();
    private final Set<String> originallyAbsentValues = new HashSet<>();
    private final Set<String> originallyAbsentIntValues = new HashSet<>();
    private WebjetAiConfigurationService service;
    private OpenAiService openAi;
    private GeminiService gemini;
    private OpenRouterService openRouter;
    private boolean originalConstantsAliasSearch;
    private RequestBean originalRequestBean;

    @BeforeEach
    void rememberConstants() {
        service = new WebjetAiConfigurationService();
        AiClient aiClient = org.mockito.Mockito.mock(AiClient.class);
        openAi = new OpenAiService(aiClient, service);
        gemini = new GeminiService(aiClient, service);
        openRouter = new OpenRouterService(aiClient, service);
        originalConstantsAliasSearch = Constants.isConstantsAliasSearch();
        Constants.setConstantsAliasSearch(false);
        for (String key : MODIFIED_KEYS) {
            if (Constants.containsKey(key)) originalValues.put(key, Constants.getString(key));
            else originallyAbsentValues.add(key);
        }
        for (String key : MODIFIED_INT_KEYS) {
            if (Constants.containsKey(key)) originalIntValues.put(key, Constants.getInt(key));
            else originallyAbsentIntValues.add(key);
        }
        Constants.setConstantsAliasSearch(originalConstantsAliasSearch);
        originalRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
    }

    @AfterEach
    void restoreConstants() throws Exception {
        originalValues.forEach(Constants::setString);
        originalIntValues.forEach(Constants::setInt);
        originallyAbsentValues.forEach(Constants::deleteConstant);
        originallyAbsentIntValues.forEach(Constants::deleteConstant);
        Constants.setConstantsAliasSearch(originalConstantsAliasSearch);
        SetCharacterEncodingFilter.setCurrentRequestBean(originalRequestBean);
    }

    @Test
    void mapsExistingConstantsToTypedProviderConfiguration() {
        Constants.setString(OpenAiService.API_KEY, "openai-secret");
        Constants.setString(GeminiService.API_KEY, "gemini-secret");
        Constants.setString(OpenRouterService.API_KEY, "router-secret");
        Constants.setInt(WebjetAiConfigurationService.PROVIDER_CONNECT_TIMEOUT_SECONDS, 7);
        Constants.setInt(WebjetAiConfigurationService.PROVIDER_RESPONSE_TIMEOUT_SECONDS, 0);

        AiProviderConfig openAiConfig = service.resolve(openAi);
        AiProviderConfig geminiConfig = service.resolve(gemini);
        AiProviderConfig openRouterConfig = service.resolve(openRouter);

        assertEquals("openai-secret", openAiConfig.apiKey());
        assertEquals("gemini-secret", geminiConfig.apiKey());
        assertEquals("router-secret", openRouterConfig.apiKey());
        assertEquals("https://www.webjetcms.com/", geminiConfig.trustedHeaders().get("Referer"));
        assertEquals("WebJET CMS", openRouterConfig.trustedHeaders().get("X-Title"));
        assertEquals(7000, openAiConfig.connectTimeoutMillis());
        assertEquals(0, openAiConfig.responseTimeoutMillis());
        assertFalse(openAiConfig.toString().contains("openai-secret"));
    }

    @Test
    void availabilityAndCacheRevisionFollowRuntimeKeyChanges() {
        Constants.setString(OpenAiService.API_KEY, "");
        assertFalse(service.isConfigured(openAi));

        Constants.setString(OpenAiService.API_KEY, "first-secret");
        assertTrue(service.isConfigured(openAi));
        String firstRevision = service.modelCacheDiscriminator(openAi);
        assertEquals(firstRevision, service.modelCacheDiscriminator(openAi));

        Constants.setString(OpenAiService.API_KEY, "second-secret");
        String secondRevision = service.modelCacheDiscriminator(openAi);
        assertNotEquals(firstRevision, secondRevision);
        assertFalse(secondRevision.contains("second-secret"));
    }

    @Test
    void usesOnlySameDomainRefererFromRequest() {
        MockHttpServletRequest sameDomainRequest = new MockHttpServletRequest();
        sameDomainRequest.setServerName("customer.example");
        sameDomainRequest.setSecure(true);
        sameDomainRequest.addHeader("Referer", "https://customer.example/admin/v9/");

        AiProviderConfig geminiConfig = service.resolve(gemini, sameDomainRequest);
        AiProviderConfig openRouterConfig = service.resolve(openRouter, sameDomainRequest);

        assertEquals(
            "https://customer.example/",
            geminiConfig.trustedHeaders().get("Referer")
        );
        assertEquals(
            "https://customer.example/",
            openRouterConfig.trustedHeaders().get("HTTP-Referer")
        );

        MockHttpServletRequest foreignRequest = new MockHttpServletRequest();
        foreignRequest.setServerName("customer.example");
        foreignRequest.setSecure(true);
        foreignRequest.addHeader("Referer", "https://attacker.example/collect");

        AiProviderConfig safeFallback = service.resolve(openRouter, foreignRequest);
        assertEquals(
            "https://customer.example/",
            safeFallback.trustedHeaders().get("HTTP-Referer")
        );
    }

    @Test
    void keepsModelCacheRevisionStableWhenRequestsAlternateDomains() {
        Constants.setString(OpenAiService.API_KEY, "shared-secret");
        MockHttpServletRequest firstDomain = new MockHttpServletRequest();
        firstDomain.setServerName("first.example");
        firstDomain.setSecure(true);
        MockHttpServletRequest secondDomain = new MockHttpServletRequest();
        secondDomain.setServerName("second.example");
        secondDomain.setSecure(true);

        String firstRevision = service.modelCacheDiscriminator(openAi, firstDomain);
        String secondRevision = service.modelCacheDiscriminator(openAi, secondDomain);

        assertNotEquals(firstRevision, secondRevision);
        assertEquals(firstRevision, service.modelCacheDiscriminator(openAi, firstDomain));
        assertEquals(secondRevision, service.modelCacheDiscriminator(openAi, secondDomain));
    }

    @Test
    void resolvesAllConfigurationWithinExplicitDomainAndRestoresRequestContext() {
        Constants.setConstantsAliasSearch(true);
        Constants.setString(OpenAiService.API_KEY, "global-secret");
        Constants.setString(DOMAIN_NAME + "-" + OpenAiService.API_KEY, "domain-secret");
        Constants.setInt(WebjetAiConfigurationService.PROVIDER_CONNECT_TIMEOUT_SECONDS, 2);
        Constants.setInt(DOMAIN_NAME + "-" + WebjetAiConfigurationService.PROVIDER_CONNECT_TIMEOUT_SECONDS, 11);
        Constants.setInt(WebjetAiConfigurationService.PROVIDER_RESPONSE_TIMEOUT_SECONDS, 3);
        Constants.setInt(DOMAIN_NAME + "-" + WebjetAiConfigurationService.PROVIDER_RESPONSE_TIMEOUT_SECONDS, 12);
        Constants.setString(DOMAIN_NAME + "-" + CONFIGURE_KEY, "domain-option");

        RequestBean previousRequestBean = new RequestBean();
        previousRequestBean.setDomain("original.example");
        SetCharacterEncodingFilter.setCurrentRequestBean(previousRequestBean);

        AiInterface provider = org.mockito.Mockito.mock(AiInterface.class);
        org.mockito.Mockito.when(provider.getApiKey())
            .thenAnswer(invocation -> Constants.getString(OpenAiService.API_KEY));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(
                DOMAIN_NAME,
                SetCharacterEncodingFilter.getCurrentRequestBean().getDomain()
            );
            assertEquals("https://customer.example/", invocation.getArgument(1));
            AiProviderConfig.Builder builder = invocation.getArgument(0);
            builder.trustedHeader("X-Domain-Option", Constants.getString(CONFIGURE_KEY));
            return null;
        }).when(provider).configure(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );

        AiProviderConfig config = service.resolveForDomain(provider, DOMAIN_NAME);

        assertEquals("domain-secret", config.apiKey());
        assertEquals(11000, config.connectTimeoutMillis());
        assertEquals(12000, config.responseTimeoutMillis());
        assertEquals("domain-option", config.trustedHeaders().get("X-Domain-Option"));
        assertSame(previousRequestBean, SetCharacterEncodingFilter.getCurrentRequestBean());
    }

    @Test
    void restoresRequestContextWhenDomainConfigurationFails() {
        RequestBean previousRequestBean = new RequestBean();
        previousRequestBean.setDomain("original.example");
        SetCharacterEncodingFilter.setCurrentRequestBean(previousRequestBean);

        AiInterface provider = org.mockito.Mockito.mock(AiInterface.class);
        org.mockito.Mockito.when(provider.getApiKey()).thenReturn("secret");
        org.mockito.Mockito.doThrow(new IllegalStateException("Configuration failed"))
            .when(provider)
            .configure(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
            );

        assertThrows(
            IllegalStateException.class,
            () -> service.resolveForDomain(provider, DOMAIN_NAME)
        );
        assertSame(previousRequestBean, SetCharacterEncodingFilter.getCurrentRequestBean());
    }
}
