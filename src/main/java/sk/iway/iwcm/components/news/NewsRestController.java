package sk.iway.iwcm.components.news;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

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
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.users.UsersDB;

/**
 * Rest controller for app news, there are only support methods,
 * because for data it will use WebpagesRestController calls
 */
@RestController
@RequestMapping(value = "/admin/rest/news/news-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_news')")
public class NewsRestController {

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
            for (int groupId : groupIds) {
                if (includeIds.isEmpty()==false) includeIds.append(",");
                includeIds.append(""+groupId).append(append);
            }
            ids = includeIds.toString();
            list.add(new LabelValue(ids, ids));
        } else {
            GroupsDB groupsDB = GroupsDB.getInstance();
            String currentDomain = DocDB.getDomain(request);

            if (Tools.isEmpty(ids)) {
                Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
                String trashDirName = propSystem.getText("config.trash_dir");

                //we dont have any ids, try to search for NEWS include in all groups
                List<String> dataList = new SimpleQuery().forListString("SELECT data FROM documents WHERE data LIKE '%!INCLUDE(/components/news/%' AND file_name NOT LIKE '"+trashDirName+"%'");
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
                int[] groupIds = Tools.getTokensInt(ids, ",+");

                for (int groupId : groupIds) {
                    GroupDetails group = groupsDB.getGroup(groupId);
                    if (group != null) {
                        if (Constants.getBoolean("multiDomainEnabled") && currentDomain.equals(group.getDomainName())==false) continue;
                        list.add(new LabelValue(group.getFullPath(), String.valueOf(groupId)));
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
}
