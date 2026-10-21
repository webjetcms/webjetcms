package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.OptionDto;

@Datatable
@RestController
@RequestMapping(value = "/admin/rest/web-pages")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class WebpagesRestController extends WebpagesDatatable {

    private final EditorService editorService;

    @Autowired
    public WebpagesRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository, EditorService editorService) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
        this.editorService = editorService;
    }

    /** Returns groups available on every selected page, or assigned to at least one page for removal. */
    @GetMapping("/perex-groups")
    public List<OptionDto> perexGroupOptions(@RequestParam("ids[]") Long[] ids, @RequestParam boolean remove) {
        if (ids.length == 0) return List.of();
        DocDB docDB = DocDB.getInstance();
        List<PerexGroupBean> groups = new ArrayList<>(docDB.getPerexGroups());
        Set<Integer> assigned = new LinkedHashSet<>();
        for (Long id : ids) {
            DocDetails doc = docDB.getDoc(id.intValue());
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

    /** Applies a perex group delta to freshly loaded editor data through the normal save workflow. */
    @Override
    public boolean processAction(DocDetails entity, String action) {
        boolean remove = "removePerexGroup".equals(action);
        if (remove == false && "addPerexGroup".equals(action) == false) return super.processAction(entity, action);

        editorService.checkPermissions(getUser(), entity, false);
        if (entity.getDocId() < 1 || Tools.getIntValue(getRequest().getParameter("historyId"), -1) > 0
                || Tools.getIntValue(getRequest().getParameter("groupId"), -1) == Constants.getInt("systemPagesDocsToApprove")) {
            throwError("editor.perex_groups.invalid");
        }
        int groupId = Tools.getIntValue(getRequest().getParameter("customData"), -1);
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
