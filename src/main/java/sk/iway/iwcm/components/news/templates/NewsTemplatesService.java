package sk.iway.iwcm.components.news.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesRepository;

@Service
public class NewsTemplatesService {

    private NewsTemplatesRepository newsTemplatesRepository;
    private static final String CACHE_KEY = "newsTemplatesService.templates";

    @Autowired
    public NewsTemplatesService(NewsTemplatesRepository newsTemplatesRepository) {
        this.newsTemplatesRepository = newsTemplatesRepository;
    }

    /**
     * Fetches the template by name from the cache or database.
     * If the template is not found in the cache, it will be fetched from the database and cached.
     * @param name
     * @return
     */
    public static NewsTemplatesEntity getTemplateByName(String name) {
        //old format from properties use news.template. prefix
        if (name != null && name.startsWith("news.template.")) name = name.substring("news.template.".length());

        NewsTemplatesEntity entity = getCacheList().get(getCacheKey(name));
        if (entity != null) {
            return entity;
        }

        NewsTemplatesService service = Tools.getSpringBean("newsTemplatesService", NewsTemplatesService.class);
        if (service == null) {
            Logger.error(NewsTemplatesService.class, "NewsTemplatesService bean not found");
            return null;
        }
        return service.getTemplateByNameFromDB(name);
    }

    /**
     * Fetches all templates from the database for current domain.
     * @return
     */
    public static List<NewsTemplatesEntity> getTemplates() {
        NewsTemplatesService service = Tools.getSpringBean("newsTemplatesService", NewsTemplatesService.class);
        if (service == null) {
            Logger.error(NewsTemplatesService.class, "NewsTemplatesService bean not found");
            return List.of();
        }
        return service.newsTemplatesRepository.findAllByDomainId(CloudToolsForCore.getDomainId());
    }

    /**
     * Fetches the template by name from the database and caches it.
     * @param name the name of the template
     * @return the NewsTemplatesEntity if found, null otherwise
     */
    private NewsTemplatesEntity getTemplateByNameFromDB(String name) {
        NewsTemplatesEntity entity = newsTemplatesRepository.findFirstByNameAndDomainId(name, CloudToolsForCore.getDomainId()).orElse(null);
        if (entity != null) {
            getCacheList().put(getCacheKey(name), entity);
        }
        return entity;
    }

    private static String getCacheKey(String name) {
        return CloudToolsForCore.getDomainId() + "." + name;
    }

    /**
     * Returns a map of cached templates. Templates are cached by their name and domainId.
     * @return
     */
    private static Map<String, NewsTemplatesEntity> getCacheList() {
        Cache cache = Cache.getInstance();
        @SuppressWarnings("unchecked")
        Map<String, NewsTemplatesEntity> list = cache.getObject(CACHE_KEY, Map.class);
        if (list == null) {
            list = new HashMap<>();
            cache.setObjectSeconds(CACHE_KEY, list, 10*60);
        }
        return list;
    }

    /**
     * Clears the cache for news templates.
     */
    public static void clearCache() {
        Cache cache = Cache.getInstance();
        cache.removeObject(CACHE_KEY);
    }

}
