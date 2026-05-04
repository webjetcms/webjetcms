package sk.iway.iwcm.components.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.quiz.jpa.QuizEntity;
import sk.iway.iwcm.components.quiz.rest.QuizService;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;


@WebjetComponent("sk.iway.iwcm.components.quiz.QuizApp")
@WebjetAppStore(
    nameKey = "components.quiz.title",
    descKey = "components.quiz.desc",
    itemKey = "cmp_quiz",
    imagePath = "/components/quiz/editoricon.png",
    componentPath = "/components/quiz/quiz.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "componentIframeWindowTab", title = "components.quiz.title", content = ""),
})
@Getter
@Setter
public class QuizApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title = "components.quiz.editorComponent.quiz",
        editor = @DataTableColumnEditor(
        )
    )
    private Integer quizId;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title = "components.quiz.editor_component.show_all"
    )
    private Boolean showAllAnswers;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTab", title="&nbsp;")
    private String iframe  = "/components/quiz/admin_list.jsp";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<OptionDto> groupOptions = new ArrayList<>();
        List<QuizEntity> quizes = QuizService.getAll();

        for (QuizEntity quiz : quizes){
            groupOptions.add(new OptionDto(quiz.getName(), String.valueOf(quiz.getId()), null));
        }
        options.put("quizId", groupOptions);
        return options;
    }
}
