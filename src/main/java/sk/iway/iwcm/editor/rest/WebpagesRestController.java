package sk.iway.iwcm.editor.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.system.datatable.Datatable;

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

}
