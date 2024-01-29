package sk.iway.iwcm.components.translation_keys.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyComparator;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyRepository;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.i18n.PropDB;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;

/**
 * TranslationKeyService
 * <p>
 * Trieda sluzi na CRUD operacie pre prekladove kluce. Pracuje s dvoma zdrojmi, properties subor a tabulka
 * webjet_properties. DefaultKeys su nemenne prekladove kluce, ktore sluzia iba na READ funkcionalitu.
 * ChangedKeys su prekladove kluce pre ktore funguju CREATE, READ, UPDATE a aj DELETE funkcionality.
 */

@Service
public class TranslationKeyService {

    private TranslationKeyRepository translationKeyRepository;
    private TranslationKeyComparator translationKeyComparator;

    //Represent maximum of support language translations (10 languages)
    private static final char LAST_ALPHABET = 'J';

    @Autowired
    TranslationKeyService(TranslationKeyRepository translationKeyRepository, TranslationKeyComparator translationKeyComparator) {
        //Logger.debug(TranslationKeyService.class, "TranslationKeyService.constructor v2, translationKeyRepository="+translationKeyRepository+" translationKeyComparator="+translationKeyComparator);
        this.translationKeyRepository = translationKeyRepository;
        this.translationKeyComparator = translationKeyComparator;
    }

    /**
     * Returns map of language-field combination based on languages conf. property. Eg.:
     * sk->A (as fieldA)
     * en->B (as fieldB)
     * @return
     */
    public Map<String, String> getLanguageFieldCombination() {
        Map<String, String> languageFieldCombination = new LinkedHashMap<>();
        //Set combination of  "language shortcut" and "field alphabet"
        String[] lngArr = Constants.getArray("languages");
        for(int i = 0; i < lngArr.length; i++) {
            char fieldAlphabet = (char)(('A') + i);

            //Support only certain number of languages
            if(fieldAlphabet > LAST_ALPHABET) break;

            languageFieldCombination.put(lngArr[i], fieldAlphabet + "");
        }

        return languageFieldCombination;
    }

    /**
     * Prepare and return List od TranslationKeyEntities.
     * Every entity contain all value-language combinations for specific translation key.
     * This value in various languages are set in custom-fields. Every entity allso have set editorFields, that set up custom-fields
     * type, name, label etc.
     *
     * @return lis of entities ready to by used as page data for Datable TranslationKeys
     */
    private List<TranslationKeyEntity> getAllData(Identity user) {
        //List<TranslationKeyEntity> defaultLngKeys = getDefaultLngKeys(request);
        Map<String, String> languageFieldCombination = getLanguageFieldCombination();
        Map<String, TranslationKeyEntity> allDataMap = new HashMap<>();

        //odfiltruj zoznam podla prav
        boolean enableAllKeys = false;
		String propertiesEnabledKeys = Constants.getStringExecuteMacro("propertiesEnabledKeys");
        if (Tools.isEmpty(propertiesEnabledKeys) || user.isEnabledItem("prop.show_all_texts")) enableAllKeys = true;
        String[] enabledKeys = Tools.getTokens(propertiesEnabledKeys, ",");

        //prejdi vsetky jazyky a napln mapu
        for (Map.Entry<String, String> lngField : languageFieldCombination.entrySet()) {
            String lng = lngField.getKey();
            String fieldAlphabet = lngField.getValue();

            //ziskaj prekladove texty pre jazyk
            //Logger.debug(getClass(), "getRes, lng="+lng);
            IwayProperties prop = Prop.getInstance(lng).getProperties();
            IwayProperties defaultKeys = Prop.getDefaulProperties(lng, lng, Constants.getServletContext());

            for (Map.Entry<String, String> propEntry : prop.entrySet()) {
                String key = propEntry.getKey();

                String text = propEntry.getValue();
                String originalText = defaultKeys.get(key);

                TranslationKeyEntity entity = allDataMap.get(key);
                if (entity == null) {
                    //prava staci kontrolovat len pri pridani, inak ak je uz pridana musi byt spravne pravo
                    if (enableAllKeys==false && PropDB.isKeyVisibleToUser(user, enabledKeys, key)==false) continue;

                    entity = new TranslationKeyEntity();
                    entity.setKey(key);

                    Date lastUpdate = getLastUpdateDate(key);
                    if (lastUpdate != null) entity.setUpdateDate(lastUpdate);

                    allDataMap.put(key, entity);
                }
                BeanWrapperImpl bw = new BeanWrapperImpl(entity);
                bw.setPropertyValue("field" + fieldAlphabet, text);
                bw.setPropertyValue("originalValue"+fieldAlphabet, originalText);
            }

        }

        //sortni to podla abecedy a nastav IDecka
        List<TranslationKeyEntity> allData = allDataMap.values().stream().sorted(Comparator.comparing(TranslationKeyEntity::getKey)).collect(Collectors.toList());
        for (int i=0; i<allData.size(); i++) {
            TranslationKeyEntity entity = allData.get(i);
            entity.setId(Long.valueOf((i+1)));
        }

        return allData;
    }

