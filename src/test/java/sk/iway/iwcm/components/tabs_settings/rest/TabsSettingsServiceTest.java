package sk.iway.iwcm.components.tabs_settings.rest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsEntity;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsRepository;

/** Tests domain-scoped tab settings caching and invalidation. */
class TabsSettingsServiceTest {

    private static final String CLASS_NAME = "example.Article";
    private final TabsSettingsRepository repository = mock(TabsSettingsRepository.class);
    private final TabsSettingsService service = new TabsSettingsService(repository);

    /** Settings, including empty results, are cached and reused without another repository lookup. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cachesSettings(boolean empty) {
        Cache cache = mock(Cache.class);
        String key = service.getCacheKey(CLASS_NAME, 9);
        List<TabsSettingsEntity> settings = empty ? List.of() : List.of(new TabsSettingsEntity());
        when(repository.findAllByClassNameAndDomainId(CLASS_NAME, 9)).thenReturn(settings);

        try (MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            caches.when(Cache::getInstance).thenReturn(cache);

            TabsSettingsEntity[] result = service.getTabsSettings(CLASS_NAME, 9);

            assertArrayEquals(settings.toArray(TabsSettingsEntity[]::new), result);
            verify(cache).setObjectSeconds(key, result, 3600, false);
            when(cache.getObject(key, TabsSettingsEntity[].class)).thenReturn(result);
            assertSame(result, service.getTabsSettings(CLASS_NAME, 9));
            verify(repository).findAllByClassNameAndDomainId(CLASS_NAME, 9);
        }
    }

    /** Cache keys isolate classes and domains, and invalidation also refreshes cluster nodes. */
    @Test
    void scopesAndClearsCache() {
        Cache cache = mock(Cache.class);
        String key = service.getCacheKey(CLASS_NAME, 9);
        assertEquals(key, service.getCacheKey(CLASS_NAME, 9));
        assertNotEquals(key, service.getCacheKey(CLASS_NAME, 17));
        assertNotEquals(key, service.getCacheKey("example.Category", 9));

        try (MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            caches.when(Cache::getInstance).thenReturn(cache);
            service.clearCache(key);
            verify(cache).removeObject(key, true);
        }
    }
}
