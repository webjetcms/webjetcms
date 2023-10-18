package sk.iway.iwcm.doc.attributes.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefEntity;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/webpages/attributes/def")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_attributes')")
@Datatable
public class DocAtrDefRestController extends DatatableRestControllerV2<DocAtrDefEntity, Long> {

    private final DocAtrDefRepository docAtrDefRepository;

    @Autowired
    public DocAtrDefRestController(DocAtrDefRepository docAtrDefRepository) {
        super(docAtrDefRepository);
        this.docAtrDefRepository = docAtrDefRepository;
    }

    /**
     * Vrati zoznam uz existujucich skupin
     * @param term
     * @return
     */
    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term) {

        if (term == null) term = "";
        String termLC = term.toLowerCase().trim();

        List<String> ac = new ArrayList<>();

        //Get all where group name is like %term%, and distict because its autocomplete list and we dont want duplicity
        List<String> groups = docAtrDefRepository.findDistinctGroups(CloudToolsForCore.getDomainId());

        //Loop gained entities and add group name to autcomplete list "ac"
        for(String group : groups) {
            if (Tools.isEmpty(termLC) || "%".equals(termLC) || group.toLowerCase().contains(term)) ac.add(group);
        }

        return ac;
    }
}
