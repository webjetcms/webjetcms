package sk.iway.iwcm.components.forum.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import sk.iway.iwcm.components.forum.jpa.DocForumEntity;

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
}
