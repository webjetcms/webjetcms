package sk.iway.iwcm.components.ai.stat.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.ai.stat.dto.DaysUsageDTO;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

@RestController
@RequestMapping("/admin/rest/ai/stat/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_stats')")
@Datatable
public class AiStatRestController extends DatatableRestControllerV2<AiStatEntity, Long> {

    private final AiStatRepository repo;

    @Autowired
    public AiStatRestController(AiStatRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public void beforeSave(AiStatEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
    }

    @Override
    public boolean beforeDelete(AiStatEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return false;
    }

    @GetMapping("pieChartMostUsed")
    public List<LabelValueInteger> getPieChartDataMostUsed(@RequestParam("created") String stringRange) {
        return AiStatService.getPieChartDataMostUsed(StatService.processDateRangeString(stringRange), repo);
    }

    @GetMapping("pieChartMostTokens")
    public List<LabelValueInteger> getPieChartDataMostTokens(@RequestParam("created") String stringRange) {
        return AiStatService.getPieChartDataMostTokens(StatService.processDateRangeString(stringRange), repo);
    }

    @GetMapping("lineCharts")
    public Map<String, List<DaysUsageDTO>> getLineChartData(@RequestParam("created") String stringRange) {
        return AiStatService.getLineChartData(StatService.processDateRangeString(stringRange), repo);
    }
}