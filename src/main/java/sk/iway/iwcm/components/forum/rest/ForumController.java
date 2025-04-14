package sk.iway.iwcm.components.forum.rest;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.users.UsersDB;

@Controller
@RequestMapping(value = "/apps/forum")
public class ForumController {
    
    @PostMapping("/saveforum")
    public String saveForum(@ModelAttribute("forumForm") DocForumEntity forumForm, HttpServletRequest request, HttpServletResponse response) {
        try {
            return DocForumService.saveDocForum(request, response, forumForm);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/saveForumFile")
    public String saveForumFile(@RequestParam("uploadedFile") CommonsMultipartFile uploadFile, HttpServletRequest request, HttpServletResponse response) {
        try {
            return DocForumService.uploadForumFile(uploadFile, request);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/createStructure")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createStructure(HttpServletRequest request,
            @RequestParam List<String> data) {
        try {
            GroupsDB groupsDB = GroupsDB.getInstance();
            GroupDetails baseGroupDetails = groupsDB
                    .getGroup(Tools.getIntValue(request.getSession().getAttribute("iwcm_group_id").toString(), -1));
            String zakladnaCesta = baseGroupDetails.getFullPath();
            GroupDetails actualGroup = baseGroupDetails;

            DocDB docDB = DocDB.getInstance();
            for (String line : data) {
                if (line.startsWith("↳ ") == false) {
                    actualGroup = groupsDB.getGroupByPath(zakladnaCesta + "/" + line);
                    if (actualGroup == null) {
                        actualGroup = groupsDB.getCreateGroup(zakladnaCesta + "/" + line);
                        if (Constants.getBoolean("syncGroupAndWebpageTitle"))
                            ulozStrankuSekcie(request, actualGroup, line, false);
                    }
                } else {
                    boolean pageExists = false;
                    List<DocDetails> docs = docDB.getDocByGroup(actualGroup.getGroupId());
                    for (int i = 0; i < docs.size(); i++) {
                        if ((docs.get(i)).getTitle().equals(line.substring(1))) {
                            pageExists = true;
                            break;
                        }
                    }
                    if (!pageExists)
                        ulozStrankuSekcie(request, actualGroup, line.replace("↳ ", ""), true);
                }
            }            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Štruktúra vytvorená"));
        } catch (Exception e) {
            if (e.getMessage().contains("Structure creation exception")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    public void ulozStrankuSekcie(HttpServletRequest request, GroupDetails group, String title, boolean available) throws Exception{
        Identity user = UsersDB.getCurrentUser(request);

        EditorForm editorForm = EditorDB.getEditorForm(request, -1, -1, group.getGroupId());
        editorForm.setAuthorId(user.getUserId());
        editorForm.setTempId(group.getTempId());
        editorForm.setPublish("1");
        editorForm.setAvailable(available);
        editorForm.setTitle(title);
        editorForm.setNavbar(title);
        int historyId = EditorDB.saveEditorForm(editorForm, request);
        if (historyId <= 0) {
            throw new Exception("Structure creation exception");
        }
    }
}
