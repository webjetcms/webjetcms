package sk.iway.iwcm.doc.clone_structure;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
/*
 * Replace for "/admin/clone.do" old struts link
 */
@RestController
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_clone_structure')")
public class CloneStructureController {

    @PostMapping("/apps/clone_structure/admin/clone/")
    public String cloneStructure(@RequestParam int srcGroupId, @RequestParam int destGroupId, @RequestParam(required = false) Boolean keepMirroring, @RequestParam(required = false) Boolean keepVirtualPath, HttpServletRequest request, HttpServletResponse response) {
        //do not sync group and webpage title during sync, keep original titles
        boolean originalValue = Constants.getBoolean("syncGroupAndWebpageTitle");

        try {
            Constants.setBoolean("syncGroupAndWebpageTitle", false);

            if (keepMirroring == null) keepMirroring = false;
            if (keepVirtualPath == null) keepVirtualPath = false;
            String returnValue =  CloneStructureService.cloneStructure(srcGroupId, destGroupId, keepMirroring, keepVirtualPath, request, response);

            return returnValue;
        } catch(Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
        finally {
            Constants.setBoolean("syncGroupAndWebpageTitle", originalValue);
        }

        return null;
    }

    @PostMapping("/apps/clone_structure/admin/cancel_sync")
    public String cancelSync(@RequestParam int rootGroupId) {
        try {
            GroupsDB groupsDB = GroupsDB.getInstance();
            DocDB docDB = DocDB.getInstance();

            List<GroupDetails> groups = groupsDB.getGroupsTree(rootGroupId, true, true);
            for(GroupDetails group : groups) {
                List<DocDetails> docs = docDB.getDocByGroup(group.getGroupId());
                for(DocDetails doc : docs) {
                    doc.setSyncId(0);
                    DocDB.saveDoc(doc, false);
                }

                group.setSyncId(0);
                groupsDB.setGroup(group, false);
            }
            return null;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

}