package sk.iway.iwcm.components.response_header.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ResponseHeaderRepository extends DomainIdRepository<ResponseHeaderEntity, Long> {
    List<ResponseHeaderEntity> findByDomainId(Integer domainId);
}