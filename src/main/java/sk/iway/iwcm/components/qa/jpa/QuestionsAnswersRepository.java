package sk.iway.iwcm.components.qa.jpa;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.List;

@Repository
public interface QuestionsAnswersRepository extends DomainIdRepository<QuestionsAnswersEntity, Long> {

    List<QuestionsAnswersEntity> findAllByGroupNameLikeAndDomainId(String groupName, int domainId);
}
