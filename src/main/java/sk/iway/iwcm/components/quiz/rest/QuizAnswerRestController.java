package sk.iway.iwcm.components.quiz.rest;

import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.components.quiz.jpa.NameValueBean;
import sk.iway.iwcm.components.quiz.jpa.QuizAnswerEntity;

@RestController
public class QuizAnswerRestController {

    @PostMapping(path = "/rest/quiz/saveAnswers/{quizId}/{formId}")
    public Collection<QuizAnswerEntity> saveAnswersAndGetResult(@PathVariable int quizId, @PathVariable String formId, @RequestBody List<NameValueBean> answers, HttpServletRequest request) {
        if (SpamProtection.canPost("quiz", formId, request)) {
            return QuizService.saveAnswersAndGetResult(quizId, formId, answers);
        }
        return null;
    }

}