package sk.iway.iwcm.components.seo.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.jpa.SearchEnginesDTO;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/stat-keywords-details")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class StatKeywordsDetailsRestController extends DatatableRestControllerV2<SearchEnginesDTO, Long> {
    
    @Autowired
    public StatKeywordsDetailsRestController() {
        super(null);
    }

    @Override
    public void beforeSave(SearchEnginesDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<SearchEnginesDTO> getAllItems(Pageable pageable) { 
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        return new DatatablePageImpl<>( SeoService.getStatKeywordsDetailsTableData(filter) );
    }

    @Override
    public Page<SearchEnginesDTO> searchItem(Map<String, String> params, Pageable pageable, SearchEnginesDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);
        return new DatatablePageImpl<>( SeoService.getStatKeywordsDetailsTableData(filter) );
    }
}
