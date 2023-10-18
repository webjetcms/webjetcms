package sk.iway.iwcm.components.seo.rest;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsEntity;
import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/seo/management-keywords")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class ManagementKeywordsRestController extends DatatableRestControllerV2<ManagementKeywordsEntity, Long> {

    private final ManagementKeywordsRepository keywordsRepository;

    @Autowired
    public ManagementKeywordsRestController(ManagementKeywordsRepository keywordsRepository) {
        super(keywordsRepository);
        this.keywordsRepository = keywordsRepository;
    }

    @Override
    public Page<ManagementKeywordsEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<ManagementKeywordsEntity> page = new DatatablePageImpl<>(keywordsRepository.findAll(pageable));
        //We need list of user name's to select
        page.addOptions("author", SeoService.getUsersFromIds( keywordsRepository.getDistinctUserIds() ), "fullName", "userId", false);
        return page;
    }

    @Override
    public void beforeSave(ManagementKeywordsEntity entity) {
        Identity user = UsersDB.getCurrentUser(getRequest());
        entity.setAuthor(user.getUserId());
        entity.setCreatedTime(new Date());
    }

    @GetMapping("/domain-autocomplete")
    public List<String> getDomainAutocomplete(@RequestParam String term) {
        return keywordsRepository.getDistinctDomainsLike("%" + term + "%");
    }

    @GetMapping("/searchBot-autocomplete")
    public List<String> getSearchBotAutocomplete(@RequestParam String term) {
        return keywordsRepository.getMostUsedBotsLike("%" + term + "%");
    }
}
