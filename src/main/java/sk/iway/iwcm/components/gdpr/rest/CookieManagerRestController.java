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
            int domainId = CloudToolsForCore.getDomainId();
            if(entity == null || entity.getDomainId() != domainId) {
                return null;
            }
            setTranslationKeysIntoEntity(entity, prop);
        } else {
            entity = new CookieManagerBean();
        }

        return entity;
    }

    /**
     * Checks the stored domain before allowing access to an existing cookie.
     *
     * @param entity submitted cookie
     * @param id cookie ID, or -1 for a new cookie
     * @return whether the cookie belongs to the current domain or is new
     */
    @Override
    public boolean checkItemPerms(CookieManagerBean entity, Long id) {
        if (id == null || entity == null) return false;
        if (id == -1) return entity.getId() < 1;
        if (id < 1 || entity.getId() != id.longValue()) return false;
        CookieManagerBean stored = new CookieManagerDB().getById(id);
        return stored != null && stored.getDomainId() == CloudToolsForCore.getDomainId();
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
        entity.setProvider(CookieManagerDB.getCookieText(prop, entity.getCookieName(), "provider"));
        entity.setPurpouse(CookieManagerDB.getCookieText(prop, entity.getCookieName(), "purpouse"));
        entity.setValidity(CookieManagerDB.getCookieText(prop, entity.getCookieName(), "validity"));
    }

    void createEditTranslationKeysFromEntity(CookieManagerBean entity) {

        Identity user = UsersDB.getCurrentUser(getRequest());
        TranslationKeyEntity translationKeyEntity = new TranslationKeyEntity();

        String language = getRequest().getParameter("breadcrumbLanguage");
        translationKeyEntity.setLng(language);

        String providerKey = CookieManagerDB.getTranslationKey(entity.getCookieName(), "provider");
        String purpouseKey = CookieManagerDB.getTranslationKey(entity.getCookieName(), "purpouse");
        String validityKey = CookieManagerDB.getTranslationKey(entity.getCookieName(), "validity");

        // The controller authorizes cookie management; these keys are generated for the current domain.
        //Provider Key
        translationKeyEntity.setKey(providerKey);
        translationKeyEntity.setValue(entity.getProvider());
        translationKeyService.saveTranslation(user, translationKeyEntity, false);

        //Purpouse key
        translationKeyEntity.setKey(purpouseKey);
        translationKeyEntity.setValue(entity.getPurpouse());
        translationKeyService.saveTranslation(user, translationKeyEntity, false);

        //Validity key
        translationKeyEntity.setKey(validityKey);
        translationKeyEntity.setValue(entity.getValidity());
        translationKeyService.saveTranslation(user, translationKeyEntity, true);
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
