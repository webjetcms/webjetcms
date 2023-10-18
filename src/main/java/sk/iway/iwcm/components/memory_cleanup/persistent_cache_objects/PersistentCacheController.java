package sk.iway.iwcm.components.memory_cleanup.persistent_cache_objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.system.cache.PersistentCacheBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping(path = "/admin/rest/settings/persistent-cache-objects")
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_data_deleting')")
public class PersistentCacheController extends DatatableRestControllerV2<PersistentCacheBean, Long> {

    private final PersistentCacheService persistentCacheService;

    @Autowired
    public PersistentCacheController(PersistentCacheService persistentCacheService, PersistentCacheRepository persistentCacheRepository) {
        super(persistentCacheRepository);
        this.persistentCacheService = persistentCacheService;
    }

    @Override
    public Page<PersistentCacheBean> getAllItems(Pageable pageable) {
        return persistentCacheService.getAllPersistentBeans(pageable);
    }

    @Override
    public boolean processAction(PersistentCacheBean entity, String action) {
        if ("refresh".equals(action)) {
            return persistentCacheService.refreshPersistentCaches(entity);
        }
        return false;
    }
}
