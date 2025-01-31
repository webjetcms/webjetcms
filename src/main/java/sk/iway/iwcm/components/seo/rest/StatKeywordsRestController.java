package sk.iway.iwcm.components.seo.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.seo.jpa.StatKeywordsDTO;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/stat-keywords")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class StatKeywordsRestController extends DatatableRestControllerV2<StatKeywordsDTO, Long> {

    private final DocDetailsRepository docDetailsRepository;

    @Autowired
    public StatKeywordsRestController(DocDetailsRepository docDetailsRepository) {
        super(null);
        this. docDetailsRepository = docDetailsRepository;
    }

    @Override
    public void beforeSave(StatKeywordsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<StatKeywordsDTO> getAllItems(Pageable pageable) {
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        return new DatatablePageImpl<>( SeoService.getStatKeywordsTableData(filter) );
    }

    @Override
    public Page<StatKeywordsDTO> searchItem(Map<String, String> params, Pageable pageable, StatKeywordsDTO search) {
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null, getUser());
        return new DatatablePageImpl<>( SeoService.getStatKeywordsTableData(filter) );
    }

    @RequestMapping(
        value="/searchEnginesSelect",
        params={"dayDate", "rootDir", "webPage"})
    @ResponseBody
    public List<String> getSearchEnginesSelectValues(
            @RequestParam("dayDate") String dayDate,
            @RequestParam("rootDir") Integer rootDir,
            @RequestParam("webPage") Integer webPage) {

        return SeoService.getSearchEnginesSelectValues(dayDate, rootDir, webPage);
    }

    @RequestMapping(value="/webPageSelect", params={"rootDir"})
    @ResponseBody
    public Map<Integer, String> getWebPageSelectValues(@RequestParam("rootDir") int rootGroupId) {
        return SeoService.getWebPageSelectValues(rootGroupId, docDetailsRepository);
    }
}
