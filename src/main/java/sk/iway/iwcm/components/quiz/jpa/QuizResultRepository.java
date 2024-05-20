package sk.iway.iwcm.components.quiz.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResultEntity, Long> {
    List<QuizResultEntity> findAllByQuizId(Integer quizId);
    Optional<QuizResultEntity> findTopByQuizIdOrderByPositionDesc(Integer quizId);

    @Transactional
    @Modifying
    @Query("UPDATE QuizResultEntity SET quizId = :newQuizId WHERE quizId = :oldQuizId")
    public void updateQuizId(@Param("newQuizId") Integer newQuizId, @Param("oldQuizId") Integer oldQuizId);
}