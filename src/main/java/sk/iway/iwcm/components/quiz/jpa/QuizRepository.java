package sk.iway.iwcm.components.quiz.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface QuizRepository extends DomainIdRepository<QuizEntity, Long> {
    Optional<QuizEntity> findById(Integer id);
}
