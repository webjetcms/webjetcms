package sk.iway.iwcm.components.forum.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.forum.jpa.DocForumEditorFields;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.DocForumRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

@RestController
@RequestMapping("/admin/rest/forum")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_diskusia')")
@Datatable
public class DocForumRestController extends DatatableRestControllerV2<DocForumEntity, Long> {


    @Autowired
    public DocForumRestController(DocForumRepository forumRepository) {
        super(forumRepository);
    }

    @Override
    public Page<DocForumEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<DocForumEntity> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new DocForumEntity(), pageable));

        //Add list of icons
        page.addOptions("editorFields.statusIcons", DocForumService.getStatusIconOptions(getProp()), "label", "value", false);

        //Add user groups
        page.addOptions("forumGroupEntity.addMessagePerms", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
        page.addOptions("forumGroupEntity.adminPerms", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);

        return page;
    }

    @Override
    public DocForumEntity processFromEntity(DocForumEntity entity, ProcessItemAction action) {
        if(entity == null) return entity;

        //Prepare nested ForumGroupEntity (aka DB forum table)
        //!! MUST BE BEFORE editorFields (because editor fields can use values from forumGroup)
        ForumGroupService.prepareForumGroup(entity);

        DocForumEditorFields fef = new DocForumEditorFields();
        fef.fromDocForum(entity, getRequest(), getProp());
        entity.setEditorFields(fef);

        return entity;
    }

    @Override
    public void beforeSave(DocForumEntity entity) {
        //Set date of last change
        entity.setQuestionDate(new Date());

        //If APPROVING is disabled, remove approving email
        if(entity.getForumGroupEntity() != null && !entity.getForumGroupEntity().getMessageConfirmation())
            entity.getForumGroupEntity().setApproveEmail("");

        //If Active was changed, do it recursive for whole tree
        if(entity.getId() != null && entity.getId() > 0) {
            //When it's edit
            Boolean oldValue = (new SimpleQuery()).forBoolean("SELECT active FROM document_forum WHERE forum_id=? AND domain_id=?", entity.getId(), entity.getDomainId());
            //If active value (actual) is different than old value in DB
            if(oldValue != null && oldValue.equals(entity.getActive())) {
                if(Tools.isTrue(entity.getActive())) DocForumService.docForumRecursiveAction(DocForumService.ActionType.UNLOCK, entity.getId().intValue(), entity.getDocId(), getUser());
                else DocForumService.docForumRecursiveAction(DocForumService.ActionType.LOCK, entity.getId().intValue(), entity.getDocId(), getUser());
            }
        }
    }

    @Override
    public void afterSave(DocForumEntity entity, DocForumEntity saved) {
        //Save ForumGroupEntity too (it's forum DB table)
        ForumGroupService.saveForum(entity.getForumGroupEntity());
    }

    @Override
    public boolean deleteItem(DocForumEntity entity, long id) {
        setForceReload(true);
        return DocForumService.docForumRecursiveAction(DocForumService.ActionType.DELETE, (int) id, entity.getDocId(), getUser());
    }

    @Override
    public boolean processAction(DocForumEntity entity, String action) {
        if ("recoverForum".equals(action)) {
            //Restore soft-deleted forum's
            setForceReload(true);
            return DocForumService.docForumRecursiveAction(DocForumService.ActionType.RECOVER, entity.getId().intValue(), entity.getDocId(), getUser());
        } else if("approveForum".equals(action)) {
            //Approve forum's
            setForceReload(true);
            return DocForumService.docForumRecursiveAction(DocForumService.ActionType.APPROVE, entity.getId().intValue(), entity.getDocId(), getUser());
        } else if("rejectForum".equals(action)) {
            //Reject forum's
            setForceReload(true);
            return DocForumService.docForumRecursiveAction(DocForumService.ActionType.REJECT, entity.getId().intValue(), entity.getDocId(), getUser());
        }

        //Unknown action
        return false;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocForumEntity> root, CriteriaBuilder builder) {

        Identity user = getUser();
        JpaTools.removePredicateWithName("docDetails", predicates);

        SpecSearch<DocForumEntity> specSearch = new SpecSearch<>();

        specSearch.addSpecSearchByUserEditable(user, "docId", predicates, root, builder);

        String docTitle = params.get("searchDocDetails");
        if (Tools.isNotEmpty(docTitle)) {
            specSearch.addSpecSearchDocFullPath(docTitle, "docId", predicates, root, builder);
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, DocForumEntity> target, Identity currentUser, Errors errors, Long id, DocForumEntity entity) {
        //Validate that if we choose approving via email, email must be set and must have valid form !!
        if(entity.getForumGroupEntity() != null && Tools.isTrue(entity.getForumGroupEntity().getMessageConfirmation()) && Tools.isEmail( entity.getForumGroupEntity().getApproveEmail() ) == false)
            errors.rejectValue("errorField.forumGroupEntity.approveEmail", null, getProp().getText("components.forum.message_confirmation.field_requested"));
    }
}
