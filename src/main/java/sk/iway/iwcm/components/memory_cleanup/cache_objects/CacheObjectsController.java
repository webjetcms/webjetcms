package sk.iway.iwcm.components.memory_cleanup.cache_objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.utils.Pair;

import java.util.HashMap;
import java.util.Map;

@RestController
@Datatable
@RequestMapping(path = "/admin/rest/settings/cache-objects")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_data_deleting')")
public class CacheObjectsController extends DatatableRestControllerV2<CacheDto, Long> {

    private final CacheObjectsService cacheObjectsService;

    @Autowired
    public CacheObjectsController(CacheObjectsService cacheObjectsService) {
        super(null);
        this.cacheObjectsService = cacheObjectsService;
    }

    @Override
    public Page<CacheDto> getAllItems(Pageable pageable) {
        return cacheObjectsService.getAllCacheBeans(getRequest(), pageable);
    }

    @Override
    public CacheDto insertItem(CacheDto entity) {
        throwError("Vytvarat cache objekt nie je povolene!");
        return null;
    }

    @Override
    public CacheDto editItem(CacheDto entity, long id) {
        throwError("datatables.error.recordIsNotEditable");
        return null;
    }

    @Override
    public boolean deleteItem(CacheDto entity, long id) {
        return cacheObjectsService.deleteCacheBean(entity);
    }

    @Override
    public Page<CacheDto> searchItem(Map<String, String> params, Pageable pageable, CacheDto search) {
        String sortKey = null;
        String sortValue = null;
        Map<String, String> searchMap = new HashMap<>();

        for (Map.Entry<String, String> paramsEntry : params.entrySet()) {
            if (cacheObjectsService.checkSearchParam(paramsEntry.getKey())) {
                String key = getCleanKey(paramsEntry.getKey());
                String value = getCleanValue(paramsEntry.getValue());
                if ("expirationDate".equals(key)) {
                    value = paramsEntry.getValue();
                }
                searchMap.put(key, value);
            } else if (cacheObjectsService.checkSortParam(paramsEntry.getKey())) {
                String[] sortParams = paramsEntry.getValue().split(",");
                sortKey = sortParams[0];
                sortValue = sortParams[1];
            }
        }

        Pair<String, String> sortPair = new Pair<>(sortKey, sortValue);
        return cacheObjectsService.getFilteredCacheDtos(searchMap, sortPair, pageable);
    }

    @Override
    public boolean processAction(CacheDto entity, String action) {
        if ("deleteAllCacheObjects".equals(action)) {
            return cacheObjectsService.deleteAllCacheBeans();
        } else if ("deletePictureCache".equals(action)) {
            return cacheObjectsService.deletePictureCache();
        }
        return false;
    }
}
