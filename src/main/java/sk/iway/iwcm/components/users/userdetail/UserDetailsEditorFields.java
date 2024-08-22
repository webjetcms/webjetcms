package sk.iway.iwcm.components.users.userdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveEntity;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.elfinder.DirTreeItem;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailsEditorFields extends BaseEditorFields {

    public UserDetailsEditorFields(){
        //konstruktor
    }

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "user.permissions.label", tab = "groupsTab", visible = false, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.admin.editUserGroups"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] permisions;

    //Signalize, if we want send email to user, about adding him into new userGroup
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "users.send_all_user_groups_emails", sortAfter = "", tab = "groupsTab", hidden = true)
    private boolean sendAllUserGroupsEmails;

    //there will be saved userGroupIds before save (old values) to compare and find newly added groups
    private String beforeSaveUserGroupIds;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "groupedit.type_email", tab = "groupsTab", visible = false, editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "menu.email"),
            @DataTableColumnEditorAttr(key = "unselectedValue", value = "") }) })
    private Integer[] emails;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.editableGroups.label", tab = "rightsTab", visible=false, filter=false, orderable=false, className = "dt-tree-group-array-alldomains", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.group.rights"),
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
    })
    private List<GroupDetails> editableGroups;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.editablePages.label", tab = "rightsTab", visible=false, filter=false, orderable=false, className = "dt-tree-page-array-alldomains", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addPage") })
    })
    private List<DocDetails> editablePages;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "approvingTab",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/groups-approve?userId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.users.groups_approve.GroupsApproveEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<GroupsApproveEntity> groupsApprove;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.writableFolders.label", tab = "rightsTab", hidden = true, className = "dt-tree-dir-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.menu.writableFolders"),
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "useredit.addGroup")
        })
    })
    private List<DirTreeItem> writableFolders;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "users.perm_groups", tab = "rightsTab", renderFormat = "dt-format-select", visible = false, orderable = false,
    editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "unselectedValue", value = ""),
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "users.perm_groups") },
            label = "users.perm_groups.label"
        )
        }
    )
    private Integer[] permGroups;

    @DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "components.user.righrs.user_group_rights", tab = "rightsTab", hidden = true,
    editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.user.permissions"),
                @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
    private String[] enabledItems;

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.user.login}]]",
        tab = "personalInfo",
        hidden = true,
        sortAfter = "allowLoginEnd",
        editor = {
            @DataTableColumnEditor(attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-import-updateByColumn", value = "login") })
        }
    )
    //pole pre zobrazenie loginu v DT za lastName, v editore chovane
    public String login;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "[[#{user.admin.password.allowWeak}]]",
        tab = "personalInfo",
        sortAfter = "password",
        hidden = true
    )
    private Boolean allowWeakPassword;

    //List of free fields
    public List<Field> fieldsDefinition;

    //Show groupsApprove As combination of approve mode and group full path
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "components.users.approving",
        className = "allow-html",
        visible = false,
        hiddenEditor = true,
        orderable = false,
        filter = false,
        tab = "approvingTab")
    private String groupsApproveShow;

    private void getModeText(int approveMode, StringBuilder sb, Prop prop) {
        switch (approveMode) {
            case 0:
                sb.append("[").append(prop.getText("useredit.approveMode.approve")).append("]");
                break;
            case 1:
                sb.append("[").append(prop.getText("useredit.approveMode.notify")).append("]");
                break;
            case 2:
                sb.append("[").append(prop.getText("useredit.approveMode.none")).append("]");
                break;
            case 3:
                sb.append("[").append(prop.getText("useredit.approveMode.level2")).append("]");
                break;
        }
    }

    public void fromUserDetailsEntity(UserDetailsEntity userDetailsOriginal, boolean loadSubQueries, HttpServletRequest request, GroupsApproveRepository groupsApproveRepository) {

        //Get list of free fields
        fieldsDefinition = getFields(userDetailsOriginal, "user", 'E');

        //Set user groups Ids into arrays permisions and eMails
        int[] userGroupsIds = Tools.getTokensInt(userDetailsOriginal.getUserGroupsIds(), ",");
        List<Integer[]> splitPermsEmails = UserDetailsService.splitGroupsToPermsAndEmails(userGroupsIds);
        permisions = splitPermsEmails.get(0);
        emails = splitPermsEmails.get(1);

        int userId = -1;
        if (userDetailsOriginal.getId() != null && userDetailsOriginal.getId().intValue() > 0) {
            userId = userDetailsOriginal.getId().intValue();

            //Allready set permGroups so we can see them in table (dont wait or loadSubQueries)

            //Set permission groups
            List<PermissionGroupBean> userPermGroups = UserGroupsDB.getPermissionGroupsFor(userId); //Get actual user perm groups

            //If group size is bigger than 0, push them into permGroup field (this group will be checked in the list of all group)
            if(userPermGroups.size() > 0) {
                permGroups = new Integer[userPermGroups.size()];

                int userPermGroupsCount = 0;
                for(PermissionGroupBean userPermGroup : userPermGroups) {

                    permGroups[userPermGroupsCount] = userPermGroup.getUserPermGroupId();
                    userPermGroupsCount++;
                }
            }
        }

        Prop prop = Prop.getInstance(Constants.getServletContext(), request);
        if(userId > 0) {
            List<GroupsApproveEntity> groups = groupsApproveRepository.findByUserId(userId);
            StringBuilder sb = new StringBuilder("");
            for(GroupsApproveEntity group : groups) {
                getModeText(group.getApproveMode(), sb, prop);
                sb.append(" ");
                if (Tools.isNotEmpty(group.getGroup().getDomainName())) sb.append(group.getGroup().getDomainName()).append(":");
                sb.append(group.getGroup().getFullPath()).append("<br />");
            }
            groupsApproveShow = sb.toString();
        }

        //Set editable groups Ids into List editableGroups
        editableGroups = new ArrayList<>();
        int[] editableGroupsIds = Tools.getTokensInt(userDetailsOriginal.getEditableGroups(), ",");
        if(editableGroupsIds.length > 0) {
            GroupsDB groupsDB = GroupsDB.getInstance();

            for(int editableGroupId : editableGroupsIds) {
                GroupDetails tmp = groupsDB.getGroup(editableGroupId);
                if (tmp != null) editableGroups.add(tmp);
            }
        }

        //Set editable pages Ids into List editablePages
        editablePages = new ArrayList<>();
        int[] editablePagesIds =  Tools.getTokensInt(userDetailsOriginal.getEditablePages(), ",");
        if(editablePagesIds.length > 0) {
            DocDB pagesDB = DocDB.getInstance();
            GroupsDB groupsDB = GroupsDB.getInstance();

            for(int editablePageId : editablePagesIds) {
                DocDetails tmp = pagesDB.getDoc(editablePageId, -1, false);
                if (tmp != null) {
                    tmp.setFullPath(GroupsTreeService.addDomainPrefixToFullPath(tmp, groupsDB));
                    editablePages.add(tmp);
                }
            }
        }

        if (loadSubQueries && userId > 0) {
            //Set permission groups
            List<PermissionGroupBean> userPermGroups = UserGroupsDB.getPermissionGroupsFor(userId); //Get actual user perm groups

            //If group size is bigger than 0, push them into permGroup field (this group will be checked in the list of all group)
            if(userPermGroups.size() > 0) {
                permGroups = new Integer[userPermGroups.size()];

                int userPermGroupsCount = 0;
                for(PermissionGroupBean userPermGroup : userPermGroups) {

                    permGroups[userPermGroupsCount] = userPermGroup.getUserPermGroupId();
                    userPermGroupsCount++;
                }
            }

            //Set disabled/non-disabled items
            Modules modules = Modules.getInstance();
            List<ModuleInfo> allModuleItems = modules.getUserEditItems(prop);

            Identity user = new Identity(UsersDB.getUser(userId));
            UsersDB.loadDisabledItemsFromDB(user, false);
            String enabledItemsKeysString = "";
            Set<String> disabledItems = user.getDisabledItemsTable().keySet();

            if (user.isAdmin()) {
                for(ModuleInfo modulItem : allModuleItems) {
                    boolean disabled = false;

                    for(String key : disabledItems) {
                        if(key.equals(modulItem.getItemKey())) {
                            disabled = true;
                            break;
                        }
                    }

                    if ("editorMiniEdit".equals(modulItem.getItemKey())) {
                        if (disabled) enabledItemsKeysString += "," + MenuService.getPermsIdWithPrefix("editorFullMenu");
                    }
                    else {
                        if(!disabled) enabledItemsKeysString += "," + MenuService.getPermsIdWithPrefix(modulItem.getItemKey());
                    }
                }
            }
            enabledItems = Tools.getTokens(enabledItemsKeysString, ",");

            //writable_folders - kazdy zaznam na novom riadku
            String folders[] = Tools.getTokens(userDetailsOriginal.getWritableFolders(), "\n");
            writableFolders = new ArrayList<>();
            for (String folder : folders) {
                folder = Tools.replace(folder, "*", ""); //ulozeny format je /images/* ale zobrazujeme len images pre jednoduchost
                DirTreeItem item = new DirTreeItem(folder);
                writableFolders.add(item);
            }

            //nastav heslo na Unchanged, aby presla validacia
            userDetailsOriginal.setPassword(UserTools.PASS_UNCHANGED);
        }

        login = userDetailsOriginal.getLogin();

        //Set this Editor fields
        userDetailsOriginal.setEditorFields(this);

        //nastav cervene zobrazenie pre userov, ktory nemaju povolene prihlasenie
        if (UserDetailsService.isUserDisabled(userDetailsOriginal)) addRowClass("is-disabled");
    }

    public void toUserDetailsEntity(UserDetailsEntity userDetailsOriginal, HttpServletRequest request) {

        if (userDetailsOriginal.getEditorFields()==null) return;

        String userGroupIdsString = UserDetailsService.getUserGroupIds(userDetailsOriginal.getEditorFields().getPermisions(), userDetailsOriginal.getEditorFields().getEmails());
        //Set new string of selected useg group ids
        userDetailsOriginal.setUserGroupsIds(userGroupIdsString);

        //Get editable group ids and add them to string
        List<GroupDetails> seletedEditableGroups = userDetailsOriginal.getEditorFields().getEditableGroups();
        if (seletedEditableGroups != null) {
            String editableGroupIdsString = "";
            for(int i = 0; i < seletedEditableGroups.size(); i++) {

                if(editableGroupIdsString.equals("")) {
                    editableGroupIdsString = "" + seletedEditableGroups.get(i).getGroupId();
                } else {
                    editableGroupIdsString += "," + seletedEditableGroups.get(i).getGroupId();
                }
            }
            //Set new string of selected editabled groups
            userDetailsOriginal.setEditableGroups(editableGroupIdsString);
        }

        //Get editable page ids and add them to string
        List<DocDetails> selectedEditablePages =  userDetailsOriginal.getEditorFields().getEditablePages();
        if (selectedEditablePages != null) {
            String editablePageIdsString = "";
            for(int i = 0; i < selectedEditablePages.size(); i++) {

                if(editablePageIdsString.equals("")) {
                    editablePageIdsString = "" + selectedEditablePages.get(i).getDocId();
                } else {
                    editablePageIdsString += "," + selectedEditablePages.get(i).getDocId();
                }
            }
            //Set new string of selected editabled pages
            userDetailsOriginal.setEditablePages(editablePageIdsString);
        }

        //writableFolders
        if (userDetailsOriginal.getEditorFields().getWritableFolders()!=null) {
            StringBuilder writableFoldersStr = new StringBuilder();
            for (DirTreeItem dir : userDetailsOriginal.getEditorFields().getWritableFolders()) {
                if (dir == null || Tools.isEmpty(dir.getVirtualPath())) continue;

                if (writableFoldersStr.length()>0) writableFoldersStr.append("\n");
                writableFoldersStr.append(dir.getVirtualPath());
                if (dir.getVirtualPath().endsWith("*")==false) {
                    //WJ kontroluje /images/* a /images/ ale GUI automaticky predpoklada len format /images/*
                    if (dir.getVirtualPath().endsWith("/")==false) writableFoldersStr.append("/");
                    writableFoldersStr.append("*");
                }

            }
            userDetailsOriginal.setWritableFolders(writableFoldersStr.toString());
        }

        //Set login and E-mail from editor fields to original entity
        if (Tools.isNotEmpty(userDetailsOriginal.getEditorFields().getLogin())) userDetailsOriginal.setLogin(userDetailsOriginal.getEditorFields().getLogin());
    }
}
