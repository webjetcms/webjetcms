package sk.iway.iwcm.components.gdpr.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.gdpr.CookieManagerBean;
import sk.iway.iwcm.components.gdpr.CookieManagerDB;
import sk.iway.iwcm.components.translation_keys.jpa.TranslationKeyEntity;
import sk.iway.iwcm.components.translation_keys.rest.TranslationKeyService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 * Sprava cookies - #53881
 * Specifikom je to, ze cookie moze mat opis vo viacerych jazykoch a tieto texty su ukladane do prekladovych klucov
 */
@RestController
@RequestMapping("/admin/rest/cookies")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuGDPR')")
@Datatable
public class CookieManagerRestController extends DatatableRestControllerV2<CookieManagerBean, Long>{

    private TranslationKeyService translationKeyService;

    @Autowired
    public CookieManagerRestController(TranslationKeyService translationKeyService) {
        super(null);
        this.translationKeyService = translationKeyService;
    }

    @Override
    public Page<CookieManagerBean> getAllItems(Pageable pageable) {

        int domainId = CloudToolsForCore.getDomainId();

        CookieManagerDB cookieMangerDB = new CookieManagerDB();

        List<CookieManagerBean> items = cookieMangerDB.findByDomainId(domainId);

        String language = getRequest().getParameter("breadcrumbLanguage");
        Prop prop = Prop.getInstance(language);
        for(CookieManagerBean item : items) {
            setTranslationKeysIntoEntity(item, prop);
        }

        DatatablePageImpl<CookieManagerBean> page = new DatatablePageImpl<>(items);

        //vygeneruj moznosti pre klasifikaciu
        List<LabelValue> classifications = new ArrayList<>();
        for(String classificator: Tools.getTokens(Constants.getString("gdprCookieClassifications"), ",")){
            LabelValue lv = new LabelValue(getProp().getText("components.cookies.cookie_manager.classification."+classificator), classificator);
            classifications.add(lv);
        }
        page.addOptions("classification", classifications, "label", "value", false);

        return page;
    }

    @Override
    public CookieManagerBean getOneItem(long id) {

        CookieManagerDB cookieMangerDB = new CookieManagerDB();

        String language = getRequest().getParameter("breadcrumbLanguage");

        Prop prop = Prop.getInstance(language);

        CookieManagerBean entity;

        if(id != -1) {
            entity = cookieMangerDB.getById((int) id);
            setTranslationKeysIntoEntity(entity, prop);
        } else {
            entity = new CookieManagerBean();
        }

        return entity;
    }

    /**
     * V databaze sa neukladaju hodnoty s opisom, poskytovatelom a platnostou, pretoze su to textove udaje.
     * Tie sa ukladaju do prekladovych klucov, preto je tu komplikovane spracovanie.
     */
    @Override
    public CookieManagerBean insertItem(CookieManagerBean entity) {

        CookieManagerDB cookieMangerDB = new CookieManagerDB();

        createEditTranslationKeysFromEntity(entity);

        entity.setDomainId(CloudToolsForCore.getDomainId());

        //Temporaly save values
        String provider = entity.getProvider();
        String purpouse = entity.getPurpouse();
        String validity = entity.getValidity();

        //Set key values to NULL
        entity.setProvider(null);
        entity.setPurpouse(null);
        entity.setValidity(null);

        //Validity, provider and purpouse are set as NULL (we dont want save them to table)
        cookieMangerDB.save(entity);

        //Set validity, provider and purpouse back to entity (to return) from temporal variables
        entity.setProvider(provider);
        entity.setPurpouse(purpouse);
        entity.setValidity(validity);

        return entity;
    }

