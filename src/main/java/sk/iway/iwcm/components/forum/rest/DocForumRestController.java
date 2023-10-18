package sk.iway.iwcm.components.forum.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.forum.jpa.DocForumEditorFields;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.DocForumRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.system.jpa.JpaTools;

@RestController
@RequestMapping("/admin/rest/forum")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_diskusia')")
@Datatable
public class DocForumRestController extends DatatableRestControllerV2<DocForumEntity, Long> {

    private final DocForumService service;

    @Autowired
    public DocForumRestController(DocForumService service, DocForumRepository forumRepository) {
        super(forumRepository);
        this.service = service;
    }

    @Override
    public Page<DocForumEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<DocForumEntity> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new DocForumEntity(), pageable));

        //Add list of icons
        page.addOptions("editorFields.statusIcons", service.getStatusIconOptions(getProp()), "label", "value", false);

        return page;
    }

    @Override
    public DocForumEntity processFromEntity(DocForumEntity entity, ProcessItemAction action) {
        if(entity == null) return entity;

        DocForumEditorFields fef = new DocForumEditorFields();
        fef.fromDocForum(entity, getRequest(), getProp());
        entity.setEditorFields(fef);
        return entity;
    }

    @Override
    public void beforeSave(DocForumEntity entity) {
        //Set date of last change
        entity.setQuestionDate(new Date());
    }

    @Override
    public boolean deleteItem(DocForumEntity entity, long id) {
        setForceReload(true);
        return service.deleteDocForum(id);
    }

    @Override
    public boolean processAction(DocForumEntity entity, String action) {
        if ("undelete".equals(action)) {
            setForceReload(true);
            service.undeleteEntity(entity.getId());
            return true;
        }
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
}
