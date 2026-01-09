package sk.iway.iwcm.components.forum.rest;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.forum.ForumSortBy;
import sk.iway.iwcm.users.UsersDB;

@Controller
@RequestMapping
public class ForumController {

    private final EditorFacade editorFacade;

    @Autowired
    public ForumController(EditorFacade editorFacade) {
        this.editorFacade = editorFacade;
    }

    @PostMapping("/apps/forum/saveforum")
    public String saveForum(@ModelAttribute("forumForm") DocForumEntity forumForm, HttpServletRequest request, HttpServletResponse response) {
        try {
            return DocForumService.saveDocForum(request, response, forumForm);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/apps/forum/saveForumFile")
    public String saveForumFile(@RequestParam("uploadedFile") MultipartFile uploadFile, HttpServletRequest request, HttpServletResponse response) {
        try {
            return DocForumService.uploadForumFile(uploadFile, request);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PreAuthorize("@WebjetSecurityService.hasPermission('cmp_diskusia')")
    @PostMapping("/admin/rest/forum/prepare-structure")
    @ResponseBody
    public String prepareStructure(@RequestParam("structure") String structure, @RequestParam("groupId") int groupId, @RequestParam("pageData") String pageData, HttpServletRequest request) {
        //Nothing to prepare
        if(Tools.isEmpty(structure)) return null;

        DocDB docDB = DocDB.getInstance();
        GroupsDB groupsDB = GroupsDB.getInstance();

        GroupDetails baseGroupDetails = groupsDB.getGroup(groupId);
        if(baseGroupDetails == null) {
            Logger.error(ForumController.class, "GroupDetails with groupId: " + groupId + " was not found");
            return  "Zakladny adresar sa nepodarilo najst.";
        }
        String basePath = baseGroupDetails.getFullPath();

        try {

            JSONObject settingsJson = new JSONObject(pageData);
            String baseData = getBaseDataString(settingsJson);

            GroupDetails actualGroup = baseGroupDetails;
            BufferedReader br = new BufferedReader(new CharArrayReader(structure.toCharArray()));
            String line;

            while((line=br.readLine()) != null) {
                if(line.startsWith(" ") == false) {
                    actualGroup = groupsDB.getGroupByPath(basePath + "/" + line);
                    Logger.debug(ForumController.class, "actualGroup=" + actualGroup + " cesta=" + basePath + "/" + line);
                    if(actualGroup == null) {
                        actualGroup = groupsDB.getCreateGroup(basePath+"/"+line);
                        Logger.debug(ForumController.class, "actualGroup vytvoreny=" + actualGroup + " cesta=" + basePath + "/" + line);
                        //hlavna stranka je placeholder
                        if (Constants.getBoolean("syncGroupAndWebpageTitle")) {
                            String result = saveSectionDoc(request, actualGroup, line, baseData, false);
                            if(Tools.isNotEmpty(result)) return result;
                        }
                    }
                } else {
                    boolean pageExists = false;
                    List<DocDetails> docs = docDB.getDocByGroup(actualGroup.getGroupId());
                    for(int i=0;i<docs.size();i++) {
                        if(docs.get(i).getTitle().equals(line.substring(1))) {
                            pageExists = true;
                            break;
                        }
                    }
                    if(!pageExists) {
                        String result = saveSectionDoc(request, actualGroup, line.substring(1), baseData, true);
                        if(Tools.isNotEmpty(result)) return result;
                    }
                }
            }
        } catch(Exception ex) {
            Logger.error(ForumController.class, "Chyba pri akcii prepareStructure : " + ex);
            return ex.getMessage();
        }

        return null;
    }

    private String getBaseDataString(JSONObject settingsJson) {
        StringBuilder baseData = new StringBuilder("<section><div class=\"container\"><div class=\"row\"><div class=\"col-md-12\"><div class=\"column-content\">!INCLUDE(/components/forum/forum_mb.jsp, type=topics,");
        baseData.append(" pageSize=").append( Tools.getIntValue(settingsJson.getString("pageSize"),10) );
        baseData.append(", pageLinksNum=").append( Tools.getIntValue(settingsJson.getString("pageLinksNum"),10) );

        boolean useDelTimeLimit = false;
        int delMinutes = 0;
        if( (settingsJson.has("useDelTimeLimit") ? settingsJson.getBoolean("useDelTimeLimit") : false) == true) {
            useDelTimeLimit = true;
            delMinutes = Tools.getIntValue(settingsJson.getString("delMinutes"),0);
        }
        baseData.append(", useDelTimeLimit=").append(useDelTimeLimit);
        baseData.append(", delMinutes=").append(delMinutes);

        baseData.append(", showSearchBox=true");
        baseData.append(", sortTopicsBy=").append(  ForumSortBy.valueOf(Tools.getStringValue(settingsJson.getString("sortTopicsBy"), ForumSortBy.LastPost.getColumnName())) );

        baseData.append(", sortAscending=").append(settingsJson.has("sortAscending") ? settingsJson.getBoolean("sortAscending") : true);
        baseData.append(", notifyPageAuthor=").append(settingsJson.has("notifyPageAuthor") ? settingsJson.getBoolean("notifyPageAuthor") : false);
        baseData.append(", rootGroup=false)!</div></div></div></div></section>");

        return baseData.toString();
    }

    private String saveSectionDoc(HttpServletRequest request, GroupDetails group, String title, String data, boolean available) {
        Identity user = UsersDB.getCurrentUser(request);

        DocDetails editorForm = editorFacade.getDocForEditor(-1, -1, group.getGroupId());
        editorForm.setAuthorId(user.getUserId());
        editorForm.setTempId(group.getTempId());
        editorForm.setAvailable(available);
        editorForm.setTitle(title);
        editorForm.setNavbar(title);
        editorForm.setData(data);

        Logger.debug(ForumController.class, "Ukladam stranku: " + title);

        DocDetails saved = editorFacade.save(editorForm);
        int historyId = saved.getHistoryId();
        if (historyId > 0) {
            Logger.debug(ForumController.class, "Ulozenie stranky: " + title + " bolo uspesne, historyId:" + historyId);
            return null;
        } else {
            Logger.debug(ForumController.class, "Ulozenie stranky: " + title + " bolo NEUSPESNE");
            return "Ulozenie stranky: " + title + " bolo NEUSPESNE";
        }
    }
}
