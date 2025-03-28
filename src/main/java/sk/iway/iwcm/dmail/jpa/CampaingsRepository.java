package sk.iway.iwcm.dmail.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface CampaingsRepository extends DomainIdRepository<CampaingsEntity, Long> {
    @Transactional
    @Modifying
    @Query("UPDATE CampaingsEntity ce SET ce.userGroupsIds = :userGroupsIds WHERE ce.id = :id AND ce.domainId = :domainId")
    public void updateUserGroups(@Param("userGroupsIds") String userGroupsIds, @Param("id") Long id, @Param("domainId") Integer domainId);
}