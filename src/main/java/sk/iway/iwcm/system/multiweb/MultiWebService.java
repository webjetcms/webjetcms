package sk.iway.iwcm.system.multiweb;

import java.io.File;
import java.util.Map;

import sk.iway.Password;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class MultiWebService {

    private final GroupDetails entity;
    private final UserDetails controllerUser;
    private final Prop prop;
    private NotifyBean userNotify;
    private final Map<String, Integer> systemDocIds;
    private String domainAlias;
    private final GroupDetails localSystemGroup;

    public MultiWebService(GroupDetails entity, GroupDetails localSystemGroup, UserDetails controllerUser, Map<String, Integer> systemDocIds, Prop prop) {
        this.entity = entity;
        this.localSystemGroup = localSystemGroup;
        this.controllerUser = controllerUser;
        this.systemDocIds = systemDocIds;
        this.prop = prop;
    }

    public void createNewDomain() {
        boolean userCreated = createUser();
        if (userCreated) {
            domainAlias = FilePathTools.getDomainBaseFolder(entity.getDomainName());
            //get last part with domain name
            domainAlias = domainAlias.substring(domainAlias.lastIndexOf(""+File.separatorChar)+1);
            int dot = domainAlias.indexOf(".");
            if (dot > 0) {
                //use only main part
                domainAlias = domainAlias.substring(0, dot);
            }

            //set domain alias
            ConfDB.setName("multiDomainAlias:"+entity.getDomainName(), domainAlias);

            //clone template
            createTemplates();

            //refresh objects in memory
            DocDB.getInstance(true);
            GroupsDB.getInstance(true);
            TemplatesDB.getInstance(true);
        }
    }

    private boolean createUser() {
        if ((new SimpleQuery()).forInt("SELECT COUNT(*) FROM users WHERE domain_id=?", entity.getGroupId()) == 0) {
            String password = Password.generateStrongPassword();

            //duplicate current user into new domain
            controllerUser.setLogin("admin");
            controllerUser.setFirstName(DB.prepareString(entity.getDomainName(), 128));
            controllerUser.setUserId(-1);
            controllerUser.setPassword(password);
            UsersDB.saveUser(controllerUser);

            //add notify to user with created password for new user in new domain
            StringBuilder text = new StringBuilder();

            text.append(prop.getText("groupedit.domain")).append(": ").append(entity.getDomainName());
            text.append(" <br/>");
            text.append(prop.getText("components.user.login")).append(": ").append(controllerUser.getLogin());
            text.append(" <br/>");
            text.append(prop.getText("logon.password")).append(": ").append(ResponseUtils.filter(password));

            userNotify = new NotifyBean(prop.getText("users.newUser"), text.toString(), NotifyBean.NotifyType.INFO);
            return true;
        }
        return false;
    }

    private void createTemplates() {
        //create template group for new domain
        TemplatesGroupBean templatesGroupBean = new TemplatesGroupBean();
        templatesGroupBean.setName(domainAlias);
        templatesGroupBean.setDirectory(domainAlias);
        templatesGroupBean.setKeyPrefix(domainAlias);
        templatesGroupBean.setProjectName(entity.getDomainName());
        templatesGroupBean.save();

        int tempId = entity.getTempId();
        TemplatesDB tempDB = TemplatesDB.getInstance();
        TemplateDetails templateDetails = tempDB.getTemplate(tempId);
        if (templateDetails != null) {
            templateDetails.setTempId(-1);
            templateDetails.setTempName(domainAlias + " " + templateDetails.getTempName());
            templateDetails.setForward(domainAlias + "/" + templateDetails.getForward());

            templateDetails.setTemplatesGroupId(templatesGroupBean.getId());

            //set docids
            if (systemDocIds.get("groupslist.system.header") != null) templateDetails.setHeaderDocId(systemDocIds.get("groupslist.system.header"));
            if (systemDocIds.get("groupslist.system.footer") != null) templateDetails.setFooterDocId(systemDocIds.get("groupslist.system.footer"));
            if (systemDocIds.get("groupslist.system.menu") != null) templateDetails.setMenuDocId(systemDocIds.get("groupslist.system.menu"));
            templateDetails.setRightMenuDocId(-1);
            templateDetails.setObjectADocId(-1);
            templateDetails.setObjectBDocId(-1);
            templateDetails.setObjectCDocId(-1);
            templateDetails.setObjectDDocId(-1);
            templateDetails.setAvailableGroups(entity.getGroupId()+","+localSystemGroup.getGroupId());

            templateDetails.setTemplateInstallName(domainAlias);

            tempDB.saveTemplate(templateDetails);
            tempId = templateDetails.getTempId();
            TemplatesDB.getInstance(true);

            //update existing domain groups with new template
            new SimpleQuery().execute("UPDATE groups SET temp_id=? WHERE domain_name=?", tempId, entity.getDomainName());
            //update existing pages with new template
            new SimpleQuery().execute("UPDATE documents SET temp_id=? WHERE root_group_l1=? OR root_group_l1=?", tempId, entity.getGroupId(), localSystemGroup.getGroupId());
        }
    }

    public NotifyBean getUserNotify() {
        return userNotify;
    }
}
