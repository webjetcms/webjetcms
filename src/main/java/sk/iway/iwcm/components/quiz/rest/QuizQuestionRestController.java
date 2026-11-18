package sk.iway.iwcm.components.quiz.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizQuestionRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/quiz/question")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
@Datatable
public class QuizQuestionRestController extends DatatableRestControllerV2<QuizQuestionEntity, Long> {
    
    private final QuizQuestionRepository quizQuestionRepository;

    @Autowired
    public QuizQuestionRestController(QuizQuestionRepository quizQuestionRepository) {
        super(quizQuestionRepository);
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    public Page<QuizQuestionEntity> getAllItems(Pageable pageable) { 
        Integer quizId = Tools.getIntValue(getRequest().getParameter("quizId"), -1);
        //-userId questions that are made under quiz, that is not saved yet
        if(quizId == -1) quizId = -getUser().getUserId();
        DatatablePageImpl<QuizQuestionEntity> page = new DatatablePageImpl<>( quizQuestionRepository.findAllByQuizId(quizId) );
        return page;
    }

    @Override
    public void beforeSave(QuizQuestionEntity entity) {
        //CREATE
        if(entity.getId() == null || entity.getId() < 1) {
            //There must be fill at least one question option (aka answer)
            boolean isAtLEastOneFill = false;
            for(int i = 1; i <= 6; i++) {
                if( Tools.isNotEmpty( entity.getOption(i) ) ) {
                    isAtLEastOneFill = true;
                    break;
                }
            }
            if(!isAtLEastOneFill) throwError("components.quiz.at_lest_one_answer_error");

            //If quizId == null -> new quiz (tmp set -userId)
            int quizId = entity.getQuizId() == null ? Tools.getIntValue(getRequest().getParameter("quizId"), -1) : entity.getQuizId();
            if(quizId == -1) entity.setQuizId( -getUser().getUserId() );
            else entity.setQuizId( quizId );

            Optional<QuizQuestionEntity> qq = quizQuestionRepository.findTopByQuizIdOrderByPositionDesc( entity.getQuizId() );
            if(qq.isPresent()) entity.setPosition( qq.get().getPosition() + 10 );
            else entity.setPosition( 10 );
        }
    }
}
