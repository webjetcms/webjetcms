package sk.iway.iwcm.components.insertScript.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.insertScript.InsertScriptBean;
import sk.iway.iwcm.components.insertScript.InsertScriptDB;
import sk.iway.iwcm.components.insertScript.InsertScriptDocBean;
import sk.iway.iwcm.components.insertScript.InsertScriptGroupBean;
import sk.iway.iwcm.components.insertScript.InsertScriptRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/insert-script")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_insert_script')")
public class InsertScriptRestController extends DatatableRestControllerV2<InsertScriptBean, Long> {
    @Autowired
    public InsertScriptRestController(InsertScriptRepository insertScriptRepository) {
        super(insertScriptRepository);
    }

    @Override
    public Page<InsertScriptBean> getAllItems(Pageable pageable) {
        List<InsertScriptBean> listedBeans = InsertScriptDB.getInstance().getAll();
        DatatablePageImpl<InsertScriptBean> data = new DatatablePageImpl<>(listedBeans);

        Prop prop = getProp();
        data.addOptions("cookieClass", getCookieClassifications(prop), "label", "value", false);

        return data;
    }

    @Override
    public void beforeDuplicate(InsertScriptBean entity) {
        if (entity.getDocIds()!=null) {
            for(InsertScriptDocBean docBean : entity.getDocIds()) {
                docBean.setId(null);
            }
        }
        if (entity.getGroupIds() != null) {
            for(InsertScriptGroupBean groupBean : entity.getGroupIds()) {
                groupBean.setId(null);
            }
        }
        super.beforeDuplicate(entity);
    }

    @Override
    public void beforeSave(InsertScriptBean entity) {
        //nastav datum ulozenia
        entity.setSaveDate(new Date());

        //nastav korektne domainId
        int domainId = CloudToolsForCore.getDomainId();
        entity.setDomainId(domainId);
        //nastav aj na inner objekty
        if (entity.getGroupIds()!=null) {
            for (InsertScriptGroupBean isg : entity.getGroupIds()) {
                isg.setDomainId(domainId);
            }
        }
    }

    public static List<LabelValueDetails> getCookieClassifications(Prop prop) {
        List<LabelValueDetails> options = new ArrayList<>();
        options.add(new LabelValueDetails(prop.getText("components.insert_script.insert_always"), "ALWAYS"));
        //prebehni klasifikacie z konstant a pridaj
        String[] classifications = Tools.getTokens(Constants.getString("gdprCookieClassifications"), ",");
        for (String cls : classifications) {
            String labelKey = "components.cookies.cookie_manager.classification."+cls;
            String label = prop.getText(labelKey);
            if (label.equals(labelKey)) label = cls;

            options.add(new LabelValueDetails(label, cls));
        }
        return options;
    }

    /**
     * Vrati zoznam uz existujucich skupin
     * @param term
     * @return
    */
    @GetMapping("/positions")
    public List<String> getPositionAutocomplete(@RequestParam String term) {

        List<String> ac = new ArrayList<>();

        //Get all where group name is like %term%, and distict because its autocomplete list and we dont want duplicity
        List<InsertScriptBean> groupNamesList =  ((InsertScriptRepository)getRepo()).findDistinctAllByPositionLikeAndDomainId("%" + term + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add group name to autcomplete list "ac"
        for(InsertScriptBean entity : groupNamesList)
            if (ac.contains(entity.getPosition()) == false) ac.add(entity.getPosition());

        return ac;
    }
}
