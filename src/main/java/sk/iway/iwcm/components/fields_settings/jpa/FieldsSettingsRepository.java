package sk.iway.iwcm.components.fields_settings.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface FieldsSettingsRepository extends DomainIdRepository<FieldsSettingsEntity, Long> {

    List<FieldsSettingsEntity> findAllByClassNameAndDomainId(String className, int domainId);

    boolean existsByClassNameAndFieldNameAndDomainIdAndIdNot(String className, String fieldName, int domainId, Long id);
}
