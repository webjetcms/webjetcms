package sk.iway.iwcm.components.forum.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ForumGroupRepository extends DomainIdRepository<ForumGroupEntity, Long> {
    Optional<ForumGroupEntity> findFirstByDocIdOrderById(Integer docId);
}
