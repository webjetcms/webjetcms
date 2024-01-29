package sk.iway.iwcm.doc.clone_structure;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
/*
 * Replace for "/admin/clone.do" old struts link
 */
@Controller
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_clone_structure')")
public class CloneStructureController {

    @PostMapping("/apps/clone_structure/admin/clone/")
    @ResponseBody
    public String cloneStructure(@RequestParam int srcGroupId, @RequestParam int destGroupId, @RequestParam(required = false) boolean keepMirroring, HttpServletRequest request, HttpServletResponse response) {
        try {
            return CloneStructureService.cloneStructure(srcGroupId, destGroupId, keepMirroring, request, response);
        } catch(Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}