    /**
     * V databaze sa neukladaju hodnoty s opisom, poskytovatelom a platnostou, pretoze su to textove udaje.
     * Tie sa ukladaju do prekladovych klucov, preto je tu komplikovane spracovanie.
     */
    @Override
    public CookieManagerBean editItem(CookieManagerBean entity, long id) {

        CookieManagerDB cookieMangerDB = new CookieManagerDB();

        createEditTranslationKeysFromEntity(entity);

        entity.setDomainId(CloudToolsForCore.getDomainId());

        //Temporaly save values
        String provider = entity.getProvider();
        String purpouse = entity.getPurpouse();
        String validity = entity.getValidity();

        //Set key values to NULL
        entity.setProvider(null);
        entity.setPurpouse(null);
        entity.setValidity(null);

        //Validity, provider and purpouse are set as NULL (we dont want save them to table)
        cookieMangerDB.save(entity);

        //Set validity, provider and purpouse back to entity (to return) from temporal variables
        entity.setProvider(provider);
        entity.setPurpouse(purpouse);
        entity.setValidity(validity);

        return entity;
    }

    void setTranslationKeysIntoEntity(CookieManagerBean entity, Prop prop) {
        String providerKey = "components.gdpr.cookies." + entity.getCookieName() + ".provider";
        String purpouseKey = "components.gdpr.cookies." + entity.getCookieName() + ".purpouse";
        String validityKey = "components.gdpr.cookies." + entity.getCookieName() + ".validity";

        String text = prop.getText(providerKey);
        entity.setProvider(text);

        text = prop.getText(purpouseKey);
        entity.setPurpouse(text);

        text = prop.getText(validityKey);
        entity.setValidity(text);
    }

    void createEditTranslationKeysFromEntity(CookieManagerBean entity) {

        Identity user = UsersDB.getCurrentUser(getRequest());
        TranslationKeyEntity translationKeyEntity = new TranslationKeyEntity();
        String prefix = null;

        String language = getRequest().getParameter("breadcrumbLanguage");
        translationKeyEntity.setLng(language);

        String providerKey;
        String purpouseKey;
        String validityKey;

        if(Constants.getBoolean("constantsAliasSearch")) {

            RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();

            if(requestBean!=null && Tools.isNotEmpty(requestBean.getDomain())) {

                prefix = MultiDomainFilter.getDomainAlias(requestBean.getDomain());
            }
        }

        if(Tools.isNotEmpty(prefix)) {
            providerKey = prefix + "-" + "components.gdpr.cookies." + entity.getCookieName() + ".provider";
            purpouseKey = prefix + "-" + "components.gdpr.cookies." + entity.getCookieName() + ".purpouse";
            validityKey = prefix + "-" + "components.gdpr.cookies." + entity.getCookieName() + ".validity";
        } else {
            providerKey = "components.gdpr.cookies." + entity.getCookieName() + ".provider";
            purpouseKey = "components.gdpr.cookies." + entity.getCookieName() + ".purpouse";
            validityKey = "components.gdpr.cookies." + entity.getCookieName() + ".validity";
        }

        //Provider Key
        translationKeyEntity.setKey(providerKey);
        translationKeyEntity.setValue(entity.getProvider());
        translationKeyService.createOrEditTranslationKeySingleLanguage(user, translationKeyEntity, false);

        //Purpouse key
        translationKeyEntity.setKey(purpouseKey);
        translationKeyEntity.setValue(entity.getPurpouse());
        translationKeyService.createOrEditTranslationKeySingleLanguage(user, translationKeyEntity, false);

        //Validity key
        translationKeyEntity.setKey(validityKey);
        translationKeyEntity.setValue(entity.getValidity());
        translationKeyService.createOrEditTranslationKeySingleLanguage(user, translationKeyEntity, true);
    }

    @Override
    public void beforeSave(CookieManagerBean entity) {
        entity.setSaveDate(new Date(Tools.getNow()));
        entity.setUserId(getUser().getUserId());
    }

    @Override
    public boolean deleteItem(CookieManagerBean entity, long id) {

        CookieManagerDB cookieMangerDB = new CookieManagerDB();

        if(cookieMangerDB.delete(entity)) return true;

        return false;
    }
}
