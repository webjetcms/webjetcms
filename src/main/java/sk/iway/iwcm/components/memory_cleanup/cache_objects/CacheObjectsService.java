package sk.iway.iwcm.components.memory_cleanup.cache_objects;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.tags.CombineTag;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.utils.Pair;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;

@Service
public class CacheObjectsService {

    private final CacheObjectsMapper memoryCleanupMapper;
    private final CacheDtoComparator cacheDtoComparator;

    @Autowired
    public CacheObjectsService(CacheObjectsMapper memoryCleanupMapper, CacheDtoComparator cacheDtoComparator) {
        this.memoryCleanupMapper = memoryCleanupMapper;
        this.cacheDtoComparator = cacheDtoComparator;
    }

    Page<CacheDto> getAllCacheBeans(HttpServletRequest request, Pageable pageable) {
        Cache cache = Cache.getInstance();
        Enumeration<CacheBean> cacheBeans = cache.getAllElements();
        List<CacheBean> listOfCacheBeans = Collections.list(cacheBeans);
        List<CacheDto> cacheDtoList = memoryCleanupMapper.beanListToDtoList(listOfCacheBeans);

        String sort = request.getParameter("sort");
        String[] sortArray = sort.split(",");
        Pair<String, String> sortPair = new Pair<>(sortArray[0], sortArray[1]);

        sortCacheDtos(sortPair, cacheDtoList);

        PagedListHolder<CacheDto> holder = new PagedListHolder<>(cacheDtoList);
        holder.setPage(pageable.getPageNumber());
        holder.setPageSize(pageable.getPageSize());

        return new PageImpl<>(holder.getPageList(), pageable, cacheDtoList.size());
    }

    boolean deleteCacheBean(CacheDto cacheBean) {
        Cache cache = Cache.getInstance();
        cache.removeObject(cacheBean.getName());
        ClusterDB.addRefresh(DB.prepareString("sk.iway.iwcm.Cache-" + cacheBean.getName(), 250));
        Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Deleting cache, key= " + cacheBean.getName(), -1, -1);
        return true;
    }

    boolean deleteAllCacheBeans() {
        Cache cache = Cache.getInstance();
        try {
            cache.clearAll();
            //zmaz aj doc db a podobne
            DocDB.getInstance(true);
            GroupsDB.getInstance(true);
            TemplatesDB.getInstance(true);
            UserGroupsDB.getInstance(true);
            //prenes zmenu na cely cluster
            ClusterDB.addRefresh("sk.iway.iwcm.Cache-delAll");

            //zmen version tag
            long now = Tools.getNow();
            ClusterDB.addRefresh("sk.iway.iwcm.tags.CombineTag-"+now);

            DB.resetHtmlAllowedFields();
            CombineTag.setVersion(now);

            Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Deleting cache ALL", -1, -1);
        } catch (Exception e) {
            Logger.error(CacheObjectsService.class, "Error deleting all cache objects. Error message: " + e.getMessage());
            return false;
        }

        return true;
    }