    /**
     * Method will sort List of TranslationKeyEntities from input based on sorting pair.
     *
     * @param sortPair Pair<String, String> Pair containing the column name and sorting method (asc/desc).
     * @param translationKeys list of entities to sort.
     */
    private void sortTranslationKeys(Pair<String, String> sortPair, List<TranslationKeyEntity> translationKeys) {
        if (sortPair.getSecond().equals("asc"))
            translationKeys.sort(translationKeyComparator.getSortingComparator(sortPair.getFirst()));
        else if (sortPair.getSecond().equals("desc"))
            translationKeys.sort(translationKeyComparator.getSortingComparator(sortPair.getFirst()).reversed());
    }

    /**
     * Return SORTED list of TranslationKeyEntities that are prepared for Datatable TranslationKey's.
     *
     * @return List<TranslationKeyEntity>
     */
    private List<TranslationKeyEntity> getSortedTranslationKeys(HttpServletRequest request) {
        List<TranslationKeyEntity> translationKeys = getAllData(UsersDB.getCurrentUser(request));
        String sort = request.getParameter("sort");
        String[] sortArray = sort.split(",");
        Pair<String, String> sortPair = new Pair<>(sortArray[0], sortArray[1]);

        sortTranslationKeys(sortPair, translationKeys);

        return translationKeys;
    }

    /**
     * Return prepared PAGE for Datatable TranslationKey's. As input data use method "getSortedTranslationKeys".
     * Prepage page with this data and allso set pagination.
     *
     * @param pageable pagination param for page.
     * @return page for Datatable TranslationKey's with sorted translation key's and set pagination.
     */
    public Page<TranslationKeyEntity> getTranslationKeys(HttpServletRequest request, Pageable pageable) {
        List<TranslationKeyEntity> translationKeys = getSortedTranslationKeys(request);

        PagedListHolder<TranslationKeyEntity> holder = new PagedListHolder<>(translationKeys);
        holder.setPageSize(pageable.getPageSize());
        holder.setPage(pageable.getPageNumber());

        return new PageImpl<>(holder.getPageList(), pageable, translationKeys.size());
    }

    /**
     * Method will handle create/edit process of TranslationKeyEntities for SINGLE language defined in entity.
     * @param user
     * @param entity
     * @param reloadPropDB - if true the properties cache will be reloaded after save
     * @return
     */
    public TranslationKeyEntity createOrEditTranslationKeySingleLanguage(Identity user, TranslationKeyEntity entity, boolean reloadPropDB) {
        if (PropDB.canEdit(user, entity.getKey()) == false)
            throw new IllegalArgumentException(Prop.getInstance().getText("components.translation_key.cantEditThisKey"));

        Date updateDate = new Date();
        String key = entity.getKey();
        String lng = entity.getLng();
        String value = entity.getValue();

        TranslationKeyEntity dbEntity = translationKeyRepository.findByKeyAndLng(key, lng);
        String oldValue = null;
        if (dbEntity == null) {
            dbEntity = new TranslationKeyEntity();
            dbEntity.setKey(key);
            dbEntity.setLng(lng);
        } else {
            oldValue = dbEntity.getValue();
        }

        String newValue = PropDB.escapeUnsafeValue(user, lng, key, value);
        if (newValue == null) newValue = "";
        if (oldValue == null || newValue.equals(oldValue)==false) {
            dbEntity.setValue(newValue);
            dbEntity.setUpdateDate(updateDate);
            translationKeyRepository.save(dbEntity);
        }
        if (reloadPropDB) {
            //reloadni textove kluce
            Prop.getInstance(true);
        }
        return dbEntity;
    }

