package sk.iway.iwcm.doc;

import org.springframework.stereotype.Service;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.rest.WebPagesListener;
import sk.iway.iwcm.i18n.Prop;

import javax.servlet.http.HttpServletRequest;
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

        if (isSystemRequested(click) && id<1) {
            if (Constants.getBoolean("templatesUseRecursiveSystemFolder")==false) {
                GroupDetails systemGroupDetails = getSystemGroupDetails();
                if (systemGroupDetails != null) id = systemGroupDetails.getGroupId();
            }
        } else if (isTrashRequested(click) && id<1) {
            GroupDetails trashGroupDetails = getTrashGroupDetails();
            if (trashGroupDetails != null) id = trashGroupDetails.getGroupId();
        }

        List<JsTreeItem> items = new ArrayList<>();
        GroupDetails group = groupsDB.getGroup(id);
        if (group == null && id != 0) return items;
        final int groupDefaultDocId = (group != null) ? group.getDefaultDocId() : -1;

        boolean parentEditable = GroupsDB.isGroupEditable(user, id);

        List<GroupDetails> groups = groupsDB.getGroups(id);

        groups = filterByPerms(groups, user);

        if (click.contains("alldomains")==false || requestedDomain!=null) groups = filterByDomain(groups, requestedDomain);

        if (click.contains("filter-system-trash")) {
            groups = filterSystem(groups);
        }
        if (isSystemRequested(click) && Constants.getBoolean("templatesUseRecursiveSystemFolder")) {
            //musis odfiltrovat a vratit iba adresare, ktore maju potomka s nazvom System
            groups = filterOnlyWithSystemChilds(groups, groupsDB);
        }
        if (isSystemRequested(click)) {
            //musis odfiltrovat Kos priecinok
            groups = filterTrash(groups);
        }

        items.addAll(groups.stream().sorted(Comparator.comparing(GroupDetails::getSortPriority)).map(g -> new GroupsJsTreeItem(g, user, showPages)).collect(Collectors.toList()));

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

        if (id == Constants.getInt("systemPagesMyPages")) {
            List<DocDetails> myPages = DocDB.getMyPages(user);
            if (myPages.size() > 0) {
                items.addAll(myPages.stream().map(doc -> new DocumentsJsTreeItem(doc, groupDefaultDocId)).collect(Collectors.toList()));
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

    public void fixSortPriority(HttpServletRequest request, int docId, GroupDetails parent, int position) {
        DocDB docDB = DocDB.getInstance();

        DocDetails doc = docDB.getDoc(docId);
        List<DocDetails> docsByGroup = docDB.getDocByGroup(parent.getGroupId());
        List<DocDetails> collect = docsByGroup.stream().filter(d -> d.getDocId() != doc.getDocId()).sorted(Comparator.comparing(DocDetails::getSortPriority)).collect(Collectors.toList());

        int sortPriority = collect.size() > 0 ? collect.get(0).getSortPriority() : parent.getSortPriority() * 10;
        collect.add(position, doc);

        for (DocDetails document : collect) {
            document.setSortPriority(sortPriority);
            DocDB.saveDoc(document);
            sortPriority += 10;
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
     * Vrati adresar Kos
     * @return
     */
    public GroupDetails getTrashGroupDetails() {
        Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        String trashDirName = propSystem.getText("config.trash_dir");
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails trashGroupDetails = groupsDB.getCreateGroup(trashDirName);
        return trashGroupDetails;
    }

    /**
     * Vrati adresar System (lokalny)
     * @return
     */
    public GroupDetails getSystemGroupDetails() {
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails system = groupsDB.getLocalSystemGroup();

        //ak sa nenaslo, pouzi globalny
        if (system == null) {
            system = groupsDB.getGroupByPath("/System");
        }

        return system;
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
}