    boolean deletePictureCache() {
        String dirStr = sk.iway.iwcm.Tools.getRealPath(Constants.getString("thumbServletCacheDir"));
        IwcmFile dir = new IwcmFile(dirStr);
        try {
            deletePictureCacheCycleClass(dir);
        } catch (Exception e) {
            Logger.error(CacheObjectsService.class, "Error deleting picture cache. Error message: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void deletePictureCacheCycleClass(IwcmFile iwcmFile) {
        for (IwcmFile file : iwcmFile.listFiles()) {
            if (file.isDirectory()) {
                deletePictureCacheCycleClass(file);
            } else {
                file.delete();
            }
        }
    }

    Page<CacheDto> getFilteredCacheDtos(Map<String, String> searchMap, Pair<String, String> sortPair,
                                        Pageable pageable) {
        Cache cache = Cache.getInstance();
        Enumeration<CacheBean> cacheBeans = cache.getAllElements();
        List<CacheBean> listOfCacheBeans = Collections.list(cacheBeans);
        List<CacheDto> cacheDtoList = memoryCleanupMapper.beanListToDtoList(listOfCacheBeans);
        List<CacheDto> filteredCacheDtos = new ArrayList<>();

        for (CacheDto entity : cacheDtoList) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
            boolean present = false;
            for (Map.Entry<String, String> searchParameter : searchMap.entrySet()) {
                present = false;

                if (searchParameter.getKey().equals("expirationDate")) {
                    Pair<Timestamp, Timestamp> datePair = getCleanExpirationDateValue(searchParameter.getValue());

                    if (null == datePair.first) {
                        if (null != entity.getExpirationDate() && entity.getExpirationDate().before(datePair.second)) {
                            present = true;
                        }
                    } else if (null == datePair.second) {
                        if (null != entity.getExpirationDate() && entity.getExpirationDate().after(datePair.first)) {
                            present = true;
                        }
                    } else {
                        if (null != entity.getExpirationDate() && entity.getExpirationDate().after(datePair.first) && entity.getExpirationDate().before(datePair.second)) {
                            present = true;
                        }
                    }
                } else {
                    String entityValue = (String) beanWrapper.getPropertyValue(searchParameter.getKey());

                    if (null == entityValue) break;

                    if (entityValue.toLowerCase().contains(searchParameter.getValue().toLowerCase())) present = true;

                    if (!present) break;
                }
            }
            if (present) filteredCacheDtos.add(entity);
        }

        sortCacheDtos(sortPair, filteredCacheDtos);

        PagedListHolder<CacheDto> holder = new PagedListHolder<>(filteredCacheDtos);
        holder.setPage(pageable.getPageNumber());
        holder.setPageSize(pageable.getPageSize());

        return new PageImpl<>(holder.getPageList(), pageable, filteredCacheDtos.size());
    }

    private Pair<Timestamp, Timestamp> getCleanExpirationDateValue(String updateDate) {
        //String filteredDate = updateDate.replace("daterange:", "");
        //String[] stringDateArray = new String[2];
        Timestamp from = null;
        Timestamp to = null;

        String[] values = Tools.getTokens(updateDate.substring(updateDate.indexOf(":") + 1), "-");
        if (values.length == 2) {
            from = new Timestamp(Tools.getLongValue(values[0], 0));
            to = new Timestamp(Tools.getLongValue(values[1], 0));
        } else if (values.length == 1) {
            if (updateDate.contains("range:-")) to = new Timestamp(Tools.getLongValue(values[0], 0));
            else from = new Timestamp(Tools.getLongValue(values[0], 0));
        }

        if (to != null) {
            //to nam pride vo formate 2.6.2020 ale mysli sa do konca dna, je potrebne pridat 24 hodin
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(to.getTime());
            cal.add(Calendar.DATE, 1);
            to = new Timestamp(cal.getTimeInMillis());
        }

//        if (filteredDate.contains("-")) {
//            if(filteredDate.startsWith("-")){
//                stringDateArray[0] = "";
//                stringDateArray[1] = filteredDate.replace("-", "");
//            } else {
//                stringDateArray = filteredDate.split("-");
//            }
//        } else {
//            stringDateArray[0] = filteredDate.replace("-", "");
//            stringDateArray[1] = "";
//        }
//
//        long[] longDateArray = new long[]{Tools.getLongValue(stringDateArray[0], 0), Tools.getLongValue(stringDateArray[1], new Date().getTime())};
//
//        Date dateFrom = new Date(longDateArray[0]);
//        Date dateTo = new Date(longDateArray[1]);
        return new Pair<>(from, to);
    }

    private void sortCacheDtos(Pair<String, String> sortPair, List<CacheDto> cacheDtoList) {
        if (sortPair.second.equals("asc")) {
            cacheDtoList.sort(cacheDtoComparator.getSortingComparator(sortPair.first));
        } else if (sortPair.second.equals("desc")) {
            cacheDtoList.sort(cacheDtoComparator.getSortingComparator(sortPair.first).reversed());
        }
    }

    boolean checkSearchParam(String param) {
        return param.startsWith("search");
    }

    boolean checkSortParam(String param) {
        return param.equals("sort");
    }
}
