package sk.iway.iwcm.components.gdpr.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.gdpr.GdprModule;
import sk.iway.iwcm.components.gdpr.GdprSearchEntity;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.components.gdpr.model.GdprResult;
import sk.iway.iwcm.components.gdpr.model.GdprResults;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Vyhladavanie v aplikaciach/moduloch pre GDPR
 */
@RestController
@RequestMapping("/admin/rest/gdpr/search")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuGDPRregexp')")
@Datatable
public class GdprSearchRestController extends DatatableRestControllerV2<GdprSearchEntity, Long>{


    @Autowired
    public GdprSearchRestController() {
        super(null);
    }

    @Override
    public Page<GdprSearchEntity> getAllItems(Pageable pageable) {
        //all vracia prazdny zoznam, az spustenie vyhladavania findByColumns nieco realne vykona

        List<GdprSearchEntity> items = new ArrayList<>();
        DatatablePageImpl<GdprSearchEntity> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public Page<GdprSearchEntity> searchItem(Map<String, String> params, Pageable pageable, GdprSearchEntity entity) {

        List<GdprRegExpBean> regexps;
        List<GdprModule> modules = new ArrayList<>();
        List<GdprSearchEntity> searchList = new ArrayList<>();

        //We will search default in all modules
        modules.add(GdprModule.WEB_STRANKY);
        modules.add(GdprModule.FORMULARE);
        modules.add(GdprModule.QA);
        modules.add(GdprModule.FORUM);

        //Get filtring param searchValue
        String searchValue = params.get("searchValue");

        //Set regexp (searchValue is used as regexp value and also regexp name)
        regexps = new ArrayList<>();
        GdprRegExpBean gdprBean = new GdprRegExpBean();
        gdprBean.setRegexpValue(searchValue);
        gdprBean.setRegexpName(searchValue);
        regexps.add(gdprBean);

        //Fill result list for every module
        GdprResults results = new GdprResults();
        for (GdprModule module : modules) {

            //Find regexps for specific modul
            //Result list format is Map<GdprModule, List<GdprResult>>
            results.put(module, module.getDB().search(regexps, getRequest()));
        }

        //Loop all moduls
        for(GdprModule module : modules) {

            //For modul get list of GdprResult from result(Map<GdprModule, List<GdprResult>>)
            List<GdprResult> list = new ArrayList<>();
            list = results.get(module);

            //Call getModuleName proc to obtain modul name from translation-key text
            String moduleName = getModuleName(module);

            //Loop list of returned GdprResult entities
            for(GdprResult listEntity : list) {

                //Create temporal GdprSearchEntity and copy value from original GdprResult entity to temporal GdprSearchEntity
                GdprSearchEntity tmp = new GdprSearchEntity();

                tmp.setId( Long.valueOf(listEntity.getId()) );
                tmp.setName(listEntity.getName());
                tmp.setModul(moduleName);
                tmp.setValue(listEntity.getText());
                //tmp.setUrl(listEntity.getLinkView());

                String url = listEntity.getLink();
                if (url.startsWith("/admin/formlist.do")) {
                    url = "/apps/form/admin/?id="+listEntity.getId()+"#/detail/"+listEntity.getName();
                } else if (url.startsWith("/components/qa/admin_answer.jsp")) {
                    url = "/apps/qa/admin/?id="+listEntity.getId();
                } else if (url.startsWith("/admin/webpages")) {
                    url = "/admin/v9/webpages/web-pages-list/?docid="+listEntity.getId();
                }

                tmp.setUrl(url);

                //This  temporal GdprSearchEntity add inside searchList
                searchList.add(tmp);
            }
        }

        //Convert searchList into page and return it
        DatatablePageImpl<GdprSearchEntity> page;
        page = new DatatablePageImpl<>(searchList);

        for (GdprModule module : modules) {
            String name = getModuleName(module);
            page.addOption("modul", name, name, null);
        }

        return page;
    }

    //Return translation-key text, of set proc param (GdprModule)
    String getModuleName(GdprModule modul) {

        Prop prop = Prop.getInstance();

        if(modul == GdprModule.WEB_STRANKY) {
            return prop.getText("components.gdpr.search.module.web_stranky");
        } else if(modul == GdprModule.FORMULARE) {
            return prop.getText("components.gdpr.search.module.forms");
        } else if(modul == GdprModule.QA) {
            return prop.getText("components.gdpr.search.module.qa");
        } else if(modul == GdprModule.FORUM) {
            return prop.getText("components.gdpr.search.module.forum");
        }

        return "";
    }


    //Bonus security
    //Preventing user from editing/inserting/deleting entity
    @Override
	public GdprSearchEntity editItem(GdprSearchEntity entity, long logId) {
		throwError("datatables.error.recordIsNotEditable");
        return null;
	}

    @Override
	public GdprSearchEntity insertItem(GdprSearchEntity entity) {
		throwError("datatables.error.recordIsNotEditable");
        return null;
	}

	@Override
    public boolean beforeDelete(GdprSearchEntity entity) {
        return false;
    }
}
