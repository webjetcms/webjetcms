package sk.iway.iwcm.components.inquiry.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InquiryUsersVoteRepository extends DomainIdRepository<InquiryUsersVoteEntity, Long> {
    @Query("SELECT DISTINCT iuve.userId FROM InquiryUsersVoteEntity iuve WHERE iuve.questionId = :questionId AND iuve.domainId = :domainId")
    List<Long> findAllDistinctUserIds(@Param("questionId") Long questionId, @Param("domainId") Integer domainId);
}
