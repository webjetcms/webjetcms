package sk.iway.iwcm.components.perex_groups;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface PerexGroupsRepository extends DomainIdRepository<PerexGroupsEntity, Long> {
    List<PerexGroupsEntity> findAllByDomainIdOrderByPerexGroupNameAsc(Integer domainId);
}
