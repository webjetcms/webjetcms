package sk.iway.iwcm.components.seo.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsEntity;
import sk.iway.iwcm.components.seo.jpa.ManagementKeywordsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/positions")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class PositionsRestController extends DatatableRestControllerV2<ManagementKeywordsEntity, Long> {

    private final ManagementKeywordsRepository keywordsRepository;

    @Autowired
    public PositionsRestController(ManagementKeywordsRepository keywordsRepository) {
        super(keywordsRepository);
        this.keywordsRepository = keywordsRepository;
    }

    @Override
    public void beforeSave(ManagementKeywordsEntity entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<ManagementKeywordsEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<ManagementKeywordsEntity> page = new DatatablePageImpl<>(keywordsRepository.findAll(pageable));
        return page;
    }
}
