package sk.iway.iwcm.components.memory_cleanup.persistent_cache_objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.system.cache.PersistentCacheBean;
import sk.iway.iwcm.system.cache.PersistentCacheDB;

import java.util.Optional;

@Service
public class PersistentCacheService {

    private final PersistentCacheRepository persistentCacheRepository;

    @Autowired
    public PersistentCacheService(PersistentCacheRepository persistentCacheRepository) {
        this.persistentCacheRepository = persistentCacheRepository;
    }

    Page<PersistentCacheBean> getAllPersistentBeans(Pageable pageable) {
        return persistentCacheRepository.findAll(pageable);
    }

    boolean refreshPersistentCaches(PersistentCacheBean persistentCacheBeanTest) {
        PersistentCacheDB persistentCacheDB = new PersistentCacheDB();
        Optional<PersistentCacheBean> optionalPersistentCacheBeanTest = persistentCacheRepository.findById(persistentCacheBeanTest.getId());

        if (optionalPersistentCacheBeanTest.isPresent()) {
            persistentCacheDB.updateData(optionalPersistentCacheBeanTest.get());
            Cache cache = Cache.getInstance();
            cache.removeObject("PersistentCache." + optionalPersistentCacheBeanTest.get().getDataValue());
        }

        return true;
    }
}