    /**
     * Method will handle create/edit process of TranslationKeyEntities for every supported language.
     *
     * CASE 1, if translation key is not in file or DB, then new DB record will by set (representing new translation key).
     * CASE 2, if translation key is only in file, then new DB record will by set (representing new value of translation key),
     *          even if old value in file and value from EDITOR are same.
     * CASE 3, if translation key have DB value, this value will be updated.
     *
     * !! Every translation key value in specific language is saved in DB or file like stand alone record.
     *
     * @param user actually logged user
     * @param entity entity representing translation key and his values in all supported languages
     * @return same entity that was entered
     */
    public TranslationKeyEntity createOrEditTranslationKey(Identity user, TranslationKeyEntity entity, boolean isImport, String importMode, Set<String> importedColumns) {
        if (PropDB.canEdit(user, entity.getKey()) == false)
            throw new IllegalArgumentException(Prop.getInstance().getText("components.translation_key.cantEditThisKey"));

        Date updateDate = new Date();
        String key = entity.getKey();

        BeanWrapper bw = new BeanWrapperImpl(entity);

        //There are allready some lng translations for this key
        //We must reuse ID's of allready existed entities (just update "updateDate" and "value")
        for(Map.Entry<String, String> entry : getLanguageFieldCombination().entrySet()) {
            String lng = entry.getKey();
            String field = entry.getValue();

            //skip language not in imported excel
            if (isImport && importedColumns!=null && importedColumns.contains("field"+field)==false) continue;

            String value = (String)bw.getPropertyValue("field"+field);

            TranslationKeyEntity dbEntity = null;
            if (isImport) {
                //pri importe pouzivame cache
                dbEntity = getCachedEntity(key, lng);
            } else {
                dbEntity = translationKeyRepository.findByKeyAndLng(key, lng);
            }

            //if entity exist and we are importig ONLY NEW keys, skip
            if (isImport && "onlyNew".equals(importMode) && dbEntity!=null) {
                Logger.debug(getClass(), "createOrEditTranslationKey, skipping key="+key+" lng="+lng+" value="+value+" because of importMode=onlyNew");
                continue;
            }

            String oldValue = null;
            if (dbEntity == null) {
                //ak kluc este nie je v DB a hodnota je prazdna preskoc
                if (Tools.isEmpty(value)) continue;

                dbEntity = new TranslationKeyEntity();
                dbEntity.setKey(key);
                dbEntity.setLng(lng);
            } else {
                oldValue = dbEntity.getValue();
            }

            String newValue = PropDB.escapeUnsafeValue(user, lng, key, value);
            if (newValue == null) newValue = "";
            //ak obe nie su zadane preskoc (asi prazdny stlpec napr. spanielsky v exceli)
            if (Tools.isEmpty(oldValue) && Tools.isEmpty(newValue)) continue;

            if (oldValue == null || newValue.equals(oldValue)==false) {
                dbEntity.setValue(newValue);
                dbEntity.setUpdateDate(updateDate);
                translationKeyRepository.save(dbEntity);

                //auditujeme kazdy zaznam len ked to nie je import, import sa audituje v RestController v afterImportChunk
                if (isImport==false) Adminlog.add(Adminlog.TYPE_PROP_UPDATE, "UPDATE:\nid: "+dbEntity.getId()+"\nprop_key: "+dbEntity.getKey()+"\nvalue:"+newValue+"\nlng:"+lng, dbEntity.getId().intValue(), -1);

                if (oldValue==null) setCachedEntity(dbEntity.getKey(), dbEntity.getLng(), dbEntity);
            }
        }

        //reloadni textove kluce
        if (isImport==false) {
            Prop.getInstance(true);

            //reload caches, so there will not be concurrentModificationException
            for(Map.Entry<String, String> entry : getLanguageFieldCombination().entrySet()) {
                String lng = entry.getKey();
                Prop.getInstance(lng);
            }
        }

        //aktualizuj date cache
        setLastUpdateDate(key, updateDate);

        return entity;
    }

    /**
     * Method will check is user have rights to delete entered entity. If yes, entity will be deleted. If no, throw IllegalArgumentException.

     * @param user actualy logged user
     * @param translationKey translation key entity we want to delete
     * @param lng language variant of translation key we want delete
     * @return number of deleted entities
     */
    public Long delete(Identity user, String key, String lng) {
        if (PropDB.canEdit(user, key) == false)
            throw new IllegalArgumentException(Prop.getInstance().getText("components.translation_key.cantEditThisKey"));

        setLastUpdateDate(key, null);

        String oldValue = Prop.getInstance(lng).getText(key);

        Long id = translationKeyRepository.deleteByKeyAndLng(key, lng);

        int idInt = -1;
        if (id != null) idInt = id.intValue();

        Adminlog.add(Adminlog.TYPE_PROP_DELETE, "DELETE:\nid: "+id+"\nprop_key: "+key+"\n:value:"+oldValue+"\nlng:"+lng, idInt, -1);

        //reloadni textove kluce
        Prop.getInstance(true);

        return id;
    }

