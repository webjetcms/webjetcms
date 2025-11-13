package sk.iway.iwcm.components.formsimple;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FormAttributesRepository extends DomainIdRepository<FormAttributesEntity, Long> {
    List<FormAttributesEntity> findAllByFormNameAndDomainId(String formName, Integer domainId);
}