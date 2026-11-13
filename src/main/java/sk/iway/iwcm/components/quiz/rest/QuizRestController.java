package sk.iway.iwcm.components.quiz.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.tools.AiTool;
import sk.iway.iwcm.components.ai.tools.AiToolDescription;
import sk.iway.iwcm.components.quiz.jpa.QuizEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizQuestionRepository;
import sk.iway.iwcm.components.quiz.jpa.QuizRepository;
import sk.iway.iwcm.components.quiz.jpa.QuizResultRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/quiz")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
@Datatable
@AiTool(description = "components.ai.tools.quiz.context")
public class QuizRestController extends DatatableRestControllerV2<QuizEntity, Long> {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;

    @Autowired
    public QuizRestController(QuizRepository quizRepository, QuizQuestionRepository quizQuestionRepository, QuizResultRepository quizResultRepository) {
        super(quizRepository);
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizResultRepository = quizResultRepository;
    }

    @Override
    public QuizEntity getOneItem(long id) {
        if (id < 1) {
            QuizEntity quiz = new QuizEntity();
            return quiz;
        }
        return super.getOneItem(id);
    }

    @Override
    public void beforeSave(QuizEntity entity) {
        if(entity.getId() == null || entity.getId() < 1L) {
            entity.setDomainId(CloudToolsForCore.getDomainId());
        }
        super.beforeSave(entity);
    }

    @Override
    public void afterSave(QuizEntity entity, QuizEntity saved) {
        Integer oldQuizId = -getUser().getUserId();
        Integer newQuizId = saved.getId().intValue();

        //Update all quizResults where quizId is -userId
        quizResultRepository.updateQuizId(newQuizId, oldQuizId);

        //Update all quizQuestion where quizId is -userId
        quizQuestionRepository.updateQuizId(newQuizId, oldQuizId);
    }

    @RequestMapping(value="/quizInfo", params={"quizId"})
    @AiToolDescription("components.ai.tools.quiz_get_quiz_type.description")
    public QuizEntity getQuizType(@RequestParam("quizId") int quizId) {
        //Default type
        if (quizId > 0) {
            Optional<QuizEntity> quiz = quizRepository.findFirstByIdAndDomainId((long) quizId, getDomainId());
            if (quiz.isPresent()) return quiz.get();
        }

        QuizEntity tmp = new QuizEntity();
        tmp.setName("");
        tmp.setQuizType("0");
        return tmp;
    }
}
