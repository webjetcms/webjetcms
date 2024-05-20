package sk.iway.iwcm.stat.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.seo.rest.SeoService;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.jpa.SearchEnginesDTO;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/stat/search-engines")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
@Datatable
public class SearchEnginesRestController extends DatatableRestControllerV2<SearchEnginesDTO, Long> {
    private final DocDetailsRepository docDetailsRepository;

    @Autowired
    public SearchEnginesRestController(DocDetailsRepository docDetailsRepository) {
        super(null);
        this.docDetailsRepository = docDetailsRepository;
    }

    @Override
    public void beforeSave(SearchEnginesDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<SearchEnginesDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        DatatablePageImpl<SearchEnginesDTO> page = new DatatablePageImpl<>( StatService.getSearchEnginesTableData(filter) );
        return page;
    }

    @Override
    public Page<SearchEnginesDTO> searchItem(Map<String, String> params, Pageable pageable, SearchEnginesDTO search) {
        //Process received params into FilterHeaderDto
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);
        DatatablePageImpl<SearchEnginesDTO> page = new DatatablePageImpl<>( StatService.getSearchEnginesTableData(filter) );
        return page;
    }

    @RequestMapping(
        value="/pieChartData",
        params={"dayDate", "rootDir"})
    public List<SearchEnginesDTO> getPieChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        String rootGroupIdQuery = FilterHeaderDto.groupIdToQuery(rootGroupId);
        return StatService.getSearchEnginesPieChartData(dateRangeArr[0], dateRangeArr[1], rootGroupIdQuery);
    }

    @RequestMapping(
        value="/searchEnginesSelect",
        params={"dayDate", "rootDir", "webPage"})
    public List<String> getSearchEnginesSelectValues(
            @RequestParam("dayDate") String dayDate,
            @RequestParam("rootDir") Integer rootDir,
            @RequestParam("webPage") Integer webPage) {

        return SeoService.getSearchEnginesSelectValues(dayDate, rootDir, webPage);
    }

    @RequestMapping(value="/webPageSelect", params={"rootDir"})
    public Map<Integer, String> getWebPageSelectValues(@RequestParam("rootDir") int rootGroupId) {
        return SeoService.getWebPageSelectValues(rootGroupId, docDetailsRepository);
    }
}