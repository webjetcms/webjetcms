package sk.iway.iwcm.components.inquiry.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InquiryRepository extends DomainIdRepository<InquiryEntity, Long> {

    List<InquiryEntity> findDistinctAllByQuestionGroupLikeAndDomainId(String questionGroup, Integer domainId);
}
