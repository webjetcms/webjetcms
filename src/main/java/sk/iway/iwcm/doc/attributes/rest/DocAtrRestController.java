package sk.iway.iwcm.doc.attributes.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrRepository;

@RestController
@RequestMapping("/admin/rest/webpages/attributes/doc")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
public class DocAtrRestController {

    private final DocAtrRepository docAtrRepository;

    @Autowired
    public DocAtrRestController(DocAtrRepository docAtrRepository) {
        this.docAtrRepository = docAtrRepository;
    }

    /**
     * Vrati zoznam uz zadanych hodnot v danom atribute
     * @param term
     * @return
     */
    @GetMapping("/autoselect/{atrId}/")
    public List<String> getAutoSelect(@PathVariable Integer atrId, @RequestParam String term) {

        if (term == null) term = "";
        String termLC = term.toLowerCase().trim();

        List<String> ac = new ArrayList<>();

        //get all distinct current values
        List<String> values = docAtrRepository.findAutoSelect(atrId);

        //Loop gained entities and add group name to autcomplete list "ac"
        for(String value : values) {
            if (Tools.isEmpty(value)) continue;
            if (Tools.isEmpty(termLC) || "%".equals(termLC) || "*".equals(termLC) || value.toLowerCase().contains(term)) ac.add(value);
        }

        return ac;
    }

}