    /**
     * Metoda sluzi na filtrovanie TranslationKeyEntit.
     *
     * @param searchMap Map<String, String> Parametre pouzite pri filtrovani TranslationKeyProperties.
     * @param sortPair  Pair<String, String> Pair v ktorom sa nachadza meno stlpca a sposob sortovana (asc/desc).
     * @param pageable  Pageable
     * @param request   HttpServletRequest
     * @return Page<TranslationKeyEntity>
     */
    Page<TranslationKeyEntity> getFilteredTranslationKeys(Map<String, String> searchMap, Pair<String, String> sortPair, Pageable pageable, HttpServletRequest request) {
        List<TranslationKeyEntity> translationKeys = getAllData(UsersDB.getCurrentUser(request));
        List<TranslationKeyEntity> filteredTranslationKeys = new ArrayList<>();

        int searchMapSize = searchMap.size();
        for (TranslationKeyEntity entity : translationKeys) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
            //we are using AND between conditions, so we must count
            int presentCount = 0;
            for (Map.Entry<String, String> searchParameter : searchMap.entrySet()) {
                if (searchParameter.getKey().equals("updateDate")) {
                    Pair<Date, Date> datePair = getCleanUpdateDateValue(searchParameter.getValue());

                    if (null != entity.getUpdateDate() && entity.getUpdateDate().after(datePair.first) && entity.getUpdateDate().before(datePair.second)) {
                        presentCount++;
                    }
                } else {
                    String entityValue = (String) beanWrapper.getPropertyValue(searchParameter.getKey());

                    if (null == entityValue) break;

                    String value = searchParameter.getValue().toLowerCase();
                    entityValue = entityValue.toLowerCase();

                    if (
                        (value.startsWith("^") && value.endsWith("$") && entityValue.equals(value.substring(1, value.length()-1))) ||
                        (value.startsWith("^") && entityValue.startsWith(value.substring(1))) ||
                        (value.endsWith("$") && entityValue.endsWith(value.substring(0, value.length()-1))) ||
                        (entityValue.contains(value))
                     ) presentCount++;
                }
            }
            //we are using AND, so count must match searchMap.size
            if (presentCount==searchMapSize) filteredTranslationKeys.add(entity);
        }

        sortTranslationKeys(sortPair, filteredTranslationKeys);

        PagedListHolder<TranslationKeyEntity> holder = new PagedListHolder<>(filteredTranslationKeys);
        holder.setPage(pageable.getPageNumber());
        holder.setPageSize(pageable.getPageSize());

