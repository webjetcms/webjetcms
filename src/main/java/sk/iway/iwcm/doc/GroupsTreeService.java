package sk.iway.iwcm.doc;

import org.springframework.stereotype.Service;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.rest.WebPagesListener;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupsTreeService {

    /**
     *
     * @param user
     * @param id - id priecinka
     * @param showPages - ak je true, vratia sa v zozname aj web stranky
     * @param click - typ zobrazenia/filtrovania
     * @param requestedDomain - pozadovana domena pre filtrovanie alebo NULL pre filter podla aktualne nastavenej domeny
     * @param request
     * @return
     */
    public List<JsTreeItem> getItems(Identity user, int id, boolean showPages, String click, String requestedDomain, HttpServletRequest request) {
        GroupsDB groupsDB = GroupsDB.getInstance();
        DocDB docDB = DocDB.getInstance();
        int idOriginal = id;

        String treeSearchValue = Tools.getStringValue(request.getParameter("treeSearchValue"), null);
        if (isSystemRequested(click) && id<1) {
            if (Constants.getBoolean("templatesUseRecursiveSystemFolder")==false) {
                GroupDetails systemGroupDetails = GroupsService.getSystemGroupDetails();
                if (systemGroupDetails != null) id = systemGroupDetails.getGroupId();
            }
        } else if (isTrashRequested(click) && id<1) {
            GroupDetails trashGroupDetails = GroupsService.getTrashGroupDetails();
            if (trashGroupDetails != null) id = trashGroupDetails.getGroupId();
        }

        List<JsTreeItem> items = new ArrayList<>();
        GroupDetails group = groupsDB.getGroup(id);
        if (group == null && id != 0) return items;
        final int groupDefaultDocId = (group != null) ? group.getDefaultDocId() : -1;

        boolean parentEditable = GroupsDB.isGroupEditable(user, id);

        List<GroupDetails> groups = getGroups(id, treeSearchValue, request);

        //Special case -> if we want tree items for STAT section AND user have cmp_stat_seeallgroups right, we do not filter by perms but RETURN ALL ITEMS
        String referer = request.getHeader("referer");
        String uri = request.getRequestURI();
        final boolean checkGroupsPerms;
        //Referer -> that we call from stat section
        //Uri -> that it's called from GroupTreeRestController NOT from WebPagesListener
        if(referer!=null && referer.contains("/apps/stat/admin/") && uri != null && uri.contains("/admin/rest/groups/tree/tree") && user.isEnabledItem("cmp_stat_seeallgroups")) {
            checkGroupsPerms = false;
        } else {
            checkGroupsPerms = true;
            groups = filterByPerms(groups, user); //Filter groups by perms
        }

        if (click.contains("alldomains")==false || requestedDomain!=null) groups = filterByDomain(groups, requestedDomain);

        GroupDetails domainRootGroup = null;
        String domainFilesPrefix = "";
        if (Constants.getBoolean("enableStaticFilesExternalDir"))
        {
            int domainId = CloudToolsForCore.getDomainId();
            domainRootGroup = GroupsDB.getInstance().getGroup(domainId);
            domainFilesPrefix =  "/" + domainRootGroup.getGroupName();
        }

        if (click.contains("filter-system-trash")) {
            groups = filterSystem(groups);
            //filter Full text Index folder /files
            groups = filterFullPath(groups, "/files");

            if (Constants.getBoolean("enableStaticFilesExternalDir") && domainRootGroup!=null)
            {
                ///files adresar vytvarame v domenovom foldri
                groups = filterFullPath(groups, domainFilesPrefix + "/files");
            }
        }

        List<JsTreeItem> filteredFilesGroupsTree = new ArrayList<>();
        if (isSystemRequested(click)) {
            if (group!=null && (group.getFullPath().startsWith("/files") || group.getFullPath().startsWith(domainFilesPrefix+"/files"))) {
                //if it's /files/something OR /Jet Portal 4/files/something then do not filter and show all groups by requested parent folder
            } else {
                if (Constants.getBoolean("templatesUseRecursiveSystemFolder")) {
                    groups = filterOnlyWithSystemChilds(groups, groupsDB);
                } else {
                    //musis odfiltrovat Kos priecinok
                    groups = filterTrash(groups);
                }

                //add /files folder to root (it's shown in /System folder)
                if (idOriginal==0) {
                    List<GroupDetails> filesGroups = filterFullPath(groupsDB.getGroups(0), "!/files");
                    if (domainRootGroup != null && Tools.isNotEmpty(domainFilesPrefix)) {
                        filesGroups.addAll( filterFullPath(groupsDB.getGroups(domainRootGroup.getGroupId()), "!"+domainFilesPrefix+"/files") );
                    }

                    if(Tools.isNotEmpty(treeSearchValue) == true) {
                        filteredFilesGroupsTree = getFilteredGroupsTree(filesGroups, treeSearchValue, user, showPages, request);
                        // This file groups is fully prepared tree with parents -> add at end
                    } else {
                        groups.addAll(filesGroups);
                    }
                }
            }
        }

        items.addAll( sortTreeBasedOnUserSettings(user, groups, showPages, checkGroupsPerms) );

        if(Tools.isEmpty(treeSearchValue)) {
            //standardne zobrazenie v stromovej strukture, rovno zobraz aj pod adresare
            int groupId = WebPagesListener.getLastGroupId(request, 0);
            if ((click.equals("dt-tree-group-filter-system-trash") || click.equals("dt-tree-filter-system-trash")) && id==0 && groupId<1 && request.getParameter("docid")==null) {
                List<JsTreeItem> rootItems = new ArrayList<>();
                rootItems.addAll(items);
                //oznac prvy adresar ako selectnuty
                if (rootItems.isEmpty()==false) {
                    rootItems.get(0).getState().setSelected(true);
                }
                //pridaj child elementy
                if (rootItems.size() <= Constants.getInt("webpagesTreeAutoOpenLimit", 2)) {
                    //ak je tam menej ako 3 grupy, tak nacitaj rovno aj subgrupy
                    for (JsTreeItem item : rootItems) {
                        if (item.getState()==null) item.setState(new JsTreeItemState());
                        item.getState().setOpened(true);
                        //toto je totalna haluz, pri rozbalenie a refreshi toto nemoze byt nastavene, inak to padne
                        if (request.getParameter("click")!=null) item.setChildren(null);

                        item.setParent("#");
                        List<JsTreeItem> subGroups = this.getItems(user, Tools.getIntValue(item.getId(), -1), showPages, click, requestedDomain, request);
                        for (JsTreeItem sub : subGroups) {
                            sub.setParent(item.getId());
                        }
                        items.addAll(subGroups);
                    }
                }
            }
        } else {
            //
            List<JsTreeItem> filteredListWithParents = addParents( items, user, showPages, checkGroupsPerms, id );
            // Add prepared file groups
            filteredListWithParents.addAll(filteredFilesGroupsTree);

            items.clear();
            items.addAll(filteredListWithParents);
        }

        if (parentEditable && showPages && id>0)
        {
            List<DocDetails> childDocs = docDB.getBasicDocDetailsByGroup(id, DocDB.ORDER_PRIORITY);
            if (childDocs != null && childDocs.size() > 0) {
                items.addAll(childDocs.stream().map(doc -> {
                    DocumentsJsTreeItem jstree = new DocumentsJsTreeItem(doc, groupDefaultDocId);
                    if (click.contains("alldomains")) {
                        jstree.setVirtualPath(addDomainPrefixToFullPath(doc, groupsDB));
                    }
                    return jstree;
                }).collect(Collectors.toList()));
            }
        }

        if (id == Constants.getInt("systemPagesNotApprovedDocs")) {
            List<DocDetails> notApproved = docDB.getNotApprovedDocs(user.getUserId());
            if (notApproved != null && notApproved.size() > 0) {
                items.addAll(notApproved.stream().map(doc -> new DocumentsJsTreeItem(doc, groupDefaultDocId)).collect(Collectors.toList()));
            }
        }

        if (id == Constants.getInt("systemPagesDocsToApprove")) {
            List<DocDetails> approve = docDB.getDocsForApprove(user.getUserId());
            if (approve != null && approve.size() > 0) {
                items.addAll(approve.stream().map(doc -> new DocumentsJsTreeItem(doc, groupDefaultDocId)).collect(Collectors.toList()));
            }
        }

        return items;
    }

    private List<JsTreeItem> sortTreeBasedOnUserSettings(Identity user, List<GroupDetails> groups, boolean showPages, boolean checkGroupsPerms) {
        String sortType = WebpagesService.getTreeSortType(user);
        boolean orderAsc = WebpagesService.isTreeSortOrderAsc(user);

        Comparator<GroupDetails> comparator;
        if("title".equals(sortType)) {
            // ignore case, sort that way is better
            comparator = Comparator.comparing(GroupDetails::getGroupName, String.CASE_INSENSITIVE_ORDER);
        } else if("createDate".equals(sortType)) {
            comparator = Comparator.comparing(GroupDetails::getGroupId);
        } else {
            //DEFAULT OPTION -> sort by "priority"
            comparator = Comparator.comparing(GroupDetails::getSortPriority);
        }

        if(!orderAsc) {
            comparator = comparator.reversed();
        }

        return groups.stream()
            .sorted(comparator)
            .map(g -> new GroupsJsTreeItem(g, user, showPages, checkGroupsPerms))
            .collect(Collectors.toList());
    }

    public void fixSortPriority(HttpServletRequest request, int docId, GroupDetails parent, int position) {
        DocDB docDB = DocDB.getInstance();

        //we need to subtrack groups from position, because we have groups in the list
        List<GroupDetails> groups = GroupsDB.getInstance().getGroups(parent.getGroupId());
        position -= groups.size();

        DocDetails doc = docDB.getDoc(docId);
        List<DocDetails> docsByGroup = docDB.getDocByGroup(parent.getGroupId(), DocDB.ORDER_PRIORITY, true, -1, -1, false, false);
        List<DocDetails> collect = docsByGroup.stream().filter(d -> d.getDocId() != doc.getDocId()).sorted(Comparator.comparing(DocDetails::getSortPriority)).collect(Collectors.toList());

        int sortPriority = collect.size() > 0 ? collect.get(0).getSortPriority() : parent.getSortPriority() * 10;
        collect.add(position, doc);

        int sortPriorityIncrementDoc = Constants.getInt("sortPriorityIncrementDoc");
        for (DocDetails document : collect) {
            document.setSortPriority(sortPriority);
            DocDB.saveDoc(document);
            sortPriority += sortPriorityIncrementDoc;
        }

        EditorDB.cleanSessionData(request);
    }

    private static List<GroupDetails> filterByPerms(List<GroupDetails> groups, Identity user) {
        List<GroupDetails> filtered = groups.stream().filter(g->{
            boolean editable = GroupsDB.isGroupEditable(user, g.getGroupId());
            boolean viewable = GroupsDB.isGroupViewable(user, g.getGroupId());

            if (g.isHiddenInAdmin() && user.isDisabledItem("editor_show_hidden_folders")) {
                return false;
            }

            return viewable || editable;
        }).collect(Collectors.toList());

        return filtered;
    }

    private static List<GroupDetails> filterByDomain(List<GroupDetails> groups, String requestedDomain) {
        if (Constants.getBoolean("multiDomainEnabled")==false) return groups;

        String currentDomain = requestedDomain!=null ? requestedDomain : CloudToolsForCore.getDomainName();
        List<GroupDetails> filtered = groups.stream().filter(g->{

            if ("System".equals(g.getGroupName()) && Tools.isEmpty(g.getDomainName()) && Constants.getBoolean("templatesUseDomainLocalSystemFolder")) {
                //odignorujme globalny system adresar, ak ma existovat lokalny
                return false;
            }

            if (Tools.isEmpty(g.getDomainName()) || g.getDomainName().equals(currentDomain)) return true;

            return false;
        }).collect(Collectors.toList());

        return filtered;
    }

    /**
     * Z adresarov odfiltruje tie obsahujuce /System v ceste
     * @param groups
     * @return
     */
    private static List<GroupDetails> filterSystem(List<GroupDetails> groups) {
        List<GroupDetails> filteredByPath = filterFullPath(groups, "/System");

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null)
		{
			return filteredByPath;
		}

        //v starych WJ mame Syste priecinok ako podpriecinok hlavneho priecinku, musime odfiltrovat takto
		String domain = rb.getDomain();
		if (Tools.isEmpty(domain)) return filteredByPath;
		int domainId = GroupsDB.getDomainId(domain);

        List<GroupDetails> filtered = filteredByPath.stream().filter(g->{
            if ("System".equals(g.getGroupName()) && g.getParentGroupId()==domainId) return false;
            return true;
        }).collect(Collectors.toList());

        return filtered;
    }

    /**
     * odfiltruje priecinok /System/Kos so zoznamu
     * @param groups
     * @return
     */
    public static List<GroupDetails> filterTrash(List<GroupDetails> groups) {
        String trashDirPath = getTrashDirPath();

        return filterFullPath(groups, "*"+trashDirPath);
    }

    public static String getTrashDirPath() {
        Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        String trashDirPath = propSystem.getText("config.trash_dir");
        return trashDirPath;
    }

    /**
     * Odfiltruje adresare zacinajuce na zadanu cestu
     * @param groups
     * @param filterFullPath
     * @return
     */
    private static List<GroupDetails> filterFullPath(List<GroupDetails> groups, String filterFullPath) {
        List<GroupDetails> filtered = groups.stream().filter(g->{
            if (filterFullPath.startsWith("!") && g.getFullPath().startsWith(filterFullPath.substring(1))==false) return false;
            if ( g.getFullPath().startsWith(filterFullPath) ) return false;
            if (filterFullPath.startsWith("*") && filterFullPath.length()>2 && g.getFullPath().contains(filterFullPath.substring(1)) ) return false;
            return true;
        }).collect(Collectors.toList());

        return filtered;
    }

    /**
     * Z adresarov PONECHA len tie, ktore ako child maju System adresar
     * @param groups
     * @return
     */
    private static List<GroupDetails> filterOnlyWithSystemChilds(List<GroupDetails> groups, GroupsDB groupsDB) {
        List<GroupDetails> allSystemFolders = groupsDB.getAllSystemFolders(false);

        List<GroupDetails> filtered = groups.stream().filter(g->{
            String fullPath = g.getFullPath();
            for (GroupDetails system : allSystemFolders) {
                //Logger.debug(GroupsTreeService.class, "Comparing system "+system.getFullPath()+" vs group "+fullPath);
                //smerom dole
                if (system.getFullPath().startsWith(fullPath)) return true;
                //smerom na childov
                if (fullPath.startsWith(system.getFullPath())) return true;
            }
            return false;
        }).collect(Collectors.toList());

        return filtered;
    }

    /**
     * Vrati true, ak je pozadovane zobrazenie system priecinka
     * @param click
     * @return
     */
    private static boolean isSystemRequested(String click) {
        return ("dt-tree-group-system".equals(click) || "dt-tree-system".equals(click));
    }

    /**
     * Vrati true, ak je pozadovane zobrazenie kosa
     * @param click
     * @return
     */
    private static boolean isTrashRequested(String click) {
        return ("dt-tree-group-trash".equals(click) || "dt-tree-trash".equals(click));
    }

    /**
     * Returns group.fullPath for DocDetails with domain prefix in multi domain enviroment
     * @param tmp - DocDetails object
     * @param groupsDB
     * @return
     */
    public static String addDomainPrefixToFullPath(DocDetails tmp, GroupsDB groupsDB) {
        GroupDetails grp = groupsDB.getGroup(tmp.getGroupId());
        StringBuilder path = new StringBuilder();
        if (grp != null) {
            if (Tools.isNotEmpty(grp.getDomainName()) && Constants.getBoolean("multiDomainEnabled")) path.append(grp.getDomainName()).append(":");
            path.append(grp.getFullPath());
        }
        path.append("/");
        path.append(tmp.getTitle());
        return path.toString();
    }

    /**
     * Returns GrooupDetails object for groupId, BUT verify permissions for user.
     * It it's not accessible, return first accessible group for user.
     * @param groupId
     * @param user
     * @return
     */
    public static GroupDetails gerDefaultGroupTreeOptionForUser(int groupId, Identity user) {
        GroupsDB groupsDB = GroupsDB.getInstance();

        //User can edit all groups -> so return group (no check needed)
        //OR user have right cmp_stat_seeallgroups (in stat section ONLY)
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        String referer = null;
        if (rb != null) {
            referer = rb.getReferrer();
        }
        if( Tools.isEmpty(user.getEditableGroups(true)) || (referer != null && referer.contains("/apps/stat/admin/") && user.isEnabledItem("cmp_stat_seeallgroups"))) {
            if(groupId > 0) return groupsDB.findGroup(groupId);

            GroupDetails rootGroup = new GroupDetails();
            rootGroup.setGroupId(-1);
            return rootGroup;
        }

        //Can handle default group ?
        boolean parentEditable = GroupsDB.isGroupEditable(user, groupId);
        boolean parentViewable = GroupsDB.isGroupViewable(user, groupId);

        //Check if user have right for this group
        //It cant be -1 (root group), because there is group restriction for you part of tree
        if( (parentEditable || parentViewable) && groupId != -1) {
            //User have right for this group)
           return groupsDB.findGroup(groupId);
        } else {
            //Problem, user missing rights for this group ... return first permitted group
            int[] permittedGroups = Tools.getTokensInt(user.getEditableGroups(true), ",");

            //Use first groupId
            return groupsDB.findGroup( permittedGroups[0] );
        }
    }

    /**
     * Filter list of groups by searchText value
     * @param groups
     * @param searchText
     * @param request
     * @return
     */
    private List<GroupDetails> filterGroups(List<GroupDetails> groups, String searchText, HttpServletRequest request) {
        String treeSearchType = Tools.getStringValue(request.getParameter("treeSearchType"), "");
        final String wantedValueLC = DB.internationalToEnglish(searchText).toLowerCase();

        //Filter by serach value and search type
        if("contains".equals(treeSearchType)) {
            return groups.stream()
                .filter(group -> DB.internationalToEnglish(group.getGroupName()).toLowerCase().contains(wantedValueLC))
                .collect(Collectors.toList());
        } else if("startwith".equals(treeSearchType)) {
            return groups.stream()
                .filter(group -> DB.internationalToEnglish(group.getGroupName()).toLowerCase().startsWith(wantedValueLC))
                .collect(Collectors.toList());
        } else if("endwith".equals(treeSearchType)) {
            return groups.stream()
                .filter(group -> DB.internationalToEnglish(group.getGroupName()).toLowerCase().endsWith(wantedValueLC))
                .collect(Collectors.toList());
        } else if("equals".equals(treeSearchType)) {
            return groups.stream()
                .filter(group -> DB.internationalToEnglish(group.getGroupName()).equalsIgnoreCase(wantedValueLC))
                .collect(Collectors.toList());
        } else return new ArrayList<>();
    }

    /**
     * Get groups list by root ID and filter by searchText
     * @param rootGroupId
     * @param searchText
     * @param request
     * @return
     */
    private List<GroupDetails> getGroups(int rootGroupId, String searchText, HttpServletRequest request) {
        GroupsDB groupsDB = GroupsDB.getInstance();

        if (Tools.isEmpty(searchText)) return groupsDB.getGroups(rootGroupId);

        //Get all suitable
        List<GroupDetails> result;
        if(rootGroupId < 1) {
            result = groupsDB.getGroupsAll();
        } else {
            result = new ArrayList<>();
            groupsDB.getGroupsTree(rootGroupId, result);
        }

        return filterGroups(result, searchText, request);
    }

    /**
     * Filter file groups by searchValue
     * @param fileGroups
     * @param searchValue
     * @param user
     * @param showPages
     * @param request
     * @return
     */
    private List<JsTreeItem> getFilteredGroupsTree(List<GroupDetails> fileGroups, String searchValue, Identity user, boolean showPages, HttpServletRequest request) {
        GroupsDB groupsDB = GroupsDB.getInstance();

        // From fileGroups get all groups expanded -> direction down (children)
        Map<Integer, GroupDetails> allGroupsExpanded = new HashMap<>();
        fileGroups.forEach(g -> allGroupsExpanded.put(g.getGroupId(), g));
        for(GroupDetails fileGroup : fileGroups) {
            List<GroupDetails> childs = new ArrayList<>();
            groupsDB.getGroupsTree(fileGroup.getGroupId(), childs);
            for(GroupDetails child : childs) {
                allGroupsExpanded.putIfAbsent(child.getGroupId(), child);
            }
        }

        // Filter all groups by wanted value
        List<GroupDetails> filtered = filterGroups(new ArrayList<>(allGroupsExpanded.values()), searchValue, request);

        //Transform to JsTreeItem
        List<JsTreeItem> items = new ArrayList<>();
        items.addAll(
            filtered.stream()
            .map(g -> new GroupsJsTreeItem(g, user, showPages))
            .collect(Collectors.toList())
        );

        // Add to filtered groups their parents -> but only to height of original groups
        Map<Integer, JsTreeItem> filteredWithParents = new HashMap<>();
        for(JsTreeItem item : items) {
            int idToAdd = Integer.parseInt(item.getId());
            while(true) {
                GroupDetails g = allGroupsExpanded.get(idToAdd);
                if(g != null && filteredWithParents.containsKey(g.getGroupId()) == false) {
                    filteredWithParents.putIfAbsent(g.getGroupId(), new GroupsJsTreeItem(g, user, showPages));
                    idToAdd = g.getParentGroupId();
                } else {
                    break;
                }
            }
        }

        // Set statuses
        for (Map.Entry<Integer, JsTreeItem> entry : filteredWithParents.entrySet()) {
            entry.getValue().setState(getLoadedState());
            Integer parentId = allGroupsExpanded.get( entry.getKey() ).getParentGroupId();
            entry.getValue().setParent( filteredWithParents.containsKey(parentId) ? parentId.toString() : "#" );
        }

        // Return filtered groups with their parents
        return new ArrayList<>(filteredWithParents.values());
    }

    /**
     * Add parent groups for foundGroups to correctly build tree structure
     * @param filteredGroups
     * @param user
     * @param showPages
     * @param checkGroupsPerms
     * @param id
     * @return
     */
    private List<JsTreeItem> addParents(List<JsTreeItem> filteredGroups, Identity user, boolean showPages, boolean checkGroupsPerms, int id) {
        GroupsDB groupsDB = GroupsDB.getInstance();
        Map<Integer, GroupDetails> kk = new HashMap<>();

        //Get whole trees
        for(JsTreeItem group : filteredGroups) {
            List<GroupDetails> parents = groupsDB.getParentGroups(Integer.parseInt(group.getId()), false);
            for(GroupDetails parent : parents) {
                kk.putIfAbsent(parent.getGroupId(), parent);
            }
        }

        // Remove groups that are higher level that we want (including parent group because its System or trash and we dont want to show them)
        if(id > 0) {
            List<GroupDetails> groupsToRemove = groupsDB.getParentGroups(id);
            for(GroupDetails group : groupsToRemove) {
                kk.remove(group.getGroupId());
            }
        }

        //Transform to JsTreeItem
        List<JsTreeItem> items = new ArrayList<>();
        items.addAll(
            kk.values().stream()
            .map(g -> new GroupsJsTreeItem(g, user, showPages, checkGroupsPerms))
            .collect(Collectors.toList())
        );

        //Set them states
        for(JsTreeItem item : items) {
            item.setState(getLoadedState());

            int parentId = kk.get( Integer.parseInt(item.getId()) ).getParentGroupId();
            item.setParent( (parentId == 0 || parentId == id)  ? "#" : String.valueOf(parentId) );
        }

        return items;
    }

    /**
     * Returns JsTreeItemState with loaded state
     * @return
     */
    private JsTreeItemState getLoadedState() {
        JsTreeItemState state =  new JsTreeItemState();
        state.setOpened(false);
        state.setLoaded(true);
        return state;
    }
}
