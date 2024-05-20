package sk.iway.iwcm.components.quiz.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.quiz.jpa.QuizAnswerRespository;
import sk.iway.iwcm.components.quiz.jpa.QuizStatDTO;
import sk.iway.iwcm.stat.ChartType;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/quiz/stat")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
@Datatable
public class QuizStatRestController extends DatatableRestControllerV2<QuizStatDTO, Long> {

    private final QuizAnswerRespository quizAnswerRespository;
    
    @Autowired
    public QuizStatRestController(QuizAnswerRespository quizAnswerRespository) {
        super(null);
        this.quizAnswerRespository = quizAnswerRespository;
    }

    @Override
    public Page<QuizStatDTO> getAllItems(Pageable pageable) {
        Integer quizId = Tools.getIntValue(getRequest().getParameter("quizId"), -1);
        String stringRange = getRequest().getParameter("dayDate");
        ChartType chartType = StatService.stringToChartTypeEnum(getRequest().getParameter("chartType"));

        return new PageImpl<>(QuizService.statTableData(quizId, stringRange, chartType, quizAnswerRespository));
    }

    @Override
    public Page<QuizStatDTO> searchItem(Map<String, String> params, Pageable pageable, QuizStatDTO search) {
        Integer quizId = -1;
        String stringRange = "";
        String chartType = "";

        for (Map.Entry<String, String> entry : params.entrySet()) { 
            if(entry.getKey().equalsIgnoreCase("quizId")) {
                quizId = Tools.getIntValue(entry.getValue(), -1);
            } else if(entry.getKey().equalsIgnoreCase("searchDayDate")) {
                stringRange = entry.getValue();
            } else if(entry.getKey().equalsIgnoreCase("chartType")) {
                chartType = entry.getValue();
            }
        }

        return new PageImpl<>(QuizService.statTableData(quizId, stringRange, StatService.stringToChartTypeEnum( chartType ), quizAnswerRespository));
    }

    @RequestMapping(value="/lineChartDataRight", params={"quizId", "dayDate"})
    @ResponseBody
    public Map<String, List<QuizStatDTO>> lineChartDataRight(
        @RequestParam("quizId") int quizId,
        @RequestParam("dayDate") String dayDate) {

        return QuizService.statLineData(quizId, dayDate, "0", quizAnswerRespository, getProp());
    }

    @RequestMapping(value="/lineChartDataRated", params={"quizId", "dayDate"})
    @ResponseBody
    public Map<String, List<QuizStatDTO>> lineChartDataRated(
        @RequestParam("quizId") int quizId,
        @RequestParam("dayDate") String dayDate) {

        return QuizService.statLineData(quizId, dayDate, "1", quizAnswerRespository, getProp());
    }
}
