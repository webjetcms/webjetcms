package sk.iway.iwcm.components.ai.stat.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.rest.AiService;
import sk.iway.iwcm.components.ai.stat.dto.DaysUsageDTO;
import sk.iway.iwcm.components.ai.stat.dto.TokenUsersDTO;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

/**
 * REST controller for AI statistics - handles datatable requests
 */
@RestController
@RequestMapping("/admin/rest/ai/stat/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_stats')")
@Datatable
public class AiStatRestController extends DatatableRestControllerV2<AiStatEntity, Long> {

    private final AiService aiService;
    private final AiStatRepository asr;
    private final AssistantDefinitionRepository adr;

    @Autowired
    public AiStatRestController(AiStatRepository asr, AssistantDefinitionRepository adr, AiService aiService) {
        super(asr);
        this.asr = asr;
        this.adr = adr;
        this.aiService = aiService;
    }

    @Override
    public Page<AiStatEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<AiStatEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new AiStatEntity(), pageable));

        page.addOptions("assistantProvider", aiService.getProviders(getProp()), "label", "value", false);
        page.addOptions("assistantAction", SupportedActions.getSupportedActions(getProp()), "label", "value", false);
        page.addOptions("assistantGroupName", aiService.getGroupsOptions(getProp()), "label", "value", false);

       return page;
    }

    @Override
    public Page<AiStatEntity> searchItem(Map<String, String> params, Pageable pageable, AiStatEntity search) {
        Page<AiStatEntity> page = asr.findAll( AiStatService.getSpecification(params, pageable), pageable);
        return new DatatablePageImpl<>( AiStatService.fillStatEntities(page.getContent(), adr, getProp()), pageable, page.getTotalElements() );
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
    public List<LabelValueInteger> getPieChartDataMostUsed(
        @RequestParam("created") String created,
        @RequestParam("provider") String provider,
        @RequestParam("action") String action,
        @RequestParam("groupName") String groupName
    ) {
        return AiStatService.getPieChartDataMostUsed(created, provider, action, groupName, asr, adr, getProp());
    }

    @GetMapping("pieChartMostTokens")
    public List<LabelValueInteger> getPieChartDataMostTokens(
        @RequestParam("created") String created,
        @RequestParam("provider") String provider,
        @RequestParam("action") String action,
        @RequestParam("groupName") String groupName
    ) {
        return AiStatService.getPieChartDataMostTokens(created, provider, action, groupName, asr, adr, getProp());
    }

    @GetMapping("lineCharts")
    public Map<String, List<DaysUsageDTO>> getLineChartData(
        @RequestParam("created") String created,
        @RequestParam("provider") String provider,
        @RequestParam("action") String action,
        @RequestParam("groupName") String groupName
    ) {
        return AiStatService.getLineChartData(created, provider, action, groupName, asr, adr);
    }

    @GetMapping("barChartTop10Users")
    public List<LabelValueInteger> getBarChartDataTop10Users(@RequestParam("created") String created) {
        return AiStatService.getBarChartDataTop10Users(asr, created, getProp());
    }

    @GetMapping("tokenUsers/all")
    public Page<TokenUsersDTO> getTokenUsers(@RequestParam("created") String created) {
       return new DatatablePageImpl<>( AiStatService.getTokenUsersTableList(created, getProp()) );
    }
}