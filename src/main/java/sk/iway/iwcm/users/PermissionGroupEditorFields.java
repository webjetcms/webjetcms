package sk.iway.iwcm.users;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.elfinder.DirTreeItem;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionGroupEditorFields extends BaseEditorFields {

    public PermissionGroupEditorFields(){
        //empty constructor
    }

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.editableGroups.label", tab = "dirs", hidden = false, className = "dt-tree-group-array-alldomains", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.group.rights"),
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
    })
    private List<GroupDetails> editableGroups;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.editablePages.label", tab = "dirs", hidden = false, className = "dt-tree-page-array-alldomains", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addPage") })
    })
    private List<DocDetails> editablePages;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="user.writableFolders.label", tab = "dirs", hidden = false, className = "dt-tree-dir-array", editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.menu.writableFolders"),
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "useredit.addGroup")
        })
    })
    private List<DirTreeItem> writableFolders;

    @DataTableColumn(inputType = DataTableColumnType.JSTREE, title = "user.permgroups.permissions.title", tab = "perms", hidden = true, editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-jstree-name", value = "jstreePerms") }) })
    private String[] permissions;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "user.permgroups.permissions.title", tab = "perms", hiddenEditor = true)
    private String permissionsText;

    public void fromPermissionGroupBean(PermissionGroupBean permissionGroupOriginal, boolean loadSubQueries, HttpServletRequest request) {

        if(loadSubQueries) {

            //writable_folders - kazdy zaznam na novom riadku
            String folders[] = Tools.getTokens(permissionGroupOriginal.getWritableFolders(), "\n");
            if (folders.length>0){
                writableFolders = new ArrayList<>();
                for (String folder : folders) {
                    folder = Tools.replace(folder, "*", ""); //ulozeny format je /images/* ale zobrazujeme len images pre jednoduchost
                    DirTreeItem item = new DirTreeItem(folder);
                    writableFolders.add(item);
                }
            }


            //Set editable groups Ids into List editableGroups
            int editableGroupsIds[] = Tools.getTokensInt(permissionGroupOriginal.getEditableGroups(), ",");
            if(editableGroupsIds.length > 0) {
                editableGroups = new ArrayList<>();
                GroupsDB groupsDB = GroupsDB.getInstance();

                for(int editableGroupId : editableGroupsIds) {
                    GroupDetails tmp = groupsDB.getGroup(editableGroupId);
                    if (tmp != null) editableGroups.add(tmp);
                }
            }

            //Set editable pages Ids into List editablePages
            int editablePagesIds[] =  Tools.getTokensInt(permissionGroupOriginal.getEditablePages(), ",");
            if(editablePagesIds.length > 0) {
                editablePages = new ArrayList<>();
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

            if (permissionGroupOriginal.getId()!=null && permissionGroupOriginal.getId().longValue()>0) {
                //Set permissions
                List<String> permsNames = permissionGroupOriginal.getPermissionNames();

                Prop prop = Prop.getInstance(Constants.getServletContext(), request);
                Modules modules = Modules.getInstance();
                List<ModuleInfo> allModuleItems = modules.getUserEditItems(prop);

                String enabledPermsNamesString = "";
                StringBuilder enabledPermsTextSb = new StringBuilder();

                for(ModuleInfo modulItem : allModuleItems) {
                    boolean disabled = true;

                    for(String key : permsNames) {
                        if(key.equals(modulItem.getItemKey())) {
                            disabled = false;
                            break;
                        }
                    }

                    if ("editorMiniEdit".equals(modulItem.getItemKey())) {
                        if (disabled) {
                            enabledPermsNamesString += "," + MenuService.getPermsIdWithPrefix("editorFullMenu");

                            if (enabledPermsTextSb.length()>0) enabledPermsTextSb.append(", ");
                            enabledPermsTextSb.append(prop.getText("user.editorFullMenu"));
                        }
                    }
                    else {
                        if(!disabled) {
                            enabledPermsNamesString += "," + MenuService.getPermsIdWithPrefix(modulItem.getItemKey());
                            if (enabledPermsTextSb.length()>0) enabledPermsTextSb.append(", ");
                            enabledPermsTextSb.append(modulItem.getLeftMenuNameKey()); //ide sem sice nameKey, ale zoznam je ziskany s prop objektom a uz je lokalizovany
                        }
                    }
                }
                permissions = Tools.getTokens(enabledPermsNamesString, ",");
                permissionsText = enabledPermsTextSb.toString();
            }
        }
    }

    public void toPermissionGroupBean(PermissionGroupBean permissionGroupOriginal) {

        if (permissionGroupOriginal.getEditorFields().getWritableFolders() != null) {
            //writableFolders
            StringBuilder writableFoldersStr = new StringBuilder();
            for (DirTreeItem dir : permissionGroupOriginal.getEditorFields().getWritableFolders()) {
                if (dir == null || Tools.isEmpty(dir.getVirtualPath())) continue;

                if (writableFoldersStr.length()>0) writableFoldersStr.append("\n");
                writableFoldersStr.append(dir.getVirtualPath());
                if (dir.getVirtualPath().endsWith("*")==false) {
                    //WJ kontroluje /images/* a /images/ ale GUI automaticky predpoklada len format /images/*
                    if (dir.getVirtualPath().endsWith("/")==false) writableFoldersStr.append("/");
                    writableFoldersStr.append("*");
                }

            }
            permissionGroupOriginal.setWritableFolders(writableFoldersStr.toString());
        }

        if (permissionGroupOriginal.getEditorFields().getEditableGroups() != null) {
            //Get editable group ids and add them to string
            List<GroupDetails> seletedEditableGroups = permissionGroupOriginal.getEditorFields().getEditableGroups();
            String editableGroupIdsString = "";
            for(int i = 0; i < seletedEditableGroups.size(); i++) {

                if(editableGroupIdsString.equals("")) {
                    editableGroupIdsString = "" + seletedEditableGroups.get(i).getGroupId();
                } else {
                    editableGroupIdsString += "," + seletedEditableGroups.get(i).getGroupId();
                }
            }
            //Set new string of selected editabled groups
            permissionGroupOriginal.setEditableGroups(editableGroupIdsString);
        }

        if (permissionGroupOriginal.getEditorFields().getEditablePages() != null) {
            //Get editable page ids and add them to string
            List<DocDetails> selectedEditablePages =  permissionGroupOriginal.getEditorFields().getEditablePages();
            String editablePageIdsString = "";
            for(int i = 0; i < selectedEditablePages.size(); i++) {

                if(editablePageIdsString.equals("")) {
                    editablePageIdsString = "" + selectedEditablePages.get(i).getDocId();
                } else {
                    editablePageIdsString += "," + selectedEditablePages.get(i).getDocId();
                }
            }
            //Set new string of selected editabled pages
            permissionGroupOriginal.setEditablePages(editablePageIdsString);
        }

        if (permissionGroupOriginal.getEditorFields().getPermissions() != null) {
            /*
                Get new permissions for permGroup from jsTree (saved in editorFields.permissions) and compare them
                with old permissions for permGroup (saved in permissionGroupOriginal.permissions  -- they are already saved in DB).
            */
            String[] newPermissionsWithPrefix = permissionGroupOriginal.getEditorFields().getPermissions();
            List<String> newPermissions = new ArrayList<>(); //without prefix

            //Fill newPermissions with new perm but remove prefix
            boolean hasEditorFullMenu = false;
            for(String withPrefix : newPermissionsWithPrefix) {
                String permKey = MenuService.removePermsIdPrefix(withPrefix);
                if ("editorFullMenu".equals(permKey)) hasEditorFullMenu = true;
                newPermissions.add(permKey);
            }

            List<String> oldPermissions = permissionGroupOriginal.getPermissionNames();

            boolean newPermNullOrEmpty = false;
            boolean oldPermNullOrEmpty = false;

            if(newPermissions == null || newPermissions.size() == 0) newPermNullOrEmpty = true;
            if(oldPermissions == null || oldPermissions.size() == 0) oldPermNullOrEmpty = true;

            if(newPermNullOrEmpty && oldPermNullOrEmpty) {
                //no action is needed
            } else if(newPermNullOrEmpty && oldPermissions!=null && !oldPermNullOrEmpty) {
                //there are no selected perm for this group (newPermissions), but some perm are already saved in DB (oldPermissions)
                //so we need loop oldPermissions and for every perm call deletePermission

                for(String perm : oldPermissions) {
                    permissionGroupOriginal.deletePermission(perm);
                }
            } else if(!newPermNullOrEmpty && oldPermNullOrEmpty) {
                //there are selected perm for this group (newPermissions), and no other perm are saved in DB (oldPermissions)
                //so we need loop newPermissions and for every perm call addPermission

                for(String perm : newPermissions) {
                    permissionGroupOriginal.addPermission(perm);
                }
            } else {
                //there are selected perm for this group (newPermissions), and also there are already saved perm in DB (oldPermissions)
                //so we need loop both newPermissions/oldPermissions and find if they are same (if not, update perm in DB)

                //loop old perms and find if perm is inside new perms (if not, delete this perm)
                if (oldPermissions != null) {
                    for(String oldPerm : oldPermissions) {
                        boolean toDelete = true;

                        for(String newPerm : newPermissions) {

                            if(oldPerm.equals(newPerm)) {
                                toDelete = false;
                                break;
                            }
                        }

                        if(toDelete) {
                            Logger.debug(PermissionGroupEditorFields.class, "deleting perm: "+oldPerm);
                            permissionGroupOriginal.deletePermission(oldPerm);
                        } else {
                            Logger.debug(PermissionGroupEditorFields.class, "keeping perm: "+oldPerm);
                        }
                    }
                }

                //loop new perms and find if perm is inside old perms (if not, add this perm)
                for(String newPerm : newPermissions) {
                    boolean toAdd = true;

                    if (oldPermissions != null) {
                        for(String oldPerm : oldPermissions) {
                            if(newPerm.equals(oldPerm)) {
                                toAdd = false;
                                break;
                            }
                        }
                    }

                    if(toAdd) {
                        Logger.debug(PermissionGroupEditorFields.class, "adding perm: "+newPerm);
                        permissionGroupOriginal.addPermission(newPerm);
                    }
                }
            }

            //otoc spravanie editorMiniEdit
            if (hasEditorFullMenu) {
                permissionGroupOriginal.deletePermission("editorMiniEdit");
            } else {
                permissionGroupOriginal.addPermission("editorMiniEdit");
            }
            permissionGroupOriginal.deletePermission("editorFullMenu");

        }
    }
}
