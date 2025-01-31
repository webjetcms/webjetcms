package sk.iway.iwcm.components.inquiry.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InquiryRepository extends DomainIdRepository<InquiryEntity, Long> {

    List<InquiryEntity> findDistinctAllByQuestionGroupLikeAndDomainId(String questionGroup, Integer domainId);

    @Query(value = "SELECT ie.questionText FROM InquiryEntity ie WHERE ie.id = :id")
    String getQuestionTextById(@Param("id")Long id);
}
