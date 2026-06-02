package sk.iway.iwcm.components.customfields.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface CustomFieldsRepository extends DomainIdRepository<CustomFieldsEntity, Long> {

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.entityId = 0 AND cfe.bonusClassName = '' AND cfe.domainId = :domainId")
    List<CustomFieldsEntity> findAllGlobalCustomFields(String className, Integer domainId);

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.entityId = :entityId AND cfe.bonusClassName = '' AND cfe.domainId = :domainId")
    List<CustomFieldsEntity> findAllByClassNameAndEntityId(String className, Long entityId, Integer domainId);

    @Query("SELECT cfe FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.bonusClassName = :bonusClassName AND cfe.bonusEntityId = :bonusEntityId AND cfe.domainId = :domainId")
    List<CustomFieldsEntity> findAllByClassNameAndBonusContext(String className, String bonusClassName, Long bonusEntityId, Integer domainId);

    @Query("SELECT cfe.id FROM CustomFieldsEntity cfe WHERE cfe.className = :className AND cfe.alphabet = :alphabet AND cfe.entityId = :entityId AND cfe.bonusClassName = :bonusClassName AND cfe.bonusEntityId = :bonusEntityId AND cfe.domainId = :domainId")
    Optional<Long> getEntityId(String className, String alphabet, Long entityId, String bonusClassName, Long bonusEntityId, Integer domainId);

}
