package sk.iway.iwcm.components.memory_cleanup.cache_objects;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Component
public class CacheDtoComparator {

    private Map<String, Comparator<CacheDto>> sortingOptions = new HashMap<>();

    public CacheDtoComparator() {
        //Comparator<Date> nullSafeDateComparator = Comparator.nullsLast(Date::compareTo);
        this.sortingOptions.put("id", Comparator.comparing(CacheDto::getId));
        this.sortingOptions.put("name", Comparator.comparing(CacheDto::getName));
        this.sortingOptions.put("expirationDate", Comparator.comparing(CacheDto::getExpirationDate));
        this.sortingOptions.put("size", Comparator.comparing(CacheDto::getSize));
    }

    Comparator<CacheDto> getSortingComparator(String key) {
        return sortingOptions.get(key);
    }

}
