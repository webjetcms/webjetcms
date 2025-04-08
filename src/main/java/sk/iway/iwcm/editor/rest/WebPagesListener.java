package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.*;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.DocumentsJsTreeItem;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsJsTreeItem;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UsersDB;

/**
 * Doplna data pre admin cast web stranky (pre prve nacitanie),
 * aby nebolo potrebne volat REST sluzby pri inicializacii
 * - optimalizacia rychlosti zobrazenia
 */
@Component
public class WebPagesListener {

    @Autowired
    private GroupsTreeService groupsTreeService;

    @Autowired
    private DocDetailsRepository docRepo;

    @Autowired
    private DocHistoryRepository docHistoryRepository;

    @Autowired
    private DocAtrDefRepository docAtrDefRepository;

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='web-pages-list'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();
            HttpServletRequest request = event.getSource().getRequest();
            Identity user = UsersDB.getCurrentUser(request);

            String treeInitialJson = "null";
            String groupsInitialJson = "null";
            String webpagesInitialJson = "null";
            Boolean hasPagesToApprove = Boolean.FALSE;
            boolean hasSystemTab = true;
            boolean hasTrashTab = true;

            GroupsDB groupsDB = GroupsDB.getInstance();

            GroupDetails system = GroupsService.getSystemGroupDetails();
            GroupDetails trash = GroupsService.getTrashGroupDetails();

            String showTab = "pills-folders-tab";

            if (user.isEnabledItem("menuWebpages")) {

                //ak je zadany docid ako parameter over/nastav korektne domenu
                int docId = Tools.getIntValue(event.getSource().getRequest().getParameter("docid"), -1);
                int groupId = Tools.getIntValue(event.getSource().getRequest().getParameter("groupid"), -1);

                if (groupId < 1 && Integer.toString(Constants.getInt("systemPagesMyPages")).equals(user.getEditableGroups()) && Tools.isNotEmpty(user.getEditablePages())) {
                    //use first group
                    List<DocDetails> myPages = DocDB.getMyPages(user);
                    if (myPages.size() > 0) {
                        groupId = myPages.get(0).getGroupId();
                        request.getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(groupId));
                    }
                }

                if (docId > 0 && EditorDB.isPageEditable(user, docId)) {
                    //DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
                    Optional<DocDetails> doc = docRepo.findById(Long.valueOf(docId));
                    if (doc.isPresent()) {
                        //ak je zadane docId prepiseme groupId aj keby bolo zadane
                        groupId = doc.get().getGroupId();
                    }
                }
                if (groupId > 0) {
                    GroupDetails group = groupsDB.getGroup(groupId);
                    if (group != null && Tools.isNotEmpty(group.getDomainName())) {
                        event.getSource().getRequest().getSession().setAttribute("preview.editorDomainName", group.getDomainName());
                        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
                        if (rb != null) {
                            rb.setDomain(DocDB.getDomain(event.getSource().getRequest()));
                        }
                    }
                }

                //over nastavenie zobrazenia
                String click = "dt-tree-group-filter-system-trash";
                boolean showPages = WebpagesService.isTreeShowPages(user);
                if (showPages) click = "dt-tree-filter-system-trash";

                //root adresare stromovej struktury
                int lastGroupId = getLastGroupId(request, groupId);
                GroupDetails lastGroup = groupsDB.getGroup(lastGroupId);

                int rootGroupId = 0;
                if (lastGroup != null && lastGroup.getFullPath().contains("/System")) {
                    if (lastGroup.getFullPath().contains(trash.getFullPath())) {
                        //it's a trash folder
                        rootGroupId = trash.getGroupId();
                        showTab = "pills-trash-tab";
                        click = "dt-tree-group-trash";
                    } else {
                        rootGroupId = system.getGroupId();
                        showTab = "pills-system-tab";
                        click = "dt-tree-group-system";
                    }
                }

                List<JsTreeItem> rootGroups = groupsTreeService.getItems(user, rootGroupId, showPages, click, null, event.getSource().getRequest());

