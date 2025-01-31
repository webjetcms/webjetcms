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

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.jpa.NumberKeywordsDTO;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/number-keywords")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class NumberKeywordsRestController extends DatatableRestControllerV2<NumberKeywordsDTO, Long> {

    @Autowired
    public NumberKeywordsRestController() {
        super(null);
    }

    @Override
    public Page<NumberKeywordsDTO> getAllItems(Pageable pageable) {
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        DatatablePageImpl<NumberKeywordsDTO> page = new DatatablePageImpl<>( SeoService.getNumberKeywordsTableData(filter) );
        return page;
    }

    @Override
    public Page<NumberKeywordsDTO> searchItem(Map<String, String> params, Pageable pageable, NumberKeywordsDTO search) {
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null, getUser());
        DatatablePageImpl<NumberKeywordsDTO> page = new DatatablePageImpl<>(SeoService.getNumberKeywordsTableData(filter));
        return page;
    }

    @RequestMapping(
        value="/barChartData",
        params={"rootDir", "webPage"})
    @ResponseBody
    public List<NumberKeywordsDTO> getLineChartData(
                @RequestParam("rootDir") int rootGroupId,
                @RequestParam("webPage") int webPageId) {

        FilterHeaderDto filter = new FilterHeaderDto();
        filter.setRootGroupId(Tools.getIntValue(rootGroupId, -1));
        filter.setWebPageId(Tools.getIntValue(webPageId, -1));
        return SeoService.getNumberKeywordsBarChartData(filter);
    }

    @Override
    public void beforeSave(NumberKeywordsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }
}
