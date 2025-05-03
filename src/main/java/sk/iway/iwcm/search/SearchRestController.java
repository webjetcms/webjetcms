package sk.iway.iwcm.search;

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

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.SpecSearch;

/**
 * SearchRestController is responsible for handling search requests related to
 * DocDetails. It extends WebpagesDatatable and provides functionality to
 * retrieve and filter DocDetails based on search criteria.
 */
@Datatable
@RestController
@RequestMapping("/admin/rest/search")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
public class SearchRestController extends WebpagesDatatable {

    @Autowired
    public SearchRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        GetAllItemsDocOptions options = getDefaultOptions(pageable, true);

        //Do not test perms for groupId, it's test later
        DatatablePageImpl<DocDetails> pageImpl = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new DocDetails(), pageable));
        WebpagesService.addOptions(pageImpl, options);
        return pageImpl;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {
        SpecSearch<DocDetails> specSearch = new SpecSearch<>();
        WebpagesService.addBaseSpecSearch(specSearch, params, predicates, root, builder);
        SearchService.getWebPagesData(params, getUser(), predicates, builder, root);
        super.addSpecSearch(params, predicates, root, builder);
    }
}