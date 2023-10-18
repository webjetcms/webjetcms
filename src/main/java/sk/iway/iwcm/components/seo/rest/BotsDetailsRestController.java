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

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.jpa.BotsDetailsDTO;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.FilterHeaderDto;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/seo/bots-details")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_seo')")
@Datatable
public class BotsDetailsRestController extends DatatableRestControllerV2<BotsDetailsDTO, Long> {
    
    @Autowired
    public BotsDetailsRestController() {
        super(null);
    }

    @Override
    public void beforeSave(BotsDetailsDTO entity) {
        throwError("datatables.error.recordIsNotEditable");
    }

    @Override
    public Page<BotsDetailsDTO> getAllItems(Pageable pageable) {  
        FilterHeaderDto filter = StatService.processRequestToStatFilter(getRequest(), null);
        int botId = Tools.getIntValue(getRequest().getParameter("botId"), -1);
        return new DatatablePageImpl<>( SeoService.getUserStatViews(filter, botId) );
    }

    @Override
    public Page<BotsDetailsDTO> searchItem(Map<String, String> params, Pageable pageable, BotsDetailsDTO search) {
        FilterHeaderDto filter = StatService.processMapToStatFilter(params, null);
        int botId = Tools.getIntValue(getRequest().getParameter("botId"), -1);
        return new DatatablePageImpl<>( SeoService.getUserStatViews(filter, botId) );
    }

    @RequestMapping(value="/botTitle", params={"botId"})
    @ResponseBody
    public String getBotTitle(@RequestParam("botId") Integer botId) {
        if(botId < 0) return "";
        return (new SimpleQuery()).forString("SELECT name FROM seo_bots WHERE seo_bots_id=?", botId);
    }

    @RequestMapping(
        value="/lineChartData",
        params={"botId", "dayDate", "rootDir"})
    @ResponseBody
    public Map<String, List<BotsDetailsDTO>> getLineChartData(
                @RequestParam("botId") int botId,
                @RequestParam("dayDate") String stringRange,
                @RequestParam("rootDir") int rootGroupId) {

        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);
        
        return SeoService.getBotsDetailsLineChartData(dateRangeArr[0], dateRangeArr[1], botId, rootGroupId);
    }
}