                if ("pills-system-tab".equals(showTab)) {
                    rootGroups = rootGroups.stream().filter(g->{
                        if (g instanceof GroupsJsTreeItem) {
                            GroupsJsTreeItem gjs = (GroupsJsTreeItem) g;
                            if (gjs.getGroup().getFullPath().startsWith(trash.getFullPath())) return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
                }

                List<JsTreeItem> expandedGroups = expandGroupsLastFolder(rootGroups, lastGroupId, user, showPages, click, request);
                treeInitialJson = JsonTools.objectToJSON(expandedGroups);

                //root group details
                DatatablePageImpl<GroupDetails> rootGroupDetails = GroupsRestController.getAllItems(lastGroupId, event.getSource().getRequest());

                if ("null".equals(groupsInitialJson) && rootGroups.isEmpty()==false && (rootGroupDetails==null || rootGroupDetails.getContent().isEmpty())) {
                    //use first group as rootGroupDetails
                    List<GroupDetails> onlyRootGroupList = new ArrayList<>();
                    if (lastGroup != null) onlyRootGroupList.add(lastGroup); //user doesnt' have edit rights to group, so it's null
                    else if (rootGroups.get(0) instanceof GroupsJsTreeItem) onlyRootGroupList.add(((GroupsJsTreeItem)rootGroups.get(0)).getGroup());

                    rootGroupDetails = new DatatablePageImpl<>(onlyRootGroupList);
                    if (onlyRootGroupList.isEmpty()==false) {
                        WebpagesService ws = new WebpagesService(onlyRootGroupList.get(0).getGroupId(), user, Prop.getInstance(request), request);
                        GroupsRestController.addOptions(rootGroupDetails, ws, user, onlyRootGroupList.get(0), request);
                    }
                }

                groupsInitialJson = JsonTools.objectToJSON(rootGroupDetails);

                GetAllItemsDocOptions options = new GetAllItemsDocOptions(event.getSource().getRequest());
                //Set group id of first group in jstree (pre-loading)
                if (rootGroupDetails!=null && rootGroupDetails.getContent().isEmpty()==false) options.setGroupId(rootGroupDetails.getContent().get(0).getGroupId());
                else options.setGroupId(0);
                //Set pageable as new instance from page 0 to page 25
                options.setPageable(PageRequest.of(0, 25).withSort(Sort.by(Sort.Direction.ASC, "id")));
                options.setDocDetailsRepository(docRepo);
                options.setDocAtrDefRepository(docAtrDefRepository);

                //Get all Doc Pages
                DatatablePageImpl<DocDetails> pages = WebpagesService.getAllItems(options);
                if (pages.getContent().isEmpty()==false) {
                    webpagesInitialJson = JsonTools.objectToJSON( pages );
                }

                //overenie, ci ma nieco na schvalenie
                //List<DocHistory> pagesForApprove = WebApproveService.getAllItems(options, null);
                try {
                    List<DocHistory> pagesForApprove = docHistoryRepository.findAll(WebApproveRestController.getToApproveConditions(options.getUserId()));
                    if (pagesForApprove.isEmpty()==false) hasPagesToApprove = Boolean.TRUE;
                } catch (Exception ex) {
                    Logger.error(ex);
                }

                if (lastGroupId == 0) request.getSession().removeAttribute(Constants.SESSION_GROUP_ID);
            }

            model.addAttribute("treeInitialJson", fixJson(treeInitialJson));
            model.addAttribute("groupsInitialJson", fixJson(groupsInitialJson));
            model.addAttribute("webpagesInitialJson", fixJson(webpagesInitialJson));
            model.addAttribute("hasPagesToApprove", hasPagesToApprove);
            model.addAttribute("showTab", showTab);

            //ak ma user specialne prava na priecinky automaticky system a kos vypni,
            //povol len ak ma pravo na niektory priecinok
            if (Tools.isNotEmpty(user.getEditableGroups()) || Tools.isNotEmpty(user.getEditablePages())) {
                hasSystemTab = false;
                hasTrashTab = false;
            }

            if (Tools.isNotEmpty(user.getEditableGroups())) {
                if (GroupsDB.isGroupEditable(user, system.getGroupId()) || GroupsDB.isGroupViewable(user, system.getGroupId())) hasSystemTab = true;
                if (GroupsDB.isGroupEditable(user, trash.getGroupId()) || GroupsDB.isGroupViewable(user, trash.getGroupId())) hasTrashTab = true;
            }
            if (Tools.isNotEmpty(user.getEditablePages())) {
                    //prejdi zoznam web stranok a over, ci tam nie je System alebo Kos
                    List<DocDetails> docs = UserTools.getEditablePages(user.getEditablePages());
            		for (DocDetails doc : docs) {
                        if (hasSystemTab==false && (","+groupsDB.getParents(doc.getGroupId())+",").contains(","+system.getGroupId()+",")) hasSystemTab = true;
                        if (hasTrashTab==false && (","+groupsDB.getParents(doc.getGroupId())+",").contains(","+trash.getGroupId()+",")) hasTrashTab = true;
                    }
            }

            model.addAttribute("hasSystemTab", Boolean.valueOf(hasSystemTab));
            model.addAttribute("hasTrashTab", Boolean.valueOf(hasTrashTab));


            request.getSession().setAttribute("userWasInWebpages", "true");
         } catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }

    }

    /**
     * Fixne problem s JSON objektom vlozenym do HTML kodu, problem je hlavne so </script> tagom, ktory na urovni HTML parsera ukonci parsing javascript kodu a hodi chybu
     * @param json
     * @return
     */
    private String fixJson(String json) {
        String fixed = json;
        fixed = Tools.replace(fixed, "</script>", "<\\/script>");
        fixed = Tools.replace(fixed, "</SCRIPT>", "<\\/SCRIPT>");
        return fixed;
    }

    /**
     * Expandne priecinky podla zadaneho naposledy otvoreneho groupId
     * @param rootGroups
     * @param lastGroupId
     * @return
     */
    private List<JsTreeItem> expandGroupsLastFolder(List<JsTreeItem> rootGroups, int lastGroupId, Identity user, boolean showPages, String click, HttpServletRequest request) {
        if (lastGroupId < 1) return rootGroups;

        List<JsTreeItem> expanded = new ArrayList<>();
        expanded.addAll(rootGroups);

        //ziskaj zoznam parent priecinkov
        List<GroupDetails> parentGroups = GroupsDB.getInstance().getParentGroups(lastGroupId);
        Collections.reverse(parentGroups);
        expandGroups(expanded, parentGroups, lastGroupId, 0, user, showPages, click, request);

        //vyresetuj selected atribut
        for (JsTreeItem item : expanded) {
            if (item.getId().equals(String.valueOf(lastGroupId))) item.getState().setSelected(true);
            else item.getState().setSelected(false);
        }

        return expanded;
    }

    private void expandGroups(List<JsTreeItem> groups, List<GroupDetails> parentGroups, int lastGroupId, int position, Identity user, boolean showPages, String click, HttpServletRequest request) {

        if (position>=parentGroups.size()) return;
        int addGroupId = parentGroups.get(position).getGroupId();

        //prevencia ConcurrentModificationException
        List<JsTreeItem> groupsOriginal = new ArrayList<>();
        groupsOriginal.addAll(groups);

        for (JsTreeItem item : groupsOriginal) {
            if (item instanceof GroupsJsTreeItem) {
                GroupsJsTreeItem gitem = (GroupsJsTreeItem)item;

                if (position == 0 && Tools.isEmpty(gitem.getParent())) {
                     gitem.setParent("#");
                }

                if (gitem.getGroup().getGroupId()==addGroupId) {
                    //nastav stav na neselektnuty a otvoreny
                    gitem.getState().setSelected(false);
                    gitem.getState().setOpened(true);
                    gitem.getState().setLoaded(true);

                    //pridaj child elementy
                    List<JsTreeItem> childs = groupsTreeService.getItems(user, addGroupId, showPages, click, null, request);
                    for (JsTreeItem child : childs) {
                        child.setParent(String.valueOf(addGroupId));
                        //ak je to posledny nastav stav na selectnuty
                        if (child.getId().equals(String.valueOf(lastGroupId))) child.getState().setSelected(true);
                    }
                    groups.addAll(childs);

                    //vyvolaj rekurziu
                    expandGroups(groups, parentGroups, lastGroupId, position+1, user, showPages, click, request);
                }
            }
            if (item instanceof DocumentsJsTreeItem) {
                DocumentsJsTreeItem ditem = (DocumentsJsTreeItem)item;
                if (position == 0 && Tools.isEmpty(ditem.getParent())) {
                     ditem.setParent("#");
                }
            }
        }
    }

    /**
     * Ziska naposledy zobrazene ID priecinka, alebo 0 ak nie je nastavene
     * @param request
     * @param groupId - groupId based on docId value or 0
     * @return
     */
    public static int getLastGroupId(HttpServletRequest request, int groupId) {
        int lastGroupId = 0;
        try {
            int docId = Tools.getIntValue(request.getParameter("docid"), -1);
            if (docId > 0) {
                //ak je zadane docid nerobime ziaden preload
                lastGroupId = groupId;
            } else if (request.getParameter("groupid") != null) {
                lastGroupId = Integer.parseInt(request.getParameter("groupid"));
            } else {
                // skus ziskat data zo session
                if (request.getSession().getAttribute(Constants.SESSION_GROUP_ID) != null) {
                    lastGroupId = Integer
                            .parseInt((String) request.getSession().getAttribute(Constants.SESSION_GROUP_ID));
                }

            }
        } catch (Exception ex) {
            //
        }

        //ak je zadana hodnota 0 zmaz zo session zapamatanie
        if (lastGroupId==0) request.getSession().removeAttribute(Constants.SESSION_GROUP_ID);

        if (lastGroupId>0) {
            GroupDetails group = GroupsDB.getInstance().getGroup(lastGroupId);
            if (group != null && Tools.isNotEmpty(group.getDomainName())) {
                String currentDomain = CloudToolsForCore.getDomainName();
                if (Tools.isNotEmpty(currentDomain) && currentDomain.contains(".") && currentDomain.equals(group.getDomainName())==false) {
                    lastGroupId = 0;
                    request.getSession().removeAttribute(Constants.SESSION_GROUP_ID);
                }
            }
        }

        return lastGroupId;
    }
}
