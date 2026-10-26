package sk.iway.iwcm.editor.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.structuremirroring.DocMirroringServiceV9;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.components.structuremirroring.MirroringService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.DeleteMirrorsResult;

/**
 * Previews direct mirrors targeted by deletion without expanding folder contents or deleting data.
 */
@Service
public class DeleteMirrorsService {
    private final EditorService editorService;

    public DeleteMirrorsService(EditorService editorService) {
        this.editorService = editorService;
    }

    /**
     * Returns unique mirrors of the selected pages, excluding the selection itself.
     * Paths inaccessible to the user are represented only by the hidden-mirrors flag.
     *
     * @param ids selected page IDs
     * @param user current administration user
     * @return direct page mirrors available for the delete confirmation
     */
    public DeleteMirrorsResult getPageMirrors(List<Integer> ids, Identity user) {
        Set<Integer> selectedIds = getSelectedIds(ids);
        Set<Integer> seenIds = new HashSet<>(selectedIds);
        DeleteMirrorsResult result = new DeleteMirrorsResult();
        DocDB docDB = DocDB.getInstance();
        GroupsDB groupsDB = GroupsDB.getInstance();

        for (int id : selectedIds) {
            DocDetails doc = docDB.getBasicDocDetails(id, false);
            if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (canAccessPage(doc, user) == false) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

            int syncId = DocMirroringServiceV9.getSyncId(id);
            // Match ON_DELETE even in Trash: only configured mirroring trees participate.
            if (syncId <= 1 || MirroringService.isEnabled(doc.getGroupId()) == false) continue;

            for (DocDetails mirror : DocMirroringServiceV9.getDocBySyncId(syncId, id)) {
                if (seenIds.add(mirror.getDocId()) == false) continue;
                if (canAccessPage(mirror, user) == false) {
                    result.setHiddenMirrors(true);
                    continue;
                }
                result.getItems().add(new DeleteMirrorsResult.Item(mirror.getDocId(), mirror.getFullPath(), groupsDB.getDomain(mirror.getGroupId())));
            }
        }
        return result;
    }

    /**
     * Returns unique mirrors of the selected folders without looking up their pages or descendants.
     *
     * @param ids selected folder IDs
     * @param user current administration user
     * @return direct folder mirrors, with an indicator for paths inaccessible to the user
     */
    public DeleteMirrorsResult getGroupMirrors(List<Integer> ids, Identity user) {
        if (user == null || user.isDisabledItem("deleteDir") || user.isDisabledItem("editDir")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Set<Integer> selectedIds = getSelectedIds(ids);
        Set<Integer> seenIds = new HashSet<>(selectedIds);
        DeleteMirrorsResult result = new DeleteMirrorsResult();
        GroupsDB groupsDB = GroupsDB.getInstance();

        for (int id : selectedIds) {
            GroupDetails group = groupsDB.getGroup(id);
            if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (isInTenant(group) == false || GroupsDB.isGroupEditable(user, id) == false) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            int syncId = GroupMirroringServiceV9.getSyncId(id);
            // A configured root can remain enabled after the whole folder moves to Trash.
            if (syncId <= 1 || MirroringService.isEnabled(id) == false) continue;

            for (GroupDetails mirror : GroupMirroringServiceV9.getGroupsBySyncId(syncId, id)) {
                if (seenIds.add(mirror.getGroupId()) == false) continue;
                if (isInTenant(mirror) == false || (GroupsDB.isGroupEditable(user, mirror.getGroupId()) == false
                        && GroupsDB.isGroupViewable(user, mirror.getGroupId()) == false)) {
                    result.setHiddenMirrors(true);
                    continue;
                }
                result.getItems().add(new DeleteMirrorsResult.Item(mirror.getGroupId(), mirror.getFullPath(), mirror.getDomainName()));
            }
        }
        return result;
    }

    private Set<Integer> getSelectedIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return new LinkedHashSet<>(ids);
    }

    private boolean canAccessPage(DocDetails doc, Identity user) {
        return user != null && isInTenant(GroupsDB.getInstance().getGroup(doc.getGroupId()))
                && editorService.isPageEditable(user, doc, true);
    }

    private boolean isInTenant(GroupDetails group) {
        return group != null && (InitServlet.isTypeCloud() == false
                || CloudToolsForCore.getDomainName().equals(group.getDomainName()));
    }
}
