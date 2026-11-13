package sk.iway.iwcm.system.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies translation failure handling with mocked HTTP responses and no real DeepL credentials. */
class DeepLFailureTest extends BaseWebjetTest {
    private String originalUrl;
    private String originalKey;
    private final Map<String, String> translations = new HashMap<>();
    private final Executor executor = mock(Executor.class);
    private final Response response = mock(Response.class);
    private MockedStatic<Cache> cacheAccess;
    private MockedStatic<Executor> http;
    private DeepL engine;

    @BeforeEach
    void setUp() throws Exception {
        originalUrl = Constants.getString("deepl_api_url");
        originalKey = Constants.getString("deepl_auth_key");
        Constants.setString("deepl_api_url", "https://deepl.invalid/v2/translate");
        Constants.setString("deepl_auth_key", "fixture-key");
        Cache cache = mock(Cache.class);
        when(cache.getObject("DeepL.translations")).thenReturn(translations);
        when(executor.execute(any(Request.class))).thenReturn(response);
        cacheAccess = mockStatic(Cache.class);
        cacheAccess.when(Cache::getInstance).thenReturn(cache);
        http = mockStatic(Executor.class);
        http.when(() -> Executor.newInstance(any(CloseableHttpClient.class))).thenReturn(executor);
        engine = spy(new DeepL());
        doNothing().when(engine).auditBilledCharacters(org.mockito.ArgumentMatchers.anyLong());
    }

    @AfterEach
    void tearDown() {
        http.close();
        cacheAccess.close();
        Constants.setString("deepl_api_url", originalUrl);
        Constants.setString("deepl_auth_key", originalKey);
    }

    /** Legacy callers receive the exact original input when DeepL rejects a translation. */
    @ParameterizedTest
    @ValueSource(ints = { 456, 500 })
    void preservesOriginalTextOnFailure(int status) throws Exception {
        when(response.returnContent()).thenThrow(new HttpResponseException(status, "Fixture failure"));
        String text = "Akciové ponuky v&nbsp;e-shope";
        assertEquals(text, engine.translate(text, "sk", "en"));
        assertThrows(TranslationException.class, () -> engine.translateOrThrow(text, "sk", "en"));
        assertEquals(Map.of(), translations);
    }

    /** Identical API input must share a cache entry even when its source used an HTML space entity. */
    @Test
    void sharesCacheForEquivalentInputs() throws Exception {
        Content content = mock(Content.class);
        when(response.returnContent()).thenReturn(content);
        when(content.asString(StandardCharsets.UTF_8)).thenReturn("{\"translations\":[{\"text\":\"Special offers\"}]}");
        assertEquals("Special offers", engine.translateOrThrow("Akciové ponuky v&nbsp;e-shope", "sk", "en"));
        assertEquals("Special offers", engine.translateOrThrow("Akciové ponuky v e-shope", "sk", "en"));
        verify(executor, times(1)).execute(any(Request.class));
        assertEquals(1, translations.size());
    }

    /** Invalid or empty responses are failures and must never become cached translations. */
    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"translations\":[]}", "{\"translations\":[{\"text\":\"\"}]}" })
    void rejectsInvalidResponse(String json) throws Exception {
        Content content = mock(Content.class);
        when(response.returnContent()).thenReturn(content);
        when(content.asString(StandardCharsets.UTF_8)).thenReturn(json);
        assertThrows(TranslationException.class, () -> engine.translateOrThrow("Ponuky", "sk", "en"));
        assertEquals(Map.of(), translations);
    }

    /** Rate limiting advances the retry counter and terminates without sleeping in the test. */
    @Test
    void limitsRateLimitRetries() throws Exception {
        when(response.returnContent()).thenThrow(new HttpResponseException(429, "Fixture rate limit"));
        doReturn(true, true, true, true, false).when(engine).applyDelay(anyInt());
        assertThrows(TranslationException.class, () -> engine.translateOrThrow("Ponuky", "sk", "en"));
        ArgumentCaptor<Integer> attempts = ArgumentCaptor.forClass(Integer.class);
        verify(engine, times(5)).applyDelay(attempts.capture());
        assertEquals(List.of(0, 1, 2, 3, 4), attempts.getAllValues());
        verify(executor, times(5)).execute(any(Request.class));
    }
}
