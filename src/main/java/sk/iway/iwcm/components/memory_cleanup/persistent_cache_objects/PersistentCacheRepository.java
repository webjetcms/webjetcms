package sk.iway.iwcm.components.memory_cleanup.persistent_cache_objects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import sk.iway.iwcm.system.cache.PersistentCacheBean;

@Repository
public interface PersistentCacheRepository extends JpaRepository<PersistentCacheBean, Long>, JpaSpecificationExecutor<PersistentCacheBean> {
}
