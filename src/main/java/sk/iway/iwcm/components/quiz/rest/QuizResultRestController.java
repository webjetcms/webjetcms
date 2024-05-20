package sk.iway.iwcm.components.quiz.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.quiz.jpa.QuizResultEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizResultRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/quiz/result")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
@Datatable
public class QuizResultRestController extends DatatableRestControllerV2<QuizResultEntity, Long> {
    
    private final QuizResultRepository quizResultRepository;

    @Autowired
    public QuizResultRestController(QuizResultRepository quizResultRepository) {
        super(quizResultRepository);
        this.quizResultRepository = quizResultRepository;
    }

    @Override
    public Page<QuizResultEntity> getAllItems(Pageable pageable) { 
        Integer quizId = Tools.getIntValue(getRequest().getParameter("quizId"), -1);
        //-userId questions that are made under quiz, that is not saved yet
        if(quizId == -1) quizId = -getUser().getUserId();
        DatatablePageImpl<QuizResultEntity> page = new DatatablePageImpl<>( quizResultRepository.findAllByQuizId(quizId) );
        return page;
    }

    @Override
    public void beforeSave(QuizResultEntity entity) { 
        //CREATE
        if(entity.getId() == null || entity.getId() < 1) { 
            //If quizId == null -> new quiz (tmp set -userId)
            if(entity.getQuizId() == null) entity.setQuizId( -getUser().getUserId() );

            //set sortOrder (position) as MAX(sortOrder) + 1
            Optional<QuizResultEntity> qr = quizResultRepository.findTopByQuizIdOrderByPositionDesc( entity.getQuizId() );
            if(qr.isPresent()) entity.setPosition( qr.get().getSortOrder() + 10 );
            else entity.setPosition( 10 );
        }
    }
}