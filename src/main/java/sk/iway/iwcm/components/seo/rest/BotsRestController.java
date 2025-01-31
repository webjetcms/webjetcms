package sk.iway.iwcm.components.seo.rest;

import java.util.Date;
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

import sk.iway.iwcm.components.seo.jpa.BotsDTO;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/bots")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class BotsRestController extends DatatableRestControllerV2<BotsDTO, Long> {

    private FilterHeaderDto filter;

    @Autowired
    public BotsRestController() {
        super(null);
    }

    @Override
    public Page<BotsDTO> getAllItems(Pageable pageable) {
        //Process params from request into FilterHeaderDto
        filter = StatService.processRequestToStatFilter(getRequest(), null);
        DatatablePageImpl<BotsDTO> page = new DatatablePageImpl<>(SeoService.getBotsTableData(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId()));
        return page;
    }

    @Override
    public Page<BotsDTO> searchItem(Map<String, String> params, Pageable pageable, BotsDTO search) {
        //Process received params into FilterHeaderDto
        filter = StatService.processMapToStatFilter(params, null, getUser());
        DatatablePageImpl<BotsDTO> page = new DatatablePageImpl<>(SeoService.getBotsTableData(filter.getDateFrom(), filter.getDateTo(), filter.getRootGroupId()));
        return page;
    }

    @RequestMapping(
        value="/pieChartData",
        params={"dayDate", "rootDir"})
    @ResponseBody
    public List<BotsDTO> getPieChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        return SeoService.getBotsPieChartData(dateRangeArr[0], dateRangeArr[1], rootGroupId);
    }

    @RequestMapping(
        value="/lineChartData",
        params={"dayDate", "rootDir"})
    @ResponseBody
    public Map<String, List<BotsDTO>> getLineChartData(
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        return SeoService.getLineChartData(dateRangeArr[0], dateRangeArr[1], rootGroupId);
    }

    @Override
    public void beforeSave(BotsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }
}