        return new PageImpl<>(holder.getPageList(), pageable, filteredTranslationKeys.size());
    }

    /**
     * Metoda sa pouziva na rozpoznanie search parametrov, ktore sa pouzivaju pri filtrovani.
     *
     * @param param String parameter.
     * @return boolean
     */
    boolean checkSearchParam(String param) {
        return param.startsWith("search");
    }

    /**
     * Metoda sa pouziva na rozpoznanie sort parametra, ktory sa pouziva pri sortovani.
     *
     * @param param String parameter.
     * @return boolean
     */
    boolean checkSortParam(String param) {
        return param.equals("sort");
    }

    /**
     * Metoda sluzi na vratenie dvojice datumov od/do.
     *
     * @param updateDate String skala akceptovatelnych datumov.
     * @return Pair<Date, Date>
     */
    private Pair<Date, Date> getCleanUpdateDateValue(String updateDate) {
        String filteredDate = updateDate.replace("daterange:", "");
        String[] stringDateArray = new String[2];
        String[] values = Tools.getTokens(filteredDate, "-");

        if (values.length==2) {
            stringDateArray = values;
        } else if (values.length==1) {
            //ked nemame from pride to ako: daterange:-1589666400000
            if (updateDate.contains("range:-")) {
                stringDateArray[0] = "";
                stringDateArray[1] = filteredDate.replace("-", "");
            } else {
                stringDateArray[0] = filteredDate.replace("-", "");
                stringDateArray[1] = "";
            }
        }

        long[] longDateArray = new long[]{Tools.getLongValue(stringDateArray[0], 0), Tools.getLongValue(stringDateArray[1], new Date().getTime())};

        Date dateFrom = new Date(longDateArray[0]);
        Date dateTo = new Date(longDateArray[1]);
        return new Pair<>(dateFrom, dateTo);
    }

    /**
     * Method will return TranslationKeyRepository set in gloval variable of this Service.
     *
     * @return TranslationKeyRepository
     */
    public TranslationKeyRepository getTranslationKeyRepository() { return translationKeyRepository; }

    /**
     * Return cached map od key-lastUpdateDate
     * @return
     */
    private Map<String, Date> getUpdateDateCache() {
        String cacheKey = "TranslationKeyService.updateDateMap";
        Cache c = Cache.getInstance();
        @SuppressWarnings("unchecked")
        Map<String, Date> updateMap = (Map<String, Date>)c.getObject(cacheKey);
        if (updateMap == null) {
            //musime mapu naplnit z DB
            updateMap = new Hashtable<>();

            List<TranslationKeyEntity> all = translationKeyRepository.findAllByOrderByUpdateDateAsc();
            for (TranslationKeyEntity entity : all) {
                if (entity.getUpdateDate()!=null) updateMap.put(entity.getKey(), entity.getUpdateDate());
            }

            c.setObjectSeconds(cacheKey, updateMap, 10*60, true);
        }
        return updateMap;
    }

    /**
     * Returns last update date for key from cache
     * @param key
     * @return
     */
    private Date getLastUpdateDate(String key) {
        Map<String, Date> updateMap = getUpdateDateCache();
        Date d = updateMap.get(key);
        if (d==null && key.contains("&")) {
            key = Tools.replace(key, "&lt;", "<");
            key = Tools.replace(key, "&gt;", ">");
            d = updateMap.get(key);
        }
        return d;
    }

    /**
     * Set last update date into cache
     * @param key
     * @param date - date OR NULL to delete key from cache
     */
    private void setLastUpdateDate(String key, Date date) {
        Map<String, Date> updateMap = getUpdateDateCache();
        if (date == null) updateMap.remove(key);
        else updateMap.put(key, date);
    }

    private TranslationKeyEntity getCachedEntity(String key, String language) {
        return getRowIdCache().get(getRowIdKey(key, language));
    }

    private void setCachedEntity(String key, String language, TranslationKeyEntity entity) {
        getRowIdCache().put(getRowIdKey(key, language), entity);
    }

    /**
     * For import of large file it's faster to have local Map of all keys instead of lookup it in DB for every row
     * @return
     */
    private Map<String, TranslationKeyEntity> getRowIdCache() {
        String CACHE_KEY = "TranslationKeyService.rowIdCache";
        Cache c = Cache.getInstance();
        @SuppressWarnings("unchecked")
        Map<String, TranslationKeyEntity> rowIdMap = (Map<String, TranslationKeyEntity>)c.getObject(CACHE_KEY);

        if (DatatableRestControllerV2.getLastImportedRow()!=null && DatatableRestControllerV2.getLastImportedRow().intValue()==1) {
            //reset cache on first row
            rowIdMap = null;
        }

        if (rowIdMap != null) return rowIdMap;

        rowIdMap = new HashMap<>();

        List<TranslationKeyEntity> allItems = translationKeyRepository.findAll();
        for (TranslationKeyEntity e : allItems) {
            rowIdMap.put(getRowIdKey(e.getKey(), e.getLng()), e);

        }

        c.setObject(CACHE_KEY, rowIdMap, 10);

        return rowIdMap;
    }

    private String getRowIdKey(String key, String language) {
        return key+"="+language;
    }

    /**
     * Simple method to save translation
     * @param key
     * @param translation
     * @param lng
     */
    public TranslationKeyEntity saveTranslation(String key, String translation, String lng) {
        TranslationKeyEntity entity = translationKeyRepository.findByKeyAndLng(key, lng);
        if (entity == null) {
            entity = new TranslationKeyEntity();
            entity.setKey(key);
        }
        entity.setUpdateDate(new Date(Tools.getNow()));
        entity.setValue(translation);
        entity.setLng(lng);
        translationKeyRepository.save(entity);

        //Prop.getInstance(Constants.getServletContext(), lng, true);
        Prop.getInstance(true);
        Prop.deleteMissingText(key, lng);

        return entity;
    }
}