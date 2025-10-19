package sk.iway.iwcm.components.news;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.users.UsersDB;

/**
 * Rest controller for app news, there are only support methods,
 * because for data it will use WebpagesRestController calls
 */
@Datatable
@RestController
@RequestMapping(value = "/admin/rest/news/news-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_news')")
public class NewsRestController extends WebpagesDatatable {

    @Autowired
    public NewsRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {

        GetAllItemsDocOptions options = getDefaultOptions(pageable, false);

        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));
        groupId = getNewsGroupId(groupId);
        getRequest().getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(groupId));

        options.setGroupId(groupId);

        //verify perms after groupId change
        if (options.getGroupId()!=Constants.getInt("systemPagesRecentPages") && GroupsDB.isGroupEditable(getUser(), options.getGroupId())==false && GroupsDB.isGroupViewable(getUser(), options.getGroupId())==false) {
            throwError("components.jstree.access_denied__group");
        }

        return WebpagesService.getAllItems(options);
    }

    @Override
    public DocDetails getOneItem(long id) {
        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));
        int historyId = Tools.getIntValue(getRequest().getParameter("historyId"), -1);

        groupId = getNewsGroupId(groupId);

        List<NotifyBean> notifyList = new ArrayList<>();
        DocDetails docToReturn = WebpagesService.getOneItem(id, groupId, historyId, editorFacade, docAtrDefRepository, notifyList, getRequest());
        addNotify(notifyList);

        return docToReturn;
    }

    @RequestMapping(value = "/convertIdsToNamePair")
    public List<LabelValue> convertIdsToNamePair(@RequestParam String ids, @RequestParam(required = false) String include, HttpServletRequest request) {

        List<LabelValue> list = new ArrayList<>();

        if (ids.startsWith("constant:")) {
            ids = Constants.getString(ids.substring(9), "");
        }

        if (Tools.isNotEmpty(include) && include.startsWith("!INCLUDE(")) {
            PageParams pp = new PageParams(include);
            int[] groupIds = Tools.getTokensInt(pp.getValue("groupIds", null), ",+");
            String append = pp.getBooleanValue("expandGroupIds", false) ? "*" : "";
            StringBuilder includeIds = new StringBuilder();
            StringBuilder includeIdsNames = new StringBuilder();
            for (int groupId : groupIds) {
                if (includeIds.isEmpty()==false) includeIds.append(",");
                includeIds.append(""+groupId).append(append);

                GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
                if (group != null) {
                    if (includeIdsNames.isEmpty()==false) includeIdsNames.append(", ");
                    includeIdsNames.append(group.getFullPath()).append(append);
                }
            }
            ids = includeIds.toString();
            list.add(new LabelValue(includeIdsNames.toString(), ids));
        } else {
            GroupsDB groupsDB = GroupsDB.getInstance();
            String currentDomain = DocDB.getDomain(request);

            if (Tools.isEmpty(ids)) {
                Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
                String trashDirName = propSystem.getText("config.trash_dir");

                //we dont have any ids, try to search for NEWS include in all groups
                // -------------------------- set by GroupsDB if parent folder doesnt exists
                List<String> dataList = new SimpleQuery().forListString("SELECT data FROM documents WHERE data LIKE '%!INCLUDE(/components/news/%' AND file_name NOT LIKE '"+trashDirName+"%' AND file_name NOT LIKE '--------------------------'");
                Set<String> duplicityCheck = new HashSet<>();

                for (String data : dataList) {

                    Pattern pattern = Pattern.compile("!INCLUDE\\(/components/news/(.*?)\\)!");
                    Matcher matcher = pattern.matcher(data);

                    while (matcher.find()) {
                        String group = matcher.group();

                        PageParams pp = new PageParams(group);
                        int[] groupIds = Tools.getTokensInt(pp.getValue("groupIds", null), ",+");
                        for (int groupId : groupIds) {
                            if (groupId > 0) {
                                GroupDetails groupDetails = groupsDB.getGroup(groupId);
                                if (groupDetails != null) {
                                    if (Constants.getBoolean("multiDomainEnabled") && currentDomain.equals(groupDetails.getDomainName())==false) continue;

                                    String append = pp.getBooleanValue("expandGroupIds", false) ? "*" : "";
                                    String value = String.valueOf(groupId)+append;

                                    if (duplicityCheck.contains(value)==false) {
                                        list.add(new LabelValue(groupDetails.getFullPath()+append, value));
                                        duplicityCheck.add(value);
                                    }
                                }
                            }
                        }
                    }
                }

                //sort list using stream api by label property
                list.sort((o1, o2) -> o1.getLabel().compareTo(o2.getLabel()));
            } else {
                //add all groups from ids
                ids = ids.trim();
                String[] strIds = ids.split("[,+]");

                for(String strId : strIds) {
                    boolean withSubfolders = false;
                    if(strId.endsWith("*")) {
                        withSubfolders = true;
                        strId = strId.substring(0, strId.length() - 1);
                    }

                    int groupId = Tools.getIntValue(strId, -1);
                    GroupDetails group = groupsDB.getGroup(groupId);
                    if (group != null) {
                        if (Constants.getBoolean("multiDomainEnabled") && currentDomain.equals(group.getDomainName())==false) continue;

                        if(withSubfolders) list.add(new LabelValue(group.getFullPath(), groupId + "*"));
                        else list.add(new LabelValue(group.getFullPath(), String.valueOf(groupId)));
                    }
                }
            }
        }

        //Filter folder's by PERM'S
        Identity currentUser = UsersDB.getCurrentUser(request);
        GroupsDB groupsDB = GroupsDB.getInstance();
        int[] editableGroups = Tools.getTokensInt(currentUser.getEditableGroups(true), ",");
        int[] editablePages = Tools.getTokensInt(currentUser.getEditablePages(), ",");

        if(editableGroups.length < 1) {
            if(editablePages.length < 1) return list;
            else return new ArrayList<>();
        } else {
            int[] expandedEditableGroups = groupsDB.expandGroupIdsToChilds(editableGroups, true);
            return list.stream()
                    .filter(group -> Tools.containsOneItem(expandedEditableGroups, Tools.getIntValue(group.getValue(), -1)))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Expand groupID from list of groupID's
     * @param currentGroupId
     * @return
     */
    private int getNewsGroupId(int currentGroupId) {
        int groupId = currentGroupId;
        //news version get group ID in groupIdList parameter
        String groupIdList = getRequest().getParameter("groupIdList");
        if (Tools.isNotEmpty(groupIdList)) {
            if (groupId < 1 || groupId == Constants.getInt("rootGroupId")) {
                try {
                    groupIdList = Tools.replace(groupIdList, "*", "");
                    if (groupIdList.indexOf(",")!=-1) groupIdList=groupIdList.substring(0, groupIdList.indexOf(","));

                    int groupIdParser = Tools.getIntValue(groupIdList, -1);
                    if (groupIdParser > 0) groupId = groupIdParser;
                } catch (Exception e) {
                    //do nothing - failsafe
                }
            }
        }
        return groupId;
    }
}
