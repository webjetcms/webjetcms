package sk.iway.iwcm.doc.news_templates.jpa;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface NewsTemplatesRepository  extends DomainIdRepository<NewsTemplatesEntity, Long> {
    Optional<NewsTemplatesEntity> findByIdAndDomainId(Long id, Integer domainId);
    Optional<NewsTemplatesEntity> findFirstByNameAndDomainId(String name, Integer domainId);
}