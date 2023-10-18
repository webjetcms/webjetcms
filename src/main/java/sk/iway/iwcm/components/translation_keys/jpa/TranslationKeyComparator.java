package sk.iway.iwcm.components.translation_keys.jpa;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * TranslationKeyComparator
 * <p>
 * Metoda sluzi na vratenie pozadovaneho comparatora na zaklade vlozeneho stringu.
 */
@Component
public class TranslationKeyComparator {

    private Map<String, Comparator<TranslationKeyEntity>> sortingOptions = new HashMap<>();

    public TranslationKeyComparator() {
        Comparator<Date> nullSafeDateComparator = Comparator.nullsFirst(Date::compareTo);
        this.sortingOptions.put("id", Comparator.comparing(TranslationKeyEntity::getId));
        this.sortingOptions.put("key", Comparator.comparing(TranslationKeyEntity::getKey));
        this.sortingOptions.put("value", Comparator.comparing(TranslationKeyEntity::getValue));
        this.sortingOptions.put("updateDate", Comparator.comparing(TranslationKeyEntity::getUpdateDate, nullSafeDateComparator));

        this.sortingOptions.put("fieldA", Comparator.comparing(TranslationKeyEntity::getFieldA));
        this.sortingOptions.put("fieldB", Comparator.comparing(TranslationKeyEntity::getFieldB));
        this.sortingOptions.put("fieldC", Comparator.comparing(TranslationKeyEntity::getFieldC));
        this.sortingOptions.put("fieldD", Comparator.comparing(TranslationKeyEntity::getFieldD));
        this.sortingOptions.put("fieldE", Comparator.comparing(TranslationKeyEntity::getFieldE));
        this.sortingOptions.put("fieldF", Comparator.comparing(TranslationKeyEntity::getFieldF));
        this.sortingOptions.put("fieldG", Comparator.comparing(TranslationKeyEntity::getFieldG));
        this.sortingOptions.put("fieldH", Comparator.comparing(TranslationKeyEntity::getFieldH));
        this.sortingOptions.put("fieldI", Comparator.comparing(TranslationKeyEntity::getFieldI));
        this.sortingOptions.put("fieldJ", Comparator.comparing(TranslationKeyEntity::getFieldJ));

        this.sortingOptions.put("originalValueA", Comparator.comparing(TranslationKeyEntity::getOriginalValueA));
        this.sortingOptions.put("originalValueB", Comparator.comparing(TranslationKeyEntity::getOriginalValueB));
        this.sortingOptions.put("originalValueC", Comparator.comparing(TranslationKeyEntity::getOriginalValueC));
        this.sortingOptions.put("originalValueD", Comparator.comparing(TranslationKeyEntity::getOriginalValueD));
        this.sortingOptions.put("originalValueE", Comparator.comparing(TranslationKeyEntity::getOriginalValueE));
        this.sortingOptions.put("originalValueF", Comparator.comparing(TranslationKeyEntity::getOriginalValueF));
        this.sortingOptions.put("originalValueG", Comparator.comparing(TranslationKeyEntity::getOriginalValueG));
        this.sortingOptions.put("originalValueH", Comparator.comparing(TranslationKeyEntity::getOriginalValueH));
        this.sortingOptions.put("originalValueI", Comparator.comparing(TranslationKeyEntity::getOriginalValueI));
        this.sortingOptions.put("originalValueJ", Comparator.comparing(TranslationKeyEntity::getOriginalValueJ));

    }

    public Comparator<TranslationKeyEntity> getSortingComparator(String key) {
        return sortingOptions.get(key);
    }

}
