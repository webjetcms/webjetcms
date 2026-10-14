package sk.iway.iwcm.editor.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;

@Datatable
@RestController
@RequestMapping(value = "/admin/rest/web-pages")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class WebpagesRestController extends WebpagesDatatable {

    @Autowired
    public WebpagesRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
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

    /** Previews headers for unsaved editor values without saving or publishing the page. */
    @PostMapping("/http-headers")
    public Map<String, String> httpHeaders(@RequestBody DocDetails doc, HttpServletRequest request, HttpServletResponse response) {
        EditorForm form = new EditorForm(doc);
        if (Boolean.TRUE.equals(doc.getGenerateUrlFromTitle())) form.setVirtualPath("");
        else if (Boolean.TRUE.equals(doc.getUrlInheritGroup())) form.setVirtualPath(doc.getEditorVirtualPath() == null ? "" : doc.getEditorVirtualPath());
        EditorDB.setVirtualPath(form);
        String path = form.getVirtualPath();
        if (path == null || path.isEmpty()) path = "/showdoc.do?docid=" + doc.getDocId();
        return PathFilter.previewPageHeaders(path, doc, request, response);
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
