package sk.iway.iwcm.components.translation_keys.rest;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEditorFields;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.utils.Pair;

@RestController
@Datatable
@RequestMapping(value = "/admin/v9/settings/translation-keys")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('edit_text')")
public class TranslationKeyController extends DatatableRestControllerV2<TranslationKeyEntity, Long> {

    private TranslationKeyService translationKeyService;

    @Autowired
    public TranslationKeyController(TranslationKeyService translationKeyService) {
        super(translationKeyService.getTranslationKeyRepository());
        //Logger.debug(TranslationKeyController.class, "TranslationKeyController.constructor v2, service="+translationKeyService);
        this.translationKeyService = translationKeyService;
    }

    @Override
    public Page<TranslationKeyEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public TranslationKeyEntity insertItem(TranslationKeyEntity translationKeyEntity) {
        TranslationKeyEntity saved = translationKeyService.createOrEditTranslationKey(getUser(), translationKeyEntity, isImporting(), getImportMode(), getImportedColumns());
        setForceReload(true);
        return saved;
    }

    @Override
    public TranslationKeyEntity editItem(TranslationKeyEntity translationKeyEntity, long id) {
        TranslationKeyEntity saved = translationKeyService.createOrEditTranslationKey(getUser(), translationKeyEntity, isImporting(), getImportMode(), getImportedColumns());
        setForceReload(true);
        return saved;
    }

    @Override
    public boolean deleteItem(TranslationKeyEntity entity, long id) {
        Map<String, String> languageFieldCombination = translationKeyService.getLanguageFieldCombination();
        TranslationKeyRepository repo = translationKeyService.getTranslationKeyRepository();
        String key = entity.getKey();
        List<String> cantBeDelete = new ArrayList<>();

        //Get value from custom-fields
        //Every lng have alphabet combination that says in which field is this lng value
        for(Map.Entry<String, String> entry : languageFieldCombination.entrySet()) {
            String lng = entry.getKey();

            //Now check if this key+lng combination is in DB
            TranslationKeyEntity fromDB = repo.findByKeyAndLng(key, lng);

            //If value for this key value combination exist but not in DB,
            // we cant delete this translation, because its value from file
            if(fromDB == null) {
                Prop lngProp = Prop.getInstance(lng);
                String translation = lngProp.getTextProp(key);
                if (translation!=null) {
                    //key exists ONLY on properties file, notify that it can't be deleted from DB
                    cantBeDelete.add(lng.toUpperCase());
                }
                continue;
            }
            translationKeyService.delete(getUser(), key, lng);
        }

        //Check if same values cant be deleted, if yes show notification
        if(cantBeDelete.isEmpty()==false) {
            String notifyMsg = getProp().getText("components.translation_key.deletePropFile.desc", String.join(",", cantBeDelete));
            //Show notification
            addNotify(new NotifyBean(getProp().getText("components.translation_key.deletePropFile.title"), notifyMsg, NotifyType.INFO, 10000));
        }

        setForceReload(true);
        Prop.getInstance(true);
        return true;
    }

    @Override
    public Page<TranslationKeyEntity> searchItem(Map<String, String> params, Pageable pageable, TranslationKeyEntity search) {
        String sortKey = null;
        String sortValue = null;
        Map<String, String> searchMap = new HashMap<>();

        for (Map.Entry<String, String> paramsEntry : params.entrySet()) {
            if (translationKeyService.checkSearchParam(paramsEntry.getKey())) {
                String key = getCleanKey(paramsEntry.getKey());
                String value = paramsEntry.getValue(); //getCleanValue(paramsEntry.getValue());
                if ("updateDate".equals(key)) {
                    value = paramsEntry.getValue();
                }
                searchMap.put(key, value);
            } else if (translationKeyService.checkSortParam(paramsEntry.getKey())) {
                String[] sortParams = paramsEntry.getValue().split(",");
                sortKey = sortParams[0];
                sortValue = sortParams[1];
            }
        }

        Pair<String, String> sortPair = new Pair<>(sortKey, sortValue);
        Page<TranslationKeyEntity> filtered = translationKeyService.getFilteredTranslationKeys(searchMap, sortPair, pageable, getRequest());
        processFromEntity(filtered, ProcessItemAction.GETALL);
        return filtered;
    }

    @Override
    public boolean processAction(TranslationKeyEntity entity, String action) {
        if ("reload".equals(action)) {
            Prop.getInstance(true);
            setForceReload(true);
            return true;
        }
        return false;
    }

    @Override
    public TranslationKeyEntity processFromEntity(TranslationKeyEntity entity, ProcessItemAction action) {
        if(entity != null && entity.getEditorFields() == null) {
            TranslationKeyEditorFields tkef = new TranslationKeyEditorFields();
            tkef.from(translationKeyService.getLanguageFieldCombination(), action);
            entity.setEditorFields(tkef);
        }
        return entity;
    }

    @Override
    public void afterImportChunk(int chunk, int totalChunks) {
        Logger.debug(getClass(), "afterImportChunk, chunk="+chunk+" totalChunks="+totalChunks);
        Adminlog.add(Adminlog.TYPE_PROP_UPDATE, "Importing keys, chunk="+(chunk+1)+"/"+totalChunks, -1, -1);
        if (chunk+1>=totalChunks) Prop.getInstance(true);
    }

    @Override
    public List<TranslationKeyEntity> findItemBy(String propertyName, TranslationKeyEntity original)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        //always return empty list, because test for existence is done in TranslationKeyService for every lng column
        List<TranslationKeyEntity> list = new ArrayList<>();
        return list;
    }


}
