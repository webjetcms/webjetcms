package sk.iway.iwcm.components.tabs_settings.rest;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsEntity;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsRepository;

/**
 * Caches tab overrides by entity class and domain for one hour.
 */
@Service
public class TabsSettingsService {

    private static final String SETTINGS_CACHE_PREFIX = "TabSettings_";
    private static final int SETTINGS_CACHE_SECONDS = 3600;

    private final TabsSettingsRepository repository;

    public TabsSettingsService(TabsSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Loads tab settings, including empty results, from the cache or repository.
     *
     * @param className fully qualified entity class name
     * @param domainId domain owning the settings
     * @return settings cached for one hour
     */
    public TabsSettingsEntity[] getTabsSettings(String className, int domainId) {
        Cache cache = Cache.getInstance();
        String cacheKey = getCacheKey(className, domainId);
        TabsSettingsEntity[] settings = cache.getObject(cacheKey, TabsSettingsEntity[].class);
        if (settings == null) {
            settings = repository.findAllByClassNameAndDomainId(className, domainId).toArray(TabsSettingsEntity[]::new);
            cache.setObjectSeconds(cacheKey, settings, SETTINGS_CACHE_SECONDS, false);
        }
        return settings;
    }

    /**
     * Builds a fixed-length cache key scoped by entity class and domain.
     *
     * @param className fully qualified entity class name
     * @param domainId domain owning the settings
     * @return the settings prefix followed by the SHA-256 hash of the class/domain pair
     */
    public String getCacheKey(String className, int domainId) {
        return SETTINGS_CACHE_PREFIX + DigestUtils.sha256Hex((className + "\n" + domainId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Removes settings locally and requests invalidation on other cluster nodes.
     *
     * @param cacheKey key returned by {@link #getCacheKey(String, int)}
     */
    public void clearCache(String cacheKey) {
        Cache.getInstance().removeObject(cacheKey, true);
    }
}
