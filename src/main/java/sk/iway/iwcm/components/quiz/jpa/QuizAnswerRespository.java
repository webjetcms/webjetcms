package sk.iway.iwcm.components.quiz.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAnswerRespository extends JpaRepository<QuizAnswerEntity, Long> {
    List<QuizAnswerEntity> findAllByFormId(String formId);

    List<QuizAnswerEntity> findAllByQuizIdAndCreatedBetweenOrderByQuizQuestionId(Integer quizId, Date from, Date to);
}