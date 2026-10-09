package sk.iway.iwcm.rag.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.rag.search.MarkdownSearchService;
import sk.iway.iwcm.rag.search.RagService;
import sk.iway.iwcm.rag.search.SemanticSearchService;
import sk.iway.iwcm.rag.service.MarkdownSourceService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies authentication and request throttling on the public Markdown search endpoint. */
class MarkdownSearchRestControllerTest extends BaseWebjetTest {

    private final SemanticSearchService semanticSearch = mock(SemanticSearchService.class);
    private final EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
    private final RagService ragService = mock(RagService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MarkdownSearchRestController(
        new MarkdownSearchService(new MarkdownSourceService(), semanticSearch, repository, ragService))).build();
    private final String originalFolders = Constants.getString("ragMarkdownFolders");
    private final boolean originalRequireLogin = Constants.getBoolean("ragMarkdownSearchRequireLogin");
    private final String originalDisabledIps = Constants.getString("spamProtectionDisabledIPs");
    private final Map<String, Integer> originalSpamLimits = Map.of(
        "spamProtectionTimeout-search", Constants.getInt("spamProtectionTimeout-search"),
        "spamProtectionHourlyLimit-search", Constants.getInt("spamProtectionHourlyLimit-search"),
        "spamProtectionIgnoreFirstRequests-search", Constants.getInt("spamProtectionIgnoreFirstRequests-search", -1)
    );

    @BeforeEach
    void configureSearchLimits() {
        SpamProtection.clearAll();
        Constants.setInt("spamProtectionTimeout-search", 10);
        Constants.setInt("spamProtectionHourlyLimit-search", 200);
        Constants.setInt("spamProtectionIgnoreFirstRequests-search", 0);
        Constants.setString("spamProtectionDisabledIPs", "");
    }

    @AfterEach
    void restoreConfiguration() {
        SpamProtection.clearAll();
        Constants.setString("ragMarkdownFolders", originalFolders);
        Constants.setBoolean("ragMarkdownSearchRequireLogin", originalRequireLogin);
        Constants.setString("spamProtectionDisabledIPs", originalDisabledIps);
        originalSpamLimits.forEach(Constants::setInt);
    }

    /** Allows ordinary users and optionally anonymous visitors to search all configured root types. */
    @ParameterizedTest
    @CsvSource({"true,false,401", "true,true,200", "false,false,200", "false,true,200"})
    void publicEndpointAppliesLoginSetting(boolean requireLogin, boolean loggedIn, int expectedStatus) throws Exception {
        Constants.setBoolean("ragMarkdownSearchRequireLogin", requireLogin);
        Constants.setString("ragMarkdownFolders", "/docs/public,/admin/docs/webjetcms,file:/docs");
        Map<String, Object> filters = Map.of("sourceRoots", List.of(
            "/docs/public/sk/admin", "/admin/docs/webjetcms/sk/admin", "file:/docs/sk/admin"));
        MockHttpServletRequestBuilder request = get("/rest/rag/markdown/search")
            .param("query", "users").param("language", "sk").param("directory", "/sk/admin/");
        if (loggedIn) {
            Identity user = new Identity();
            assertFalse(user.isAdmin());
            request.sessionAttr(Constants.USER_KEY, user);
        }
        try (var domains = mockStatic(CloudToolsForCore.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(7);
            var response = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andExpect(header().string("Cache-Control", "no-store"));
            if (expectedStatus == 200) {
                response.andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"results\":[]")));
                verify(semanticSearch).searchChunks(eq("users"), eq(7), eq("sk"), eq(10),
                    eq(RagEntityType.MARKDOWN), eq(filters), any());
            } else {
                verifyNoInteractions(semanticSearch);
            }
            verifyNoInteractions(repository, ragService);
        }
    }

    /** Rejects excess searches before AI calls for all users, without charging invalid or unauthenticated requests. */
    @ParameterizedTest
    @CsvSource({"false,60,200", "true,60,200", "false,-2,1", "true,-2,1"})
    void throttlesPaidCalls(boolean loggedIn, int timeout, int hourlyLimit) throws Exception {
        Constants.setBoolean("ragMarkdownSearchRequireLogin", false);
        Constants.setString("ragMarkdownFolders", "/docs/public");
        Constants.setInt("spamProtectionTimeout-search", timeout);
        Constants.setInt("spamProtectionHourlyLimit-search", hourlyLimit);
        var request = get("/rest/rag/markdown/search").param("query", "users").param("language", "sk");

        try (var domains = mockStatic(CloudToolsForCore.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(7);
            mvc.perform(get("/rest/rag/markdown/search").param("query", "users")
                .param("language", "sk").param("directory", "/en/admin/"))
                .andExpect(status().isBadRequest());
            Constants.setBoolean("ragMarkdownSearchRequireLogin", true);
            mvc.perform(request).andExpect(status().isUnauthorized());
            verifyNoInteractions(semanticSearch, repository, ragService);

            Constants.setBoolean("ragMarkdownSearchRequireLogin", loggedIn);
            if (loggedIn) request.sessionAttr(Constants.USER_KEY, new Identity());
            mvc.perform(request).andExpect(status().isOk());
            verify(semanticSearch).searchChunks(eq("users"), eq(7), eq("sk"), eq(10),
                eq(RagEntityType.MARKDOWN), eq(Map.of("sourceRoot", "/docs/public")), any());
            clearInvocations(semanticSearch, repository, ragService);

            mvc.perform(request)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Cache-Control", "no-store"));

            verifyNoInteractions(semanticSearch, repository, ragService);
        }
    }

}
