package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.DeleteMirrorsService;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.spring.DatatableExceptionHandlerV2;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

@Datatable
@RestController
@RequestMapping(value = "/admin/rest/web-pages")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class WebpagesRestController extends WebpagesDatatable {

    private final DeleteMirrorsService deleteMirrorsService;
    private final EditorService editorService;

    @Autowired
    public WebpagesRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository, DeleteMirrorsService deleteMirrorsService, EditorService editorService) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
        this.deleteMirrorsService = deleteMirrorsService;
        this.editorService = editorService;
    }

    /**
     * Previews additional mirrored pages affected by deleting the selected pages, without deleting data.
     * Called by {@code bindDeleteMirrors} when the page delete confirmation opens, before submission is enabled.
     *
     * @param ids IDs of the pages selected for deletion
     * @return unique additional mirrors with accessible paths and a flag indicating any inaccessible mirrors
     */
    @PostMapping("/deletion-preview")
    public DeleteMirrorsResult getDeletionPreview(@RequestParam("ids") List<Integer> ids) {
        return deleteMirrorsService.getPageMirrors(ids, getUser());
    }

    /**
     * Registers rows that request file cleanup before deleting any page, so the batch shares one cleanup pass.
     *
     * @param request current Editor request
     * @param batch Editor action and submitted page rows
     * @return Editor response with a cleanup summary when available
     */
    @Override
    public ResponseEntity<DatatableResponse<DocDetails>> handleEditor(HttpServletRequest request,
            @RequestBody DatatableRequest<Long, DocDetails> batch) {
        if (!batch.isDelete()) return super.handleEditor(request, batch);
        batch.getData().forEach(this::selectForFileCleanup);
        return deleteWithFileCleanup(() -> super.handleEditor(request, batch), DatatableResponse::addNotify);
    }

    private boolean selectForFileCleanup(Long id, DocDetails entity) {
        return entity != null && entity.getEditorFields() != null && entity.getEditorFields().isDeleteUnusedPageFiles()
            && editorService.selectForFileCleanup(Math.toIntExact(id));
    }

    /**
     * Handles optional file cleanup for a single deletion. An enclosing Editor batch finishes
     * cleanup for its selected rows after processing the batch.
     *
     * @param id page ID to delete
     * @param entity submitted page containing the cleanup choice
     * @return deletion result with a cleanup notification when a standalone deletion produces one
     */
    @Override
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") long id, @RequestBody DocDetails entity) {
        // The enclosing Editor request already selected its rows and will finish the whole batch.
        if (!selectForFileCleanup(id, entity)) return super.delete(id, entity);
        return deleteWithFileCleanup(() -> super.delete(id, entity),
            (body, summary) -> body.put("notify", List.of(summary)));
    }

    /**
     * Finishes cleanup for completed deletions even if a later row fails. On failure, makes any
     * summary available to the error handler and rethrows the original deletion exception.
     *
     * @param <T> type of the deletion response body
     * @param delete operation that deletes a page or batch
     * @param addSummary attaches the cleanup notification to the successful response body
     * @return the operation's response with any cleanup notification attached
     */
    private <T> ResponseEntity<T> deleteWithFileCleanup(Supplier<ResponseEntity<T>> delete, BiConsumer<T, NotifyBean> addSummary) {
        ResponseEntity<T> response;
        try {
            response = delete.get();
        } catch (RuntimeException ex) {
            try {
                NotifyBean summary = editorService.finishFileCleanup(true);
                if (summary != null) RequestBean.setAttribute(DatatableExceptionHandlerV2.ERROR_NOTIFY, summary);
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
                Logger.error(WebpagesRestController.class, cleanupError);
            }
            throw ex;
        }
        NotifyBean summary = editorService.finishFileCleanup(false);
        if (summary != null && response.getBody() != null) addSummary.accept(response.getBody(), summary);
        return response;
    }

    /** Returns groups available on every selected page, or assigned to at least one page for removal. */
    @PostMapping(value = "/perex-groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<OptionDto> perexGroupOptions(@RequestBody Long[] ids, @RequestParam boolean remove) {
        if (ids.length == 0) return List.of();
        DocDB docDB = DocDB.getInstance();
        List<PerexGroupBean> groups = new ArrayList<>(docDB.getPerexGroups());
        Set<Integer> assigned = new LinkedHashSet<>();
        for (Long id : ids) {
            // Resolve linked copies/slave docs in the same way as the editor data used by processAction.
            DocDetails doc = editorService.prepareDocForEditor(id.intValue(), -1);
            editorService.checkPermissions(getUser(), doc, false);
            if (remove) {
                if (doc.getPerexGroups() != null) assigned.addAll(Arrays.asList(doc.getPerexGroups()));
            } else {
                Set<Integer> available = new LinkedHashSet<>();
                for (PerexGroupBean group : docDB.getPerexGroups(doc.getGroupId())) available.add(group.getPerexGroupId());
                groups.removeIf(group -> available.contains(group.getPerexGroupId()) == false);
            }
        }
        if (remove) groups.removeIf(group -> assigned.contains(group.getPerexGroupId()) == false);
        return DocDB.fixPerexNameDuplicityForOptions(groups);
    }

    /** Returns both user group types, limited to assigned groups when removing them. */
    @PostMapping(value = "/user-groups", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<OptionDto> userGroupOptions(@RequestBody Long[] ids, @RequestParam boolean remove) {
        if (ids.length == 0) return List.of();
        Set<Integer> assigned = new LinkedHashSet<>();
        for (Long id : ids) {
            DocDetails doc = editorService.prepareDocForEditor(id.intValue(), -1);
            editorService.checkPermissions(getUser(), doc, false);
            if (remove) {
                for (int groupId : Tools.getTokensInt(doc.getPasswordProtected(), ",")) assigned.add(groupId);
            }
        }
        return UserGroupsDB.getInstance().getUserGroups().stream()
            .filter(group -> remove == false || assigned.contains(group.getUserGroupId()))
            .map(group -> new OptionDto(group.getUserGroupName(), String.valueOf(group.getUserGroupId()), null))
            .toList();
    }

    /** Applies a group delta to freshly loaded editor data through the normal save workflow. */
    @Override
    public boolean processAction(DocDetails entity, String action) {
        boolean userGroup = "addUserGroup".equals(action) || "removeUserGroup".equals(action);
        boolean remove = "removePerexGroup".equals(action) || "removeUserGroup".equals(action);
        if (userGroup == false && remove == false && "addPerexGroup".equals(action) == false) return super.processAction(entity, action);

        editorService.checkPermissions(getUser(), entity, false);
        String invalidGroup = userGroup ? "editor.user_groups.invalid" : "editor.perex_groups.invalid";
        if (entity.getDocId() < 1 || Tools.getIntValue(getRequest().getParameter("historyId"), -1) > 0
                || Tools.getIntValue(getRequest().getParameter("groupId"), -1) == Constants.getInt("systemPagesDocsToApprove")) {
            throwError(invalidGroup);
        }
        int groupId = Tools.getIntValue(getRequest().getParameter("customData"), -1);
        if (userGroup) return changeUserGroup(entity, groupId, remove);

        DocDB docDB = DocDB.getInstance();
        List<PerexGroupBean> available = remove ? docDB.getPerexGroups() : docDB.getPerexGroups(entity.getGroupId());
        if (groupId < 1 || available.stream().noneMatch(group -> group.getPerexGroupId() == groupId)) {
            throwError("editor.perex_groups.invalid");
        }

        Set<Integer> groups = new LinkedHashSet<>();
        if (entity.getPerexGroups() != null) groups.addAll(Arrays.asList(entity.getPerexGroups()));
        boolean changed = remove ? groups.remove(groupId) : groups.add(groupId);
        if (changed) {
            entity.setPerexGroups(groups.toArray(Integer[]::new));
            editItem(entity, entity.getDocId());
        }
        return true;
    }

    /** Keeps the editor's split fields in sync so saving does not overwrite passwordProtected. */
    private boolean changeUserGroup(DocDetails entity, int groupId, boolean remove) {
        List<UserGroupDetails> available = UserGroupsDB.getInstance().getUserGroups();
        if (groupId < 1 || available.stream().noneMatch(group -> group.getUserGroupId() == groupId)) {
            throwError("editor.user_groups.invalid");
        }
        Set<Integer> groups = new LinkedHashSet<>();
        for (int id : Tools.getTokensInt(entity.getPasswordProtected(), ",")) groups.add(id);
        boolean changed = remove ? groups.remove(groupId) : groups.add(groupId);
        if (changed) {
            List<Integer[]> split = UserDetailsService.splitGroupsToPermsAndEmails(groups.stream().mapToInt(Integer::intValue).toArray());
            entity.getEditorFields().setPermisions(split.get(0));
            entity.getEditorFields().setEmails(split.get(1));
            entity.setPasswordProtected(UserDetailsService.getUserGroupIds(split.get(0), split.get(1)));
            editItem(entity, entity.getDocId());
        }
        return true;
    }

    /**
     * Vrati zoznam parent adresarov pre zadane docid, nastavi aj domenu ak je odlisna
     * a kartu (System/Kos) v ktorej sa nachadza koren adresarov
     * @param docId
     * @return
     */
    @RequestMapping(path="/parents/{id}")
    public ParentGroupsResult parentGroups(@PathVariable("id") int docId) {
        ParentGroupsResult result = new ParentGroupsResult();

        DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
        if (doc != null) {
            List<GroupDetails> parentGroups = GroupsDB.getInstance().getParentGroups(doc.getGroupId());
            result.setParentGroups(doc.getGroup(), parentGroups);
        }

        return result;
    }

    @Override
    public void validateEditorForCustomFields(HttpServletRequest request, DatatableRequest<Long, DocDetails> target,
            Identity user, Errors errors, Long id, DocDetails entity) {

        //set textKeyPrefix to properly resolve custom fields
        RequestBean.clearTextKeyPrefixes();
        if (entity != null) DocEditorFields.setRequestBeanTextPrefixes(entity.getTempId());

        super.validateEditorForCustomFields(request, target, user, errors, id, entity);
    }



}
