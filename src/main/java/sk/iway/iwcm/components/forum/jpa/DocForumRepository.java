package sk.iway.iwcm.components.forum.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface DocForumRepository extends DomainIdRepository<DocForumEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM DocForumEntity WHERE id = :id AND domainId = :domainId")
    public void hardDelete(@Param("id")Long id, @Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE DocForumEntity SET deleted = 1 WHERE id = :id AND domainId = :domainId")
    public void softDelete(@Param("id")Long id, @Param("domainId")Integer domainId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE DocForumEntity SET deleted = 0 WHERE id = :id AND domainId = :domainId")
    public void undeleteEntity(@Param("id")Long id, @Param("domainId")Integer domainId);

    List<DocForumEntity> findAllByDomainId(Integer domainId);
}