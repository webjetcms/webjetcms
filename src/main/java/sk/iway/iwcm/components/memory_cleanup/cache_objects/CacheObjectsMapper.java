package sk.iway.iwcm.components.memory_cleanup.cache_objects;

import org.springframework.stereotype.Component;
import sk.iway.iwcm.CacheBean;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.tags.support.ResponseUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class CacheObjectsMapper {

    List<CacheDto> beanListToDtoList(List<CacheBean> cacheBeans) {
        long counter = 0;
        List<CacheDto> cacheDtoList = new ArrayList<>();

        for (CacheBean bean : cacheBeans) {
            long size = bean.getName().length();
            CacheDto cacheDto = new CacheDto();

            cacheDto.setId(++counter);
            cacheDto.setName(bean.getName());
            cacheDto.setExpirationDate(new Date(bean.getExpiryTime()));

            try {
                cacheDto.setToStringObjectValue(bean.getObject().getClass().toString() + "\n" + ResponseUtils.filter(bean.getObject().toString()));

                if (bean.getObject() != null) {
                    if (bean.getObject() instanceof String) {
                        size = ((String) bean.getObject()).length() + 80;
                    } else if (bean.getObject() instanceof DocDetails) {
                        size = ((DocDetails) bean.getObject()).getData().length() + ((DocDetails) bean.getObject()).getTitle().length() + 80;
                        size = size * 2;
                    } else if (bean.getObject() instanceof BrowserDetector) {
                        size = ((BrowserDetector) bean.getObject()).getUserAgentString().length() + 80;
                    }
                }
            } catch (Exception ex) {
                size += 1000;
            }

            cacheDto.setSize(size);
            cacheDtoList.add(cacheDto);
        }

        return cacheDtoList;
    }
}
