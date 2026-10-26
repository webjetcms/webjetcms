package sk.iway.iwcm.doc.clone_structure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.structuremirroring.MirroringService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;
/*
 * Replace for "/admin/clone.do" old struts link
 */
@RestController
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_clone_structure')")
public class CloneStructureController {

    private final DocDetailsRepository docDetailsRepository;

    public CloneStructureController(DocDetailsRepository docDetailsRepository) {
        this.docDetailsRepository = docDetailsRepository;
    }

    @PostMapping("/apps/clone_structure/admin/clone/")
    public String cloneStructure(@RequestParam int srcGroupId, @RequestParam int destGroupId, @RequestParam(required = false) Boolean keepMirroring, @RequestParam(required = false) Boolean keepVirtualPath, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (keepMirroring == null) keepMirroring = false;
            if (keepVirtualPath == null) keepVirtualPath = false;
            String returnValue =  CloneStructureService.cloneStructure(srcGroupId, destGroupId, keepMirroring, keepVirtualPath, request, response);

            return returnValue;
        } catch(Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/apps/clone_structure/admin/cancel_sync")
    public String cancelSync(@RequestParam int rootGroupId, HttpServletRequest request) {
        GroupsDB groupsDB = GroupsDB.getInstance();
        if (rootGroupId < 1 || groupsDB.getGroup(rootGroupId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Identity user = UsersDB.getCurrentUser(request);
        String domainName = CloudToolsForCore.getDomainName();
        for (GroupDetails group : groupsDB.getGroupsTree(rootGroupId, true, true)) {
            if (!GroupsDB.isGroupEditable(user, group.getGroupId()) ||
                (Constants.getBoolean("multiDomainEnabled") && !domainName.equals(group.getDomainName()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (Constants.getBoolean("adminCheckUserGroups")) {
                for (DocDetails doc : docDetailsRepository.findAllByGroupId(group.getGroupId())) {
                    if (!DocDB.canAccess(doc, user, true)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
        }

        try {
            MirroringService.clearSyncId(rootGroupId);
        } catch (Exception ex) {
            return ex.getMessage();
        }
        return null;
    }

}
