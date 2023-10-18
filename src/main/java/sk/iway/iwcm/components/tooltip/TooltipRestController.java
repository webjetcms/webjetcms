package sk.iway.iwcm.components.tooltip;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.dictionary.DictionaryDB;
import sk.iway.iwcm.components.dictionary.model.DictionaryBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/tooltip")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_tooltip')")
@Datatable
public class TooltipRestController extends DatatableRestControllerV2<DictionaryBean, Long> {

    @Autowired
    TooltipRestController(){
        super(null);
    }

    @Override
    public Page<DictionaryBean> getAllItems(Pageable pageable) {

        String domain = null;
        if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
            domain = CloudToolsForCore.getDomainName();
        }

        //Get all items where name = "tooltip" (for tooltip page)
        List<DictionaryBean> items = DictionaryDB.getItems("tooltip", domain, "name");

        DatatablePageImpl<DictionaryBean> page = new DatatablePageImpl<>(items);

        LayoutService ls = new LayoutService(getRequest());
        page.addOptions("language", ls.getLanguages(false, true), "label", "value", false);
        page.addOptions("domain", ls.getLayoutBean().getHeader().getDomains(), null, null, false);

        return page;
    }

    @Override
    public DictionaryBean getOne(@PathVariable("id") long id) {

        //If id isnt -1 return exist entity
        if(id != -1) {
            return DictionaryDB.getDictionary((int)id);
        }

        //Create new empty entity DictionaryBean
        DictionaryBean entity = new DictionaryBean();
        //Set dictionary group (in table we show entyties with value dictionaryGroups = "tooltip")
        entity.setDictionaryGroup("tooltip");

        //Set curent domain as default
        String currentDomain = CloudToolsForCore.getDomainName();
        entity.setDomain(currentDomain);

        return entity;
    }

    @Override
    public DictionaryBean insertItem(DictionaryBean entity) {

        return saveDictionaryBean(entity);
    }

    @Override
    public boolean deleteItem(DictionaryBean entity, long id) {

        Logger.debug(TooltipRestController.class, "SAVE, id="+id);
        if (entity == null) return true;

        //Try delete not null entity
		boolean deleteOK = DictionaryDB.deleteDictionary(entity.getDictionaryId());

        //If deleteOK == false, entity delete failed, set request atrribute errorText and return false
        if (deleteOK == false) {
			Prop prop = Prop.getInstance(getRequest());
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
            return false;
		}

        return true;
    }

    @Override
    public DictionaryBean editItem(DictionaryBean entity, long id) {

        return saveDictionaryBean(entity);
    }

    DictionaryBean saveDictionaryBean(DictionaryBean entity) {
        Logger.debug(TooltipRestController.class, "SAVE");

		String name = entity.getName();
		name = Tools.replace(name, "&nbsp;", " ");
		name = Tools.replace(name, Constants.NON_BREAKING_SPACE, " ");
		entity.setName(name);

        //Try save entity
        boolean saveOK = DictionaryDB.saveTooltip(entity);

        //If saveOK == false, entity saving failed, set request atrribute errorText and return null
        if (saveOK == false) {
            Prop prop = Prop.getInstance(getRequest());
			getRequest().setAttribute("errorText", prop.getText("admin.conf_editor.update_alert"));
            return null;
        }

        return entity;
    }
}
