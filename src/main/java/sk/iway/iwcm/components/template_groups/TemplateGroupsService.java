package sk.iway.iwcm.components.template_groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.templates.TemplateDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.i18n.PropDB;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TemplateGroupsService {

    TemplateDetailsService templateDetailsService;

    @Autowired
    public TemplateGroupsService(TemplateDetailsService templateDetailsService) {
        this.templateDetailsService = templateDetailsService;
    }

    List<TemplatesGroupBean> getAllTemplateGroups(HttpServletRequest request) {
        List<TemplatesGroupBean> temp = new ArrayList<>();
        String lng = getLng(request);

        IwayProperties prop = Prop.getChangedProperties(lng, "temp-group-");
        List<TemplatesGroupBean> templateGroupBeans = filterByUser(TemplatesGroupDB.getAllTemplatesGroupsWithCount(), UsersDB.getCurrentUser(request));

        for (TemplatesGroupBean item : templateGroupBeans) {
            item.setProjectName(prop.getProperty("temp-group-" + item.getId() + ".project.name"));
            item.setProjectAuthor(prop.getProperty("temp-group-" + item.getId() + ".project.author"));
            item.setProjectCopyright(prop.getProperty("temp-group-" + item.getId() + ".project.copyright"));
            item.setProjectDeveloper(prop.getProperty("temp-group-" + item.getId() + ".project.developer"));
            item.setProjectGenerator(prop.getProperty("temp-group-" + item.getId() + ".project.generator"));
            item.setProjectFieldA(prop.getProperty("temp-group-" + item.getId() + ".project.field.a"));
            item.setProjectFieldB(prop.getProperty("temp-group-" + item.getId() + ".project.field.b"));
            item.setProjectFieldC(prop.getProperty("temp-group-" + item.getId() + ".project.field.c"));
            item.setProjectFieldD(prop.getProperty("temp-group-" + item.getId() + ".project.field.d"));

            if (InitServlet.isTypeCloud()) {
                //Pre MULTIWEB mozeme zobrazit len nepriradene sablony (id=1) alebo tie, ktore zacinaju na nas domainalias
                if (item.getId() == 1 || item.getProjectName().toLowerCase().startsWith(MultiDomainFilter.getDomainAlias(DocDB.getDomain(request)))) {
                    temp.add(item);
                }
            } else {
                temp.add(item);
            }
        }

        return temp;
    }

    /**
     * If user has only perms for some folders/domains returns only template groups used in his available templates
     * @param user
     * @return
     */
    public List<TemplatesGroupBean> getTemplateGroups(UserDetails user) {
        List<TemplatesGroupBean> all = TemplatesGroupDB.getAllTemplatesGroups();
        return filterByUser(all, user);
    }

    /**
     * If user has only perms for some folders/domains show only template groups used in his available templates
     * @param all
     * @param user
     * @return
     */
    public List<TemplatesGroupBean> filterByUser(List<TemplatesGroupBean> all, UserDetails user) {
        if (Tools.isEmpty(user.getEditableGroups(true))) return all;

        if (CloudToolsForCore.isControllerDomain()) return all;

        List<TemplatesGroupBean> filtered = new ArrayList<>();

        List<TemplateDetails> temps = templateDetailsService.getAllTemplateDetails(user);
        Set<Long> tempGroupIds = new HashSet<>();
        for (TemplateDetails temp : temps) {
            tempGroupIds.add(temp.getTemplatesGroupId());
        }

        for (TemplatesGroupBean group : all) {
            if (tempGroupIds.contains(group.getId())) {
                filtered.add(group);
            }
        }

        return filtered;
    }

    TemplatesGroupBean saveTemplateGroup(TemplatesGroupBean templateGroupBean, HttpServletRequest request) {
        if (InitServlet.isTypeCloud()) {
            String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(request));
            //Pre MULTIWEB mozeme zobrazit len tie, ktore zacinaju na domainalias
            if (!templateGroupBean.getProjectName().toLowerCase().startsWith(domainAlias)) {
                templateGroupBean.setProjectName(domainAlias + "-" + templateGroupBean.getProjectName());
            }
        }

        TemplatesGroupBean optionalTemplatesGroupBean = TemplatesGroupDB.getTemplatesGroupByName(templateGroupBean.getName());
        if (null != optionalTemplatesGroupBean) {
            return null;
        }

        if (TemplatesGroupDB.getInstance().save(templateGroupBean)) {
            String prefix = "temp-group-" + templateGroupBean.getId() + ".project";
            IwayProperties iwayProperties = mapIwayProperties(templateGroupBean);
            // defaultne vytvaram novu skupinu pre vychodzi jazyk
            PropDB.save(null, iwayProperties, getLng(request), prefix, null, true);

            Prop.getInstance(true);
        }

        return templateGroupBean;
    }

    TemplatesGroupBean editTemplateGroupBean(TemplatesGroupBean templateGroupBean, long id, HttpServletRequest request) {
        String lng = getLng(request);
        templateGroupBean.setTemplatesGroupId(id);
        TemplatesGroupBean optionalTemplateGroupBean = TemplatesGroupDB.getInstance().getById(templateGroupBean.getId());
        if (null == optionalTemplateGroupBean) {
            return null;
        }

        if (templateGroupBean.save()) {
            String prefix = "temp-group-" + templateGroupBean.getId() + ".project";
            IwayProperties iwayProperties = mapIwayProperties(templateGroupBean);
            PropDB.save(null, iwayProperties, lng, prefix, null, false);

            Prop.getInstance(true);
        }

        return templateGroupBean;
    }

    boolean deleteTemplateGroupBean(long id) {
        String prefixForDelete = "temp-group-" + id + ".project";
        if (TemplatesGroupDB.delete(id)) {
            new SimpleQuery().execute("DELETE FROM " + ConfDB.PROPERTIES_TABLE_NAME + " WHERE prop_key IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    prefixForDelete + ".name",
                    prefixForDelete + ".author",
                    prefixForDelete + ".copyright",
                    prefixForDelete + ".developer",
                    prefixForDelete + ".generator",
                    prefixForDelete + ".field.a",
                    prefixForDelete + ".field.b",
                    prefixForDelete + ".field.c",
                    prefixForDelete + ".field.d");

            return true;
        }

        return false;
    }

    private IwayProperties mapIwayProperties(TemplatesGroupBean templatesGroupBean) {
        IwayProperties iwayProperties = new IwayProperties();
        iwayProperties.setProperty("name", templatesGroupBean.getProjectName());
        iwayProperties.setProperty("author", templatesGroupBean.getProjectAuthor());
        iwayProperties.setProperty("copyright", templatesGroupBean.getProjectCopyright());
        iwayProperties.setProperty("developer", templatesGroupBean.getProjectDeveloper());
        iwayProperties.setProperty("generator", templatesGroupBean.getProjectGenerator());
        iwayProperties.setProperty("field.a", templatesGroupBean.getProjectFieldA());
        iwayProperties.setProperty("field.b", templatesGroupBean.getProjectFieldB());
        iwayProperties.setProperty("field.c", templatesGroupBean.getProjectFieldC());
        iwayProperties.setProperty("field.d", templatesGroupBean.getProjectFieldD());

        return iwayProperties;
    }

    List<LabelValueDetails> getDirectories(){
        List<LabelValueDetails> listOfDirectories = new ArrayList<>();

        for (String dirName : FileTools.getDirsNames("/templates/")) {
            listOfDirectories.add(new LabelValueDetails("/templates/" + dirName, removeInstallName(dirName)) );
            //pridaj aj dalsiu uroven adresarov
            for (String subDirName : FileTools.getDirsNames("/templates/" + dirName)) {
                listOfDirectories.add(new LabelValueDetails("/templates/" + dirName + "/" + subDirName, removeInstallName(dirName+"/"+subDirName)) );
            }
        }

        return listOfDirectories;
    }

    private String removeInstallName(String path) {
        String installName = Constants.getInstallName();
        String logInstallName = Constants.getLogInstallName();
        if (Tools.isEmpty(logInstallName)) logInstallName = installName;

        //ak nezacina na installName ani na logInstallName je to v poriadku
        if (path.startsWith(installName)==false && path.startsWith(logInstallName)==false) return path;

        //z nejakeho dovodu sa presna zhoda uklada ako /
        if (path.equals(installName) || path.equals(installName+"/") || path.equals(logInstallName) || path.equals(logInstallName+"/")) return "/";

        //odstran logInstallName / installName a vrat bez toho, combine to tam automaticky pouzije
        if (path.startsWith(logInstallName+"/")) return path.substring(logInstallName.length()+1);
        return path.substring(installName.length()+1);
    }

    public List<LabelValueDetails> getInlineEditors(Prop prop){
        List<LabelValueDetails> listOfInlineEditors = new ArrayList<>();
        listOfInlineEditors.add(new LabelValueDetails(prop.getText("editor.type_select.standard.js"), ""));
        listOfInlineEditors.add(new LabelValueDetails(prop.getText("editor.type_select.html.js"), "html"));
        listOfInlineEditors.add(new LabelValueDetails(prop.getText("editor.type_select.page_builder.js"), "pageBuilder"));
        listOfInlineEditors.add(new LabelValueDetails(prop.getText("editor.type_select.grid_editor.js"), "gridEditor"));

        return listOfInlineEditors;
    }

    private String getLng(HttpServletRequest request) {
        String lng = request.getParameter("breadcrumbLanguage");
        if (Tools.isEmpty(lng)) lng = Prop.getLngForJavascript(request);
        if ("cs".equals(lng)) lng = "cz";
        return lng;
    }
}
