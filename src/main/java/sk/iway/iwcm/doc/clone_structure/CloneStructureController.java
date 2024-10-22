package sk.iway.iwcm.doc.clone_structure;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/*
 * Replace for "/admin/clone.do" old struts link
 */
@RestController
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_clone_structure')")
public class CloneStructureController {

    @PostMapping("/apps/clone_structure/admin/clone/")
    public String cloneStructure(@RequestParam int srcGroupId, @RequestParam int destGroupId, @RequestParam(required = false) Boolean keepMirroring, @RequestParam(required = false) Boolean keepVirtualPath, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (keepMirroring == null) keepMirroring = false;
            if (keepVirtualPath == null) keepVirtualPath = false;
            return CloneStructureService.cloneStructure(srcGroupId, destGroupId, keepMirroring, keepVirtualPath, request, response);
        } catch(Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}
