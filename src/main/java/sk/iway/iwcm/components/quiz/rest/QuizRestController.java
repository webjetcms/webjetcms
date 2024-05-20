package sk.iway.iwcm.components.quiz.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public void afterSave(QuizEntity entity, QuizEntity saved) {
        Integer oldQuizId = -getUser().getUserId();
        Integer newQuizId = saved.getId().intValue();

        //Update all quizResults where quizId is -userId
        quizResultRepository.updateQuizId(newQuizId, oldQuizId);

        //Update all quizQuestion where quizId is -userId
        quizQuestionRepository.updateQuizId(newQuizId, oldQuizId);
    }

    @RequestMapping(value="/quizInfo", params={"quizId"})
    public QuizEntity getQuizType(@RequestParam("quizId") int quizId) {
        //Default type
        if(quizId > 0) {
            Optional<QuizEntity> quizOptional =  quizRepository.findById(quizId);
            if(quizOptional.isPresent()) return quizOptional.get();
        }

        QuizEntity tmp = new QuizEntity();
        tmp.setName("");
        tmp.setQuizType("0");
        return tmp;
    }
}
