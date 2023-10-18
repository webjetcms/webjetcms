package sk.iway.iwcm.components.inquiry.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InquiryAnswerRepository extends DomainIdRepository<InquiryAnswerEntity, Long> {

    Page<InquiryAnswerEntity> findAllByQuestionIdAndDomainId(Integer questionId, Integer domainId, Pageable pageable);

    List<InquiryAnswerEntity> findAllByQuestionIdAndDomainId(Integer questionId, Integer domainId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM InquiryAnswerEntity WHERE questionId = :questionId AND domainId = :domainId")
    void deleteAnswers(@Param("questionId")Integer questionId, @Param("domainId")Integer domainId);
}
